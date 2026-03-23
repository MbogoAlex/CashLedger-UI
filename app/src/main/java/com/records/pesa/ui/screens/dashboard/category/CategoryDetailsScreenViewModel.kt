package com.records.pesa.ui.screens.dashboard.category

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.datastore.DataStoreRepository
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.CategoryKeyword
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.mapper.toResponseTransactionCategory
import com.records.pesa.mapper.toTransaction
import com.records.pesa.models.CategoryKeyword as ModelCategoryKeyword
import com.records.pesa.models.TimePeriod
import com.records.pesa.models.TransactionCategory
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.transactionCategory
import com.records.pesa.service.category.CategoryService
import com.records.pesa.service.transaction.TransactionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

enum class DeletionStatus { INITIAL, LOADING, SUCCESS, FAIL }

data class MemberStat(
    val keyword: String,
    val displayName: String,
    val totalIn: Double,
    val totalOut: Double,
    val txCount: Int
)

data class TrendPoint(
    val date: LocalDate,
    val totalOut: Double,
    val totalIn: Double,
    val hasData: Boolean = false,
    val label: String = ""  // x-axis label; set for all points when aggregated
)

data class InsightItem(
    val text: String,
    val isPositive: Boolean? = null, // null = neutral, true = good, false = caution
    val requiresPremium: Boolean = false
)

data class CategoryDetailsScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val preferences: UserPreferences? = null,
    val isPremium: Boolean = false,
    val categoryId: String = "",
    val newCategoryName: String = "",
    val newMemberName: String = "",
    val categoryKeyword: ModelCategoryKeyword = ModelCategoryKeyword(0, "", ""),
    val category: TransactionCategory = transactionCategory,
    val selectedPeriod: TimePeriod = TimePeriod.THIS_MONTH,
    val startDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    val endDate: LocalDate = LocalDate.now(),
    val showCustomPicker: Boolean = false,
    val totalIn: Double = 0.0,
    val totalOut: Double = 0.0,
    val txCount: Int = 0,
    val typeBreakdown: List<Pair<String, Int>> = emptyList(),
    val trendData: List<TrendPoint> = emptyList(),
    val memberStats: List<MemberStat> = emptyList(),
    val insights: List<InsightItem> = emptyList(),
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL,
    val deletionStatus: DeletionStatus = DeletionStatus.INITIAL
)

class CategoryDetailsScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository,
    private val categoryService: CategoryService,
    private val transactionService: TransactionService,
    private val dataStoreRepository: DataStoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryDetailsScreenUiState())
    val uiState: StateFlow<CategoryDetailsScreenUiState> = _uiState.asStateFlow()

    private val categoryId: String? = savedStateHandle[CategoryDetailsScreenDestination.categoryId]

    // Cache all transactions for client-side period filtering
    private var allTransactions: List<com.records.pesa.db.models.Transaction> = emptyList()
    private var allKeywords: List<CategoryKeyword> = emptyList()

    // ── Period management ─────────────────────────────────────────────────────

    fun selectPeriod(period: TimePeriod) {
        val isPremium = uiState.value.isPremium
        // Non-premium: cap at THIS_MONTH (max 30-day periods only)
        val safePeriod = if (!isPremium && period is TimePeriod.THIS_YEAR) TimePeriod.THIS_MONTH else period
        val (start, end) = safePeriod.getDateRange()
        _uiState.update {
            it.copy(
                selectedPeriod = safePeriod,
                startDate = start,
                endDate = end,
                showCustomPicker = false
            )
        }
        recomputeStats()
    }

    fun setCustomStartDate(date: LocalDate) {
        val isPremium = uiState.value.isPremium
        val earliest = if (isPremium) LocalDate.of(2000, 1, 1) else LocalDate.now().minusDays(30)
        val clamped = if (date.isBefore(earliest)) earliest else date
        _uiState.update { it.copy(startDate = clamped) }
        recomputeStats()
    }

    fun setCustomEndDate(date: LocalDate) {
        _uiState.update { it.copy(endDate = date) }
        recomputeStats()
    }

    fun toggleCustomPicker() {
        _uiState.update { it.copy(showCustomPicker = !it.showCustomPicker) }
    }

    // ── Category CRUD ─────────────────────────────────────────────────────────

    fun editCategoryName(name: String) {
        _uiState.update { it.copy(newCategoryName = name) }
    }

    fun editMemberName(name: String) {
        _uiState.update { it.copy(newMemberName = name) }
    }

    fun updateCategoryKeyword(categoryKeyword: ModelCategoryKeyword) {
        _uiState.update { it.copy(categoryKeyword = categoryKeyword) }
    }

    fun updateCategoryName() {
        _uiState.update { it.copy(loadingStatus = LoadingStatus.LOADING) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val cat = categoryService.getRawCategoryById(uiState.value.categoryId.toInt()).first()
                    categoryService.updateCategory(
                        cat.copy(
                            name = uiState.value.newCategoryName,
                            updatedTimes = (cat.updatedTimes ?: 0.0) + 1.0
                        )
                    )
                    _uiState.update { it.copy(loadingStatus = LoadingStatus.SUCCESS) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(loadingStatus = LoadingStatus.FAIL) }
                }
            }
        }
    }

    fun updateMemberName() {
        _uiState.update { it.copy(loadingStatus = LoadingStatus.LOADING) }
        viewModelScope.launch {
            val query = transactionService.createUserTransactionQuery(
                userId = uiState.value.userDetails.backUpUserId.toInt(),
                entity = uiState.value.categoryKeyword.keyWord,
                categoryId = uiState.value.categoryId.toInt(),
                budgetId = null, transactionType = null, moneyDirection = null,
                startDate = LocalDate.now().minusYears(10), endDate = LocalDate.now(), latest = true
            )
            withContext(Dispatchers.IO) {
                val kw = categoryService.getCategoryKeyword(uiState.value.categoryKeyword.id).first()
                val transactions = transactionService.getUserTransactions(query).first()
                for (tx in transactions) {
                    try {
                        transactionService.updateTransaction(
                            tx.toTransaction(uiState.value.userDetails.userId)
                                .copy(nickName = uiState.value.newMemberName)
                        )
                    } catch (e: Exception) { Log.e("updateMemberName", e.toString()) }
                }
                try {
                    categoryService.updateCategoryKeyword(kw.copy(nickName = uiState.value.newMemberName))
                    _uiState.update { it.copy(loadingStatus = LoadingStatus.SUCCESS) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(loadingStatus = LoadingStatus.FAIL) }
                }
            }
        }
    }

    fun removeCategoryMember(categoryId: Int, keywordId: Int) {
        _uiState.update { it.copy(loadingStatus = LoadingStatus.LOADING) }
        viewModelScope.launch {
            try {
                val keyword = categoryService.getCategoryKeyword(keywordId).first()
                val transactions = transactionService.getTransactionsByEntity(entity = keyword.keyword).first()
                for (tx in transactions) dbRepository.deleteTransactionFromCategoryMapping(tx.id)
                dbRepository.deleteCategoryKeywordByKeywordId(keywordId = keywordId)
                _uiState.update { it.copy(loadingStatus = LoadingStatus.SUCCESS) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loadingStatus = LoadingStatus.FAIL) }
            }
        }
    }

    fun removeCategory(categoryId: Int) {
        _uiState.update { it.copy(deletionStatus = DeletionStatus.LOADING) }
        viewModelScope.launch {
            try {
                dbRepository.deleteFromCategoryMappingByCategoryId(categoryId)
                dbRepository.deleteCategoryKeywordByCategoryId(categoryId)
                dbRepository.deleteCategory(categoryId)
                _uiState.update { it.copy(deletionStatus = DeletionStatus.SUCCESS) }
            } catch (e: Exception) {
                _uiState.update { it.copy(deletionStatus = DeletionStatus.FAIL) }
            }
        }
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    fun getCategory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    categoryService.getCategoryWithTransactions(uiState.value.categoryId.toInt())
                        .collect { cwt ->
                            allTransactions = cwt.transactions
                            allKeywords = cwt.keyWords
                            val responseCategory = cwt.toResponseTransactionCategory()
                            _uiState.update { it.copy(category = responseCategory) }
                            recomputeStats()
                        }
                } catch (e: Exception) {
                    Log.e("CategoryDetails.getCategory", e.toString())
                }
            }
        }
    }

    private fun recomputeStats() {
        val start = uiState.value.startDate
        val end = uiState.value.endDate
        val period = uiState.value.selectedPeriod
        val filtered = allTransactions.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }

        // Previous period (same duration, immediately before)
        val daySpan = start.until(end, java.time.temporal.ChronoUnit.DAYS)
        val prevEnd = start.minusDays(1)
        val prevStart = prevEnd.minusDays(daySpan)
        val prevFiltered = allTransactions.filter { !it.date.isBefore(prevStart) && !it.date.isAfter(prevEnd) }

        val totalIn  = filtered.filter { it.transactionAmount > 0 }.sumOf { it.transactionAmount }
        val totalOut = filtered.filter { it.transactionAmount < 0 }.sumOf { abs(it.transactionAmount) }
        val prevOut  = prevFiltered.filter { it.transactionAmount < 0 }.sumOf { abs(it.transactionAmount) }
        val prevIn   = prevFiltered.filter { it.transactionAmount > 0 }.sumOf { it.transactionAmount }

        val typeBreakdown = filtered.groupBy { it.transactionType }
            .map { (t, l) -> t to l.size }.sortedByDescending { it.second }

        val trendData   = buildTrendData(filtered, start, end)
        val memberStats = buildMemberStats(filtered, allKeywords)
        val insights    = buildInsights(filtered, prevFiltered, totalIn, totalOut, prevIn, prevOut, memberStats, period, start, end)

        _uiState.update {
            it.copy(
                totalIn = totalIn, totalOut = totalOut, txCount = filtered.size,
                typeBreakdown = typeBreakdown, trendData = trendData,
                memberStats = memberStats, insights = insights
            )
        }
    }

    private fun buildTrendData(
        transactions: List<com.records.pesa.db.models.Transaction>,
        start: LocalDate,
        end: LocalDate
    ): List<TrendPoint> {
        val daySpan = start.until(end, java.time.temporal.ChronoUnit.DAYS).toInt()

        return when {
            // Daily — up to 31 days
            daySpan <= 31 -> {
                val grouped = transactions.groupBy { it.date }
                val dayFmt = java.time.format.DateTimeFormatter.ofPattern("d MMM")
                (0..daySpan).map { offset ->
                    val date = start.plusDays(offset.toLong())
                    val txs = grouped[date] ?: emptyList()
                    TrendPoint(
                        date = date,
                        totalIn  = txs.filter { it.transactionAmount > 0 }.sumOf { it.transactionAmount },
                        totalOut = txs.filter { it.transactionAmount < 0 }.sumOf { abs(it.transactionAmount) },
                        hasData  = txs.isNotEmpty(),
                        label    = if (txs.isNotEmpty()) date.format(dayFmt) else ""
                    )
                }
            }

            // Weekly — 32 to 90 days
            daySpan <= 90 -> {
                val weekFmt = java.time.format.DateTimeFormatter.ofPattern("d MMM")
                val grouped = transactions.groupBy { it.date }
                var weekStart = start
                val points = mutableListOf<TrendPoint>()
                while (!weekStart.isAfter(end)) {
                    val weekEnd = minOf(weekStart.plusDays(6), end)
                    var inSum = 0.0; var outSum = 0.0; var hasAny = false
                    var d = weekStart
                    while (!d.isAfter(weekEnd)) {
                        val txs = grouped[d] ?: emptyList()
                        inSum  += txs.filter { it.transactionAmount > 0 }.sumOf { it.transactionAmount }
                        outSum += txs.filter { it.transactionAmount < 0 }.sumOf { abs(it.transactionAmount) }
                        if (txs.isNotEmpty()) hasAny = true
                        d = d.plusDays(1)
                    }
                    points.add(TrendPoint(
                        date     = weekStart,
                        totalIn  = inSum,
                        totalOut = outSum,
                        hasData  = hasAny,
                        label    = if (hasAny) weekStart.format(weekFmt) else ""
                    ))
                    weekStart = weekStart.plusDays(7)
                }
                points
            }

            // Monthly — more than 90 days
            else -> {
                val monthFmt = java.time.format.DateTimeFormatter.ofPattern("MMM yy")
                val grouped = transactions.groupBy {
                    it.date.withDayOfMonth(1)
                }
                var monthStart = start.withDayOfMonth(1)
                val points = mutableListOf<TrendPoint>()
                while (!monthStart.isAfter(end)) {
                    val txs = grouped[monthStart] ?: emptyList()
                    points.add(TrendPoint(
                        date     = monthStart,
                        totalIn  = txs.filter { it.transactionAmount > 0 }.sumOf { it.transactionAmount },
                        totalOut = txs.filter { it.transactionAmount < 0 }.sumOf { abs(it.transactionAmount) },
                        hasData  = txs.isNotEmpty(),
                        label    = monthStart.format(monthFmt)
                    ))
                    monthStart = monthStart.plusMonths(1)
                }
                points
            }
        }
    }

    private fun buildMemberStats(
        transactions: List<com.records.pesa.db.models.Transaction>,
        keywords: List<CategoryKeyword>
    ): List<MemberStat> = keywords.map { kw ->
        val memberTxs = transactions.filter { it.entity.equals(kw.keyword, ignoreCase = true) }
        MemberStat(
            keyword = kw.keyword,
            displayName = kw.nickName?.takeIf { it.isNotBlank() } ?: kw.keyword,
            totalIn = memberTxs.filter { it.transactionAmount > 0 }.sumOf { it.transactionAmount },
            totalOut = memberTxs.filter { it.transactionAmount < 0 }.sumOf { abs(it.transactionAmount) },
            txCount = memberTxs.size
        )
    }.sortedByDescending { it.totalOut + it.totalIn }

    private fun buildInsights(
        current: List<com.records.pesa.db.models.Transaction>,
        previous: List<com.records.pesa.db.models.Transaction>,
        totalIn: Double,
        totalOut: Double,
        prevIn: Double,
        prevOut: Double,
        memberStats: List<MemberStat>,
        period: TimePeriod,
        start: LocalDate,
        end: LocalDate
    ): List<InsightItem> {
        val insights = mutableListOf<InsightItem>()
        val dateFmt = java.time.format.DateTimeFormatter.ofPattern("EEE d MMM")
        val shortFmt = java.time.format.DateTimeFormatter.ofPattern("d MMM")

        // Human-readable label for the previous comparison period
        val prevLabel: String = when (period) {
            TimePeriod.TODAY       -> "yesterday"
            TimePeriod.YESTERDAY   -> "two days ago"
            TimePeriod.THIS_WEEK   -> "last week"
            TimePeriod.LAST_WEEK   -> "the week before"
            TimePeriod.THIS_MONTH  -> "last month"
            TimePeriod.LAST_MONTH  -> "the month before"
            TimePeriod.THIS_YEAR   -> "last year"
            is TimePeriod.SPECIFIC_YEAR -> "the year before"
            else -> {
                val days = start.until(end, java.time.temporal.ChronoUnit.DAYS) + 1
                "the previous $days days"
            }
        }

        // Days remaining in current period (for projections)
        val today = LocalDate.now()
        val daysRemaining = if (end.isAfter(today)) today.until(end, java.time.temporal.ChronoUnit.DAYS).toInt() else 0
        val daysElapsed = start.until(today.coerceAtMost(end), java.time.temporal.ChronoUnit.DAYS).toInt().coerceAtLeast(1)

        // ── FREE INSIGHTS ─────────────────────────────────────────────────────

        // 1. Spending vs named previous period
        if (totalOut > 0 || prevOut > 0) {
            when {
                prevOut == 0.0 && totalOut > 0 ->
                    insights.add(InsightItem(
                        "No spending was recorded $prevLabel — this is the first activity (KES ${fmt(totalOut)} out)",
                        isPositive = null
                    ))
                totalOut > 0 && prevOut > 0 -> {
                    val pct = ((totalOut - prevOut) / prevOut * 100).toInt()
                    val diff = abs(totalOut - prevOut)
                    insights.add(InsightItem(
                        when {
                            pct > 20  -> "⚠️ Spending jumped ${pct}% compared to $prevLabel — KES ${fmt(diff)} more (KES ${fmt(totalOut)} vs KES ${fmt(prevOut)})"
                            pct > 5   -> "Spending up ${pct}% compared to $prevLabel (KES ${fmt(totalOut)} vs KES ${fmt(prevOut)} — KES ${fmt(diff)} more)"
                            pct < -20 -> "🎉 Spending dropped ${abs(pct)}% compared to $prevLabel — saved KES ${fmt(diff)} (KES ${fmt(totalOut)} vs KES ${fmt(prevOut)})"
                            pct < -5  -> "Spending down ${abs(pct)}% vs $prevLabel — KES ${fmt(diff)} less (KES ${fmt(totalOut)} vs KES ${fmt(prevOut)})"
                            else      -> "Spending held steady vs $prevLabel — KES ${fmt(totalOut)} (${if (pct >= 0) "+$pct" else "$pct"}%)"
                        },
                        isPositive = when {
                            pct > 5  -> false
                            pct < -5 -> true
                            else     -> null
                        }
                    ))
                }
            }
        }

        // 2. Income vs named previous period
        if (totalIn > 0 && prevIn > 0) {
            val pct = ((totalIn - prevIn) / prevIn * 100).toInt()
            val diff = abs(totalIn - prevIn)
            if (abs(pct) > 5) {
                insights.add(InsightItem(
                    if (pct > 0)
                        "Received KES ${fmt(diff)} more than $prevLabel (KES ${fmt(totalIn)} vs KES ${fmt(prevIn)}, +${pct}%)"
                    else
                        "Received KES ${fmt(diff)} less than $prevLabel (KES ${fmt(totalIn)} vs KES ${fmt(prevIn)}, ${pct}%)",
                    isPositive = pct > 0
                ))
            }
        } else if (totalIn > 0 && prevIn == 0.0) {
            insights.add(InsightItem(
                "Received KES ${fmt(totalIn)} — no income was recorded in this category $prevLabel",
                isPositive = null
            ))
        }

        // 3. Net balance with context
        val net = totalIn - totalOut
        if (totalIn > 0 || totalOut > 0) {
            val context = if (prevOut > 0 || prevIn > 0) {
                val prevNet = prevIn - prevOut
                val netDiff = net - prevNet
                " (${if (netDiff >= 0) "KES ${fmt(netDiff)} better" else "KES ${fmt(abs(netDiff))} worse"} than $prevLabel)"
            } else ""
            insights.add(InsightItem(
                if (net >= 0)
                    "Net positive: KES ${fmt(net)} more in than out$context"
                else
                    "Net negative: KES ${fmt(abs(net))} more out than in$context",
                isPositive = net >= 0
            ))
        }

        // 4. Daily spending rate
        if (totalOut > 0 && daysElapsed > 0) {
            val dailyRate = totalOut / daysElapsed
            val prevDailyRate = if (prevOut > 0) prevOut / (daysElapsed) else 0.0
            val rateContext = if (prevDailyRate > 0) {
                val rPct = ((dailyRate - prevDailyRate) / prevDailyRate * 100).toInt()
                " (${if (rPct >= 0) "▲ ${abs(rPct)}%" else "▼ ${abs(rPct)}%"} vs $prevLabel)"
            } else ""
            insights.add(InsightItem(
                "Spending at KES ${fmt(dailyRate)}/day on average$rateContext",
                isPositive = if (prevDailyRate > 0) dailyRate < prevDailyRate else null
            ))
        }

        // 5. Top member with volume detail
        if (memberStats.isNotEmpty()) {
            val top = memberStats.first()
            if (top.txCount > 0) {
                val share = if (totalOut > 0 && top.totalOut > 0) " (${(top.totalOut / totalOut * 100).toInt()}% of total spending)" else ""
                insights.add(InsightItem(
                    "Most active: ${top.displayName} — ${top.txCount} transaction${if (top.txCount != 1) "s" else ""}, KES ${fmt(top.totalOut + top.totalIn)} total$share"
                ))
            }
        }

        // 6. Busiest day — specific date, amount and count
        if (current.isNotEmpty()) {
            val peakEntry = current.groupBy { it.date }
                .maxByOrNull { (_, txs) -> txs.sumOf { t -> abs(t.transactionAmount) } }
            if (peakEntry != null) {
                val peakAmt = peakEntry.value.sumOf { abs(it.transactionAmount) }
                insights.add(InsightItem(
                    "Busiest day: ${peakEntry.key.format(dateFmt)} — KES ${fmt(peakAmt)} across ${peakEntry.value.size} transaction${if (peakEntry.value.size != 1) "s" else ""}"
                ))
            }
        }

        // 7. Quietest day (only if period has meaningful data)
        if (current.size >= 5) {
            val quietEntry = current.groupBy { it.date }
                .minByOrNull { (_, txs) -> txs.sumOf { t -> abs(t.transactionAmount) } }
            if (quietEntry != null) {
                val quietAmt = quietEntry.value.sumOf { abs(it.transactionAmount) }
                insights.add(InsightItem(
                    "Lightest day: ${quietEntry.key.format(dateFmt)} — only KES ${fmt(quietAmt)} (${quietEntry.value.size} transaction${if (quietEntry.value.size != 1) "s" else ""})"
                ))
            }
        }

        // ── PREMIUM INSIGHTS ──────────────────────────────────────────────────

        // P1. End-of-period spend projection
        if (daysRemaining > 0 && totalOut > 0 && daysElapsed > 0) {
            val dailyRate = totalOut / daysElapsed
            val projected = totalOut + dailyRate * daysRemaining
            val saving = projected - prevOut
            val savingText = if (prevOut > 0)
                " — ${if (saving > 0) "KES ${fmt(saving)} more" else "KES ${fmt(abs(saving))} less"} than $prevLabel"
            else ""
            insights.add(InsightItem(
                "📈 Projection: at your current KES ${fmt(dailyRate)}/day rate, you'll spend ~KES ${fmt(projected)} by ${end.format(shortFmt)}$savingText",
                isPositive = if (prevOut > 0) projected < prevOut else null,
                requiresPremium = true
            ))
        }

        // P2. Unusual activity alert — compare to 3-month rolling average
        val threeMonthsAgo = end.minusMonths(3)
        val historicalTxs = allTransactions.filter {
            !it.date.isBefore(threeMonthsAgo) && it.date.isBefore(start)
        }
        if (historicalTxs.isNotEmpty() && current.isNotEmpty()) {
            val historicalDays = threeMonthsAgo.until(start, java.time.temporal.ChronoUnit.DAYS).toInt().coerceAtLeast(1)
            val historicalDailyRate = historicalTxs.filter { it.transactionAmount < 0 }
                .sumOf { abs(it.transactionAmount) } / historicalDays
            val currentDailyRate = if (daysElapsed > 0) totalOut / daysElapsed else 0.0
            if (historicalDailyRate > 0 && currentDailyRate > 0) {
                val multiple = currentDailyRate / historicalDailyRate
                when {
                    multiple >= 2.5 -> insights.add(InsightItem(
                        "🚨 Unusual spike: spending ${String.format("%.1f", multiple)}× your 3-month daily average (KES ${fmt(currentDailyRate)}/day vs KES ${fmt(historicalDailyRate)}/day norm) — investigate if this was expected",
                        isPositive = false, requiresPremium = true
                    ))
                    multiple <= 0.3 -> insights.add(InsightItem(
                        "📉 Very low activity: only ${String.format("%.0f", multiple * 100)}% of your 3-month daily average — category may be underused or payments moved elsewhere",
                        isPositive = null, requiresPremium = true
                    ))
                }
            }
        }

        // P3. Day-of-week pattern
        if (allTransactions.size >= 10) {
            val byDow = allTransactions.groupBy { it.date.dayOfWeek }
            val topDow = byDow.maxByOrNull { (_, txs) -> txs.size }
            if (topDow != null) {
                val totalAllTx = allTransactions.size
                val dowPct = (topDow.value.size * 100 / totalAllTx)
                val dowName = topDow.key.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
                insights.add(InsightItem(
                    "📅 You transact most on ${dowName}s — $dowPct% of all-time activity in this category falls on a $dowName. Plan accordingly!",
                    isPositive = null, requiresPremium = true
                ))
            }
        }

        // P4. Member concentration risk
        if (memberStats.size >= 2 && totalOut > 0) {
            val topMember = memberStats.first()
            val concentration = (topMember.totalOut / totalOut * 100).toInt()
            if (concentration >= 60) {
                insights.add(InsightItem(
                    "⚖️ High concentration: ${topMember.displayName} alone accounts for ${concentration}% of spending — consider diversifying or setting a budget cap for this member",
                    isPositive = false, requiresPremium = true
                ))
            }
        }

        // P5. Largest single outflow — name and date
        if (current.isNotEmpty()) {
            val biggest = current.filter { it.transactionAmount < 0 }
                .maxByOrNull { abs(it.transactionAmount) }
            if (biggest != null) {
                val bigPct = if (totalOut > 0) (abs(biggest.transactionAmount) / totalOut * 100).toInt() else 0
                insights.add(InsightItem(
                    "💸 Largest single outflow: KES ${fmt(abs(biggest.transactionAmount))} to ${biggest.entity} on ${biggest.date.format(dateFmt)} — ${bigPct}% of total spending this period",
                    isPositive = null, requiresPremium = true
                ))
            }
        }

        // P6. Consecutive periods of increasing spend (rolling 3 periods)
        val periodDuration = start.until(end, java.time.temporal.ChronoUnit.DAYS) + 1
        val p2End   = start.minusDays(1)
        val p2Start = p2End.minusDays(periodDuration - 1)
        val p3End   = p2Start.minusDays(1)
        val p3Start = p3End.minusDays(periodDuration - 1)
        val p2Out = allTransactions.filter { !it.date.isBefore(p2Start) && !it.date.isAfter(p2End) }
            .filter { it.transactionAmount < 0 }.sumOf { abs(it.transactionAmount) }
        val p3Out = allTransactions.filter { !it.date.isBefore(p3Start) && !it.date.isAfter(p3End) }
            .filter { it.transactionAmount < 0 }.sumOf { abs(it.transactionAmount) }
        if (totalOut > 0 && p2Out > 0 && p3Out > 0) {
            when {
                totalOut > p2Out && p2Out > p3Out ->
                    insights.add(InsightItem(
                        "📈 3 consecutive periods of rising spend: KES ${fmt(p3Out)} → KES ${fmt(p2Out)} → KES ${fmt(totalOut)}. This category is trending expensive — consider a budget",
                        isPositive = false, requiresPremium = true
                    ))
                totalOut < p2Out && p2Out < p3Out ->
                    insights.add(InsightItem(
                        "📉 3 consecutive periods of falling spend: KES ${fmt(p3Out)} → KES ${fmt(p2Out)} → KES ${fmt(totalOut)} — excellent discipline in this category!",
                        isPositive = true, requiresPremium = true
                    ))
            }
        }

        // P7. Savings opportunity — gap between current and best historical period
        val periodWindows = mutableListOf<Double>()
        var windowStart = allTransactions.minByOrNull { it.date }?.date ?: start
        while (!windowStart.isAfter(end.minusDays(periodDuration - 1))) {
            val windowEnd = windowStart.plusDays(periodDuration - 1)
            val windowOut = allTransactions
                .filter { !it.date.isBefore(windowStart) && !it.date.isAfter(windowEnd) }
                .filter { it.transactionAmount < 0 }.sumOf { abs(it.transactionAmount) }
            if (windowOut > 0) periodWindows.add(windowOut)
            windowStart = windowStart.plusDays(periodDuration)
        }
        if (periodWindows.size >= 3 && totalOut > 0) {
            val bestPeriod = periodWindows.min()
            if (totalOut > bestPeriod * 1.15) {
                val saving = totalOut - bestPeriod
                insights.add(InsightItem(
                    "💡 Savings opportunity: your best comparable period spent only KES ${fmt(bestPeriod)}. Matching it would save you KES ${fmt(saving)} — a ${(saving / totalOut * 100).toInt()}% reduction",
                    isPositive = null, requiresPremium = true
                ))
            }
        }

        // P8. All-time summary
        if (allTransactions.isNotEmpty()) {
            val allTimeOut = allTransactions.filter { it.transactionAmount < 0 }.sumOf { abs(it.transactionAmount) }
            val allTimeIn  = allTransactions.filter { it.transactionAmount > 0 }.sumOf { it.transactionAmount }
            val earliest   = allTransactions.minByOrNull { it.date }?.date
            val monthsActive = if (earliest != null)
                earliest.until(today, java.time.temporal.ChronoUnit.MONTHS).toInt().coerceAtLeast(1) else 1
            insights.add(InsightItem(
                "🗂️ All-time: KES ${fmt(allTimeOut)} out, KES ${fmt(allTimeIn)} in across ${allTransactions.size} transactions over $monthsActive month${if (monthsActive != 1) "s" else ""}. Monthly average: KES ${fmt(allTimeOut / monthsActive)} out",
                requiresPremium = true
            ))
        }

        return insights
    }

    private fun fmt(amount: Double): String = String.format("%,.0f", amount)

    fun resetLoadingStatus() {
        _uiState.update {
            it.copy(loadingStatus = LoadingStatus.INITIAL, deletionStatus = DeletionStatus.INITIAL)
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            dataStoreRepository.getUserPreferences().collect { prefs ->
                val isPremium = prefs.permanent ||
                    (prefs.expiryDate?.isAfter(LocalDateTime.now()) == true)
                _uiState.update { it.copy(preferences = prefs, isPremium = isPremium) }
            }
        }
    }

    fun getUserDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(userDetails = dbRepository.getUsers().first()[0]) }
            while (uiState.value.userDetails.userId == 0) delay(1000)
            getCategory()
        }
    }

    init {
        _uiState.update { it.copy(categoryId = categoryId!!) }
        loadPreferences()
        getUserDetails()
    }
}
