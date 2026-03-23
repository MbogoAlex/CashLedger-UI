package com.records.pesa.ui.screens.dashboard.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.datastore.DataStoreRepository
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.Budget
import com.records.pesa.db.models.Transaction
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.ExecutionStatus
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.service.category.CategoryService
import com.records.pesa.ui.screens.dashboard.category.InsightItem
import com.records.pesa.ui.screens.dashboard.category.TrendPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

data class BudgetInfoScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val isPremium: Boolean = false,
    val budgetId: String? = null,
    val budget: Budget? = null,
    // Computed from local transactions
    val actualSpending: Double = 0.0,
    val percentUsed: Int = 0,
    val remaining: Double = 0.0,
    val isOverBudget: Boolean = false,
    val overage: Double = 0.0,
    val daysLeft: Int = 0,
    val daysElapsed: Int = 0,
    val totalDays: Int = 0,
    val dailyBudget: Double = 0.0,       // remaining / daysLeft (forward-looking)
    val dailyLimitLine: Double = 0.0,    // budgetLimit / totalDays (fixed reference for chart)
    val dailyAvg: Double = 0.0,
    val projectedTotal: Double = 0.0,
    // Chart + insights
    val transactions: List<Transaction> = emptyList(),
    val trendData: List<TrendPoint> = emptyList(),
    val insights: List<InsightItem> = emptyList(),
    // Edit fields
    val budgetName: String = "",
    val budgetLimit: String = "",
    val budgetStartDate: String = "",
    val budgetLimitDate: String = "",
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL,
    val executionStatus: ExecutionStatus = ExecutionStatus.INITIAL
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetInfoScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository,
    private val categoryService: CategoryService,
    private val dataStoreRepository: DataStoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetInfoScreenUiState())
    val uiState: StateFlow<BudgetInfoScreenUiState> = _uiState.asStateFlow()

    private val budgetIdArg: String? = savedStateHandle[BudgetInfoScreenDestination.budgetId]

    fun updateBudgetName(name: String) = _uiState.update { it.copy(budgetName = name) }
    fun updateBudgetLimit(amount: String) = _uiState.update { it.copy(budgetLimit = amount) }
    fun updateStartDate(date: String) = _uiState.update { it.copy(budgetStartDate = date) }
    fun updateLimitDate(date: String) = _uiState.update { it.copy(budgetLimitDate = date) }

    fun resetLoadingStatus() = _uiState.update {
        it.copy(loadingStatus = LoadingStatus.INITIAL, executionStatus = ExecutionStatus.INITIAL)
    }

    fun saveBudgetEdits() {
        val budget = _uiState.value.budget ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(loadingStatus = LoadingStatus.LOADING) }
            try {
                val updated = budget.copy(
                    name = _uiState.value.budgetName.ifBlank { budget.name },
                    budgetLimit = _uiState.value.budgetLimit.toDoubleOrNull() ?: budget.budgetLimit,
                    startDate = _uiState.value.budgetStartDate.takeIf { it.isNotBlank() }
                        ?.let { LocalDate.parse(it) } ?: budget.startDate,
                    limitDate = _uiState.value.budgetLimitDate.takeIf { it.isNotBlank() }
                        ?.let { LocalDate.parse(it) } ?: budget.limitDate
                )
                dbRepository.updateBudget(updated)
                _uiState.update { it.copy(loadingStatus = LoadingStatus.SUCCESS) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loadingStatus = LoadingStatus.FAIL) }
            }
        }
    }

    fun deleteBudget() {
        val budget = _uiState.value.budget ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(executionStatus = ExecutionStatus.LOADING) }
            try {
                dbRepository.deleteBudget(budget)
                _uiState.update { it.copy(executionStatus = ExecutionStatus.SUCCESS) }
            } catch (e: Exception) {
                _uiState.update { it.copy(executionStatus = ExecutionStatus.FAIL) }
            }
        }
    }

    private fun observeBudgetAndSpending() {
        val id = budgetIdArg?.toIntOrNull() ?: return
        viewModelScope.launch {
            dbRepository.getBudgetById(id)
                .filterNotNull()
                .flatMapLatest { budget ->
                    _uiState.update {
                        it.copy(
                            budget = budget,
                            budgetName = budget.name,
                            budgetLimit = budget.budgetLimit.toString(),
                            budgetStartDate = budget.startDate.toString(),
                            budgetLimitDate = budget.limitDate.toString()
                        )
                    }
                    val start = budget.startDate
                    val end = budget.limitDate
                    dbRepository.getOutflowForCategory(budget.categoryId, start, end)
                }
                .collectLatest { spending ->
                    val budget = _uiState.value.budget ?: return@collectLatest
                    recomputeAll(budget, spending)
                }
        }
    }

    private fun recomputeAll(budget: Budget, spending: Double) {
        val today = LocalDate.now()
        val start = budget.startDate
        val end = budget.limitDate

        val totalDays = ChronoUnit.DAYS.between(start, end).toInt().coerceAtLeast(1)
        val daysElapsed = ChronoUnit.DAYS.between(start, today).toInt().coerceIn(0, totalDays)
        val daysLeft = (totalDays - daysElapsed).coerceAtLeast(0)
        val percentUsed = if (budget.budgetLimit > 0)
            ((spending / budget.budgetLimit) * 100).roundToInt().coerceAtLeast(0)
        else 0
        val remaining = (budget.budgetLimit - spending).coerceAtLeast(0.0)
        val isOverBudget = spending > budget.budgetLimit
        val overage = if (isOverBudget) spending - budget.budgetLimit else 0.0
        val dailyBudget = if (daysLeft > 0) remaining / daysLeft else 0.0
        val dailyLimitLine = if (totalDays > 0) budget.budgetLimit / totalDays else 0.0
        val dailyAvg = if (daysElapsed > 0) spending / daysElapsed else 0.0
        val projectedTotal = dailyAvg * totalDays

        _uiState.update {
            it.copy(
                actualSpending = spending,
                percentUsed = percentUsed,
                remaining = remaining,
                isOverBudget = isOverBudget,
                overage = overage,
                daysLeft = daysLeft,
                daysElapsed = daysElapsed,
                totalDays = totalDays,
                dailyBudget = dailyBudget,
                dailyLimitLine = dailyLimitLine,
                dailyAvg = dailyAvg,
                projectedTotal = projectedTotal
            )
        }

        // Load transactions and build chart + insights
        viewModelScope.launch {
            val transactions = categoryService.getCategoryWithTransactions(budget.categoryId)
                .first()
                .transactions
                .filter { it.date >= start && it.date <= end }
                .sortedBy { it.date }

            val trendData = buildTrendData(transactions, start, end)
            val insights = buildInsights(
                transactions = transactions,
                budget = budget,
                spending = spending,
                dailyAvg = dailyAvg,
                projectedTotal = projectedTotal,
                daysLeft = daysLeft,
                daysElapsed = daysElapsed,
                totalDays = totalDays,
                remaining = remaining,
                isOverBudget = isOverBudget,
                overage = overage
            )

            _uiState.update {
                it.copy(
                    transactions = transactions,
                    trendData = trendData,
                    insights = insights
                )
            }
        }
    }

    private fun buildTrendData(
        transactions: List<Transaction>,
        start: LocalDate,
        end: LocalDate
    ): List<TrendPoint> {
        val daySpan = ChronoUnit.DAYS.between(start, end).toInt()
        val dayFmt = DateTimeFormatter.ofPattern("d MMM")
        val weekFmt = DateTimeFormatter.ofPattern("d MMM")
        val monthFmt = DateTimeFormatter.ofPattern("MMM yy")

        return when {
            daySpan <= 31 -> {
                val grouped = transactions.groupBy { it.date }
                (0..daySpan).map { offset ->
                    val date = start.plusDays(offset.toLong())
                    val txs = grouped[date] ?: emptyList()
                    TrendPoint(
                        date = date,
                        totalOut = txs.filter { it.transactionAmount < 0 }.sumOf { abs(it.transactionAmount) },
                        totalIn = txs.filter { it.transactionAmount > 0 }.sumOf { it.transactionAmount },
                        hasData = txs.isNotEmpty(),
                        label = if (txs.isNotEmpty()) date.format(dayFmt) else ""
                    )
                }
            }
            daySpan <= 90 -> {
                val grouped = transactions.groupBy { it.date }
                var weekStart = start
                val points = mutableListOf<TrendPoint>()
                while (!weekStart.isAfter(end)) {
                    val weekEnd = minOf(weekStart.plusDays(6), end)
                    var inSum = 0.0; var outSum = 0.0; var hasAny = false
                    var d = weekStart
                    while (!d.isAfter(weekEnd)) {
                        val txs = grouped[d] ?: emptyList()
                        inSum += txs.filter { it.transactionAmount > 0 }.sumOf { it.transactionAmount }
                        outSum += txs.filter { it.transactionAmount < 0 }.sumOf { abs(it.transactionAmount) }
                        if (txs.isNotEmpty()) hasAny = true
                        d = d.plusDays(1)
                    }
                    points.add(TrendPoint(date = weekStart, totalOut = outSum, totalIn = inSum,
                        hasData = hasAny, label = if (hasAny) weekStart.format(weekFmt) else ""))
                    weekStart = weekStart.plusDays(7)
                }
                points
            }
            else -> {
                val grouped = transactions.groupBy { it.date.withDayOfMonth(1) }
                var monthStart = start.withDayOfMonth(1)
                val points = mutableListOf<TrendPoint>()
                while (!monthStart.isAfter(end)) {
                    val txs = grouped[monthStart] ?: emptyList()
                    points.add(TrendPoint(date = monthStart,
                        totalOut = txs.filter { it.transactionAmount < 0 }.sumOf { abs(it.transactionAmount) },
                        totalIn = txs.filter { it.transactionAmount > 0 }.sumOf { it.transactionAmount },
                        hasData = txs.isNotEmpty(), label = monthStart.format(monthFmt)))
                    monthStart = monthStart.plusMonths(1)
                }
                points
            }
        }
    }

    private fun buildInsights(
        transactions: List<Transaction>,
        budget: Budget,
        spending: Double,
        dailyAvg: Double,
        projectedTotal: Double,
        daysLeft: Int,
        daysElapsed: Int,
        totalDays: Int,
        remaining: Double,
        isOverBudget: Boolean,
        overage: Double
    ): List<InsightItem> {
        val insights = mutableListOf<InsightItem>()
        val dateFmt = DateTimeFormatter.ofPattern("EEE d MMM")
        val shortFmt = DateTimeFormatter.ofPattern("d MMM")
        fun fmt(d: Double) = "%,.0f".format(d)

        val start = budget.startDate
        val end = budget.limitDate
        val today = LocalDate.now()

        // ── FREE INSIGHTS ─────────────────────────────────────────────────────

        // 1. Burn rate + projection
        if (spending > 0 && daysElapsed > 0) {
            val projStr = if (projectedTotal > budget.budgetLimit)
                "⚠️ You're on track to spend KES ${fmt(projectedTotal)} — KES ${fmt(projectedTotal - budget.budgetLimit)} over your limit."
            else
                "At this rate you'll spend KES ${fmt(projectedTotal)} by ${end.format(shortFmt)} — KES ${fmt(budget.budgetLimit - projectedTotal)} under limit."
            insights.add(InsightItem(
                "Spending KES ${fmt(dailyAvg)}/day on average. $projStr",
                isPositive = projectedTotal <= budget.budgetLimit
            ))
        }

        // 2. Budget status
        val statusText = when {
            isOverBudget -> "🔴 Over budget by KES ${fmt(overage)} with $daysLeft days remaining."
            percentUsed >= 80 -> "🟡 ${percentUsed}% used — only KES ${fmt(remaining)} left with $daysLeft days to go. Pace carefully."
            else -> "🟢 ${percentUsed}% used with $daysLeft days left. You're on track."
        }
        insights.add(InsightItem(statusText, isPositive = !isOverBudget && percentUsed < 80))

        // 3. Daily allowance from today
        if (daysLeft > 0 && !isOverBudget) {
            insights.add(InsightItem(
                "To stay within budget, spend no more than KES ${fmt(budget.dailyAllowance(remaining, daysLeft))}/day from today.",
                isPositive = null
            ))
        }

        // ── PREMIUM INSIGHTS ──────────────────────────────────────────────────

        // P1. Projection with exact exceed date
        if (dailyAvg > 0 && daysLeft > 0 && !isOverBudget) {
            val daysToExceed = (remaining / dailyAvg).toInt()
            val exceedDate = today.plusDays(daysToExceed.toLong())
            if (exceedDate.isBefore(end)) {
                insights.add(InsightItem(
                    "🚨 At current pace you'll exceed your budget on ${exceedDate.format(shortFmt)} — ${ChronoUnit.DAYS.between(exceedDate, end)} days early. Cut daily spend to KES ${fmt(remaining.toDouble() / daysLeft)} to make it last.",
                    isPositive = false,
                    requiresPremium = true
                ))
            }
        }

        // P2. Spending velocity — week over week
        if (transactions.size >= 5 && totalDays >= 14) {
            val midpoint = start.plusDays(totalDays.toLong() / 2)
            val firstHalf = transactions.filter { it.date < midpoint && it.transactionAmount < 0 }
                .sumOf { abs(it.transactionAmount) }
            val secondHalf = transactions.filter { it.date >= midpoint && it.transactionAmount < 0 }
                .sumOf { abs(it.transactionAmount) }
            if (firstHalf > 0 && secondHalf > 0) {
                val change = ((secondHalf - firstHalf) / firstHalf * 100).toInt()
                insights.add(InsightItem(
                    when {
                        change > 10 -> "⚡ Spending accelerated ${change}% in the second half of this budget (KES ${fmt(firstHalf)} → KES ${fmt(secondHalf)}). You started slower but picked up pace."
                        change < -10 -> "📉 Spending slowed ${abs(change)}% in the second half (KES ${fmt(firstHalf)} → KES ${fmt(secondHalf)}). Good discipline."
                        else -> "Spending was roughly consistent across both halves of this budget."
                    },
                    isPositive = change < -10,
                    requiresPremium = true
                ))
            }
        }

        // P3. Single largest transaction
        val largest = transactions.filter { it.transactionAmount < 0 }
            .minByOrNull { it.transactionAmount }
        if (largest != null && spending > 0) {
            val largeAmt = abs(largest.transactionAmount)
            val pct = (largeAmt / spending * 100).toInt()
            insights.add(InsightItem(
                "🏆 Biggest single outflow: KES ${fmt(largeAmt)} to ${largest.entity} on ${largest.date.format(dateFmt)} — ${pct}% of your entire budget spend.",
                isPositive = pct < 20,
                requiresPremium = true
            ))
        }

        // P4. Unusual spike detection
        if (transactions.isNotEmpty() && daysElapsed > 3) {
            val dailySpend = transactions.filter { it.transactionAmount < 0 }
                .groupBy { it.date }
                .mapValues { (_, txs) -> txs.sumOf { tx -> abs(tx.transactionAmount) } }
            val avgDaily = dailySpend.values.average()
            val spike = dailySpend.maxByOrNull { it.value }
            if (spike != null && spike.value > avgDaily * 2.5) {
                val multiplier = (spike.value / avgDaily).toInt()
                insights.add(InsightItem(
                    "📍 On ${spike.key.format(dateFmt)} you spent KES ${fmt(spike.value)} — ${multiplier}x your daily average of KES ${fmt(avgDaily)}. This one day drove a significant share of your spend.",
                    isPositive = false,
                    requiresPremium = true
                ))
            }
        }

        // P5. Weekend vs weekday
        if (transactions.size >= 7) {
            val weekendSpend = transactions.filter {
                it.transactionAmount < 0 &&
                    (it.date.dayOfWeek.value >= 6)
            }.sumOf { abs(it.transactionAmount) }
            val weekdaySpend = transactions.filter {
                it.transactionAmount < 0 &&
                    (it.date.dayOfWeek.value < 6)
            }.sumOf { abs(it.transactionAmount) }
            val weekendDays = (0..ChronoUnit.DAYS.between(start, end).toInt())
                .count { start.plusDays(it.toLong()).dayOfWeek.value >= 6 }.coerceAtLeast(1)
            val weekdayDays = (totalDays - weekendDays).coerceAtLeast(1)
            val weAvg = weekendSpend / weekendDays
            val wdAvg = weekdaySpend / weekdayDays
            if (weAvg > wdAvg * 1.3) {
                insights.add(InsightItem(
                    "📅 You spend ${((weAvg / wdAvg - 1) * 100).toInt()}% more on weekends than weekdays in this budget (KES ${fmt(weAvg)}/day vs KES ${fmt(wdAvg)}/day). Weekend spending is your budget's biggest risk.",
                    isPositive = false,
                    requiresPremium = true
                ))
            } else if (wdAvg > weAvg * 1.3) {
                insights.add(InsightItem(
                    "📅 Your weekday spending (KES ${fmt(wdAvg)}/day) is ${((wdAvg / weAvg - 1) * 100).toInt()}% higher than weekends (KES ${fmt(weAvg)}/day) in this budget.",
                    isPositive = null,
                    requiresPremium = true
                ))
            }
        }

        // P6. Top entity concentration
        if (transactions.isNotEmpty() && spending > 0) {
            val topEntity = transactions.filter { it.transactionAmount < 0 }
                .groupBy { it.entity }
                .mapValues { (_, txs) -> txs.sumOf { tx -> abs(tx.transactionAmount) } }
                .maxByOrNull { it.value }
            if (topEntity != null) {
                val pct = (topEntity.value / spending * 100).toInt()
                if (pct >= 25) {
                    val saving = topEntity.value * 0.2
                    insights.add(InsightItem(
                        "💡 ${topEntity.key} accounts for ${pct}% of your budget spend (KES ${fmt(topEntity.value)}). Cutting it by 20% would save KES ${fmt(saving)} — enough to extend your budget by ${(saving / dailyAvg.coerceAtLeast(1.0)).toInt()} days.",
                        isPositive = null,
                        requiresPremium = true
                    ))
                }
            }
        }

        // P7. All-time budget performance (same name)
        viewModelScope.launch {
            val allBudgets = dbRepository.getAllBudgets().first()
                .filter { it.name == budget.name && it.id != budget.id }
            if (allBudgets.isNotEmpty()) {
                val metLimit = allBudgets.count { b ->
                    val s = b.startDate
                    val e = b.limitDate
                    val spent = dbRepository.getOutflowForCategory(b.categoryId, s, e).first()
                    spent <= b.budgetLimit
                }
                val successPct = (metLimit.toDouble() / allBudgets.size * 100).toInt()
                insights.add(InsightItem(
                    "📈 Across ${allBudgets.size} previous '${budget.name}' budget${if (allBudgets.size != 1) "s" else ""}, you stayed within limit $metLimit time${if (metLimit != 1) "s" else ""} (${successPct}% success rate).",
                    isPositive = successPct >= 50,
                    requiresPremium = true
                ))
                _uiState.update { it.copy(insights = _uiState.value.insights + insights.takeLast(1)) }
            }
        }

        return insights
    }

    private fun Budget.dailyAllowance(remaining: Double, daysLeft: Int) =
        if (daysLeft > 0) remaining / daysLeft else 0.0

    private fun Int.toPercent() = "${this}%"
    private val percentUsed: Int get() = _uiState.value.percentUsed

    private fun observePremium() {
        viewModelScope.launch {
            dataStoreRepository.getUserPreferences().collect { prefs ->
                val isPremium = prefs.permanent ||
                    (prefs.expiryDate != null &&
                        prefs.expiryDate.isAfter(java.time.LocalDateTime.now()))
                _uiState.update { it.copy(isPremium = isPremium) }
            }
        }
    }

    init {
        _uiState.update { it.copy(budgetId = budgetIdArg) }
        viewModelScope.launch {
            _uiState.update { it.copy(userDetails = dbRepository.getUsers().first()[0]) }
        }
        observePremium()
        observeBudgetAndSpending()
    }
}

