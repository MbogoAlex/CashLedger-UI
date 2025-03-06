package com.records.pesa.ui.screens.transactions

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.yml.charts.common.extensions.isNotNull
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.db.models.userPreferences
import com.records.pesa.mapper.toTransactionItem
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.transaction.SortedTransactionItem
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.TransactionScreenTab
import com.records.pesa.service.transaction.TransactionService
import com.records.pesa.service.userAccount.UserAccountService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.absoluteValue


data class TransactionsScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val preferences: UserPreferences = userPreferences,
    val transactions: List<TransactionItem> = emptyList(),
    val moneyInTransactions: List<TransactionItem> = emptyList(),
    val moneyOutTransactions: List<TransactionItem> = emptyList(),
    val groupedTransactionItems: List<SortedTransactionItem> = emptyList(),
    val moneyOutSorted: List<SortedTransactionItem> = emptyList(),
    val nickName: String = "",
    val entity: String = "",
    val transactionType: String = "All types",
    val defaultTransactionType: String? = null,
    val selectableTransactionTypes: List<String> = emptyList(),
    val totalMoneyIn: Double = 0.0,
    val totalMoneyOut: Double = 0.0,
    val currentDate: String = "2024-06-15",
    val firstDayOfMonth: String = "2024-06-15",
    val defaultStartDate: String? = null,
    val defaultEndDate: String? = null,
    val startDate: String = "2024-06-15",
    val endDate: String = "2024-06-15",
    val datesSet: Boolean = false,
    val categoryId: Int? = null,
    val budgetId: Int? = null,
    val categoryName: String? = null,
    val budgetName: String? = null,
    val errorCode: Int = 0,
    val comment: String = "",
    val downLoadUri: Uri? = null,
    val moneyDirection: String? = null,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL,
    val downloadingStatus: DownloadingStatus = DownloadingStatus.INITIAL,
)
class TransactionsScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository,
    private val transactionService: TransactionService,
    private val userAccountService: UserAccountService
): ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsScreenUiState())
    val uiState: StateFlow<TransactionsScreenUiState> = _uiState.asStateFlow()

    private val defaultStartDate: String? = savedStateHandle[TransactionsScreenDestination.startDate]
    private val defaultEndDate: String? = savedStateHandle[TransactionsScreenDestination.endDate]

    private val categoryId: String? = savedStateHandle[TransactionsScreenDestination.categoryId]
    private val budgetId: String? = savedStateHandle[TransactionsScreenDestination.budgetId]

    private val transactionType: String? = savedStateHandle[TransactionsScreenDestination.transactionType]
    private val moneyDirection: String? = savedStateHandle[TransactionsScreenDestination.moneyDirection]

    private var filterJob: Job? = null

    fun setInitialDates() {
        val currentDate = LocalDate.now()
        val firstDayOfMonth = currentDate.withDayOfMonth(1)
        var startDate = firstDayOfMonth
        var endDate = currentDate
        _uiState.update {
            it.copy(
                currentDate = currentDate.toString(),
                firstDayOfMonth = firstDayOfMonth.toString(),
                startDate = defaultStartDate ?: startDate.toString(),
                endDate = defaultEndDate ?: endDate.toString(),
                categoryId = categoryId?.toInt(),
                budgetId = budgetId?.toInt(),
                categoryName = savedStateHandle[TransactionsScreenDestination.categoryName],
                budgetName = savedStateHandle[TransactionsScreenDestination.budgetName],
                defaultStartDate = defaultStartDate,
                moneyDirection = moneyDirection,
                defaultEndDate = defaultEndDate
            )
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 101 // Arbitrary integer for the permission request code
    }

    fun changeStartDate(startDate: LocalDate, tab: TransactionScreenTab) {
        _uiState.update {
            it.copy(
                startDate = startDate.toString(),
                loadingStatus = LoadingStatus.LOADING
            )
        }
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            delay(500L)
            when(tab) {
                TransactionScreenTab.ALL_TRANSACTIONS -> getTransactions()
                TransactionScreenTab.GROUPED -> getGroupedByEntityTransactions()
            }
        }
    }

    fun changeEndDate(endDate: LocalDate, tab: TransactionScreenTab) {
        _uiState.update {
            it.copy(
                endDate = endDate.toString(),
                loadingStatus = LoadingStatus.LOADING
            )
        }
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            delay(500)
            when(tab) {
                TransactionScreenTab.ALL_TRANSACTIONS -> getTransactions()
                TransactionScreenTab.GROUPED -> getGroupedByEntityTransactions()
            }
        }
    }

    fun changeEntity(entity: String, tab: TransactionScreenTab) {
        _uiState.update {
            it.copy(
                entity = entity,
                loadingStatus = LoadingStatus.LOADING
            )
        }
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            delay(500L)
            when(tab) {
                TransactionScreenTab.ALL_TRANSACTIONS -> getTransactions()
                TransactionScreenTab.GROUPED -> getGroupedByEntityTransactions()
            }
        }
    }

    fun clearSearch(tab: TransactionScreenTab) {
        _uiState.update {
            it.copy(
                entity = "",
                loadingStatus = LoadingStatus.LOADING
            )
        }
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            delay(500L)
            when(tab) {
                TransactionScreenTab.ALL_TRANSACTIONS -> getTransactions()
                TransactionScreenTab.GROUPED -> getGroupedByEntityTransactions()
            }
        }
    }

    fun changeTransactionType(transactionType: String, tab: TransactionScreenTab) {
        _uiState.update {
            it.copy(
                transactionType = if(transactionType.lowercase() == "buy goods and services") "Buy Goods and Services (till)" else if(transactionType.lowercase() == "withdrawal") "Withdraw Cash" else transactionType,
                loadingStatus = LoadingStatus.LOADING
            )
        }
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            delay(500L)
            when(tab) {
                TransactionScreenTab.ALL_TRANSACTIONS -> getTransactions()
                TransactionScreenTab.GROUPED -> getGroupedByEntityTransactions()
            }
        }

    }

    fun updateNickname(name: String) {
        _uiState.update {
            it.copy(
                nickName = name
            )
        }
    }

    fun getTransactions() {
        Log.i("CALLED", "SEARCHING FOR ${uiState.value.entity}")
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }

        val query = transactionService.createUserTransactionQuery(
            userId = uiState.value.userDetails.userId,
            entity = uiState.value.entity,
            categoryId = uiState.value.categoryId,
            budgetId = uiState.value.budgetId,
            transactionType = if(uiState.value.transactionType.lowercase() != "all types") uiState.value.transactionType else null,
            moneyDirection = uiState.value.moneyDirection,
            startDate = LocalDate.parse(uiState.value.startDate),
            endDate = LocalDate.parse(uiState.value.endDate),
            latest = true
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    transactionService.getUserTransactions(query).collect() { transactions ->
                        _uiState.update {
                            it.copy(
                                transactions = transactions.map { transactionWithCategories ->  transactionWithCategories.toTransactionItem() },
                                totalMoneyIn = transactions.map { transactionWithCategories ->  transactionWithCategories.toTransactionItem() }.filter { transaction ->  transaction.transactionAmount > 0}.sumOf { transaction -> transaction.transactionAmount },
                                totalMoneyOut = transactions.map { transactionWithCategories -> transactionWithCategories.toTransactionItem() }.filter { transaction ->  transaction.transactionAmount < 0}.sumOf { transaction -> transaction.transactionAmount.absoluteValue },
                                loadingStatus = LoadingStatus.SUCCESS
                            )
                        }
                    }
                    Log.d("TRANSACTIONS_SIZE", uiState.value.transactions.size.toString())
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("GetTransactionsException", e.toString())
                }
            }
        }
    }



    fun getMoneyInTransactions() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        viewModelScope.launch {
            try {
                val response = apiRepository.getMoneyIn(
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId,
                    entity = uiState.value.entity,
                    categoryId = uiState.value.categoryId,
                    budgetId = uiState.value.budgetId,
                    transactionType = if(uiState.value.transactionType.lowercase() != "all types") uiState.value.transactionType else null,
                    moneyIn = true,
                    latest = true,
                    startDate = if(uiState.value.defaultStartDate.isNullOrEmpty()) uiState.value.startDate else uiState.value.defaultStartDate!!,
                    endDate = if(uiState.value.defaultEndDate.isNullOrEmpty()) uiState.value.endDate else uiState.value.defaultEndDate!!
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            moneyInTransactions = response.body()?.data?.transaction?.transactions!!,
                            totalMoneyIn = response.body()?.data?.transaction?.totalMoneyIn!!,
                            totalMoneyOut = response.body()?.data?.transaction?.totalMoneyOut!!,
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL,
                            errorCode = response.code()
                        )
                    }
                    Log.e("GetTransactionsResponseError", response.toString())
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("GetTransactionsException", e.toString())
            }
        }
    }

    fun getMoneyOutTransactions() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        viewModelScope.launch {
            try {
                val response = apiRepository.getMoneyIn(
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId,
                    entity = uiState.value.entity,
                    categoryId = uiState.value.categoryId,
                    budgetId = uiState.value.budgetId,
                    transactionType = if(uiState.value.transactionType.lowercase() != "all types") uiState.value.transactionType else null,
                    moneyIn = true,
                    latest = true,
                    startDate = if(uiState.value.defaultStartDate.isNullOrEmpty()) uiState.value.startDate else uiState.value.defaultStartDate!!,
                    endDate = if(uiState.value.defaultEndDate.isNullOrEmpty()) uiState.value.endDate else uiState.value.defaultEndDate!!
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            moneyOutTransactions = response.body()?.data?.transaction?.transactions!!,
                            totalMoneyIn = response.body()?.data?.transaction?.totalMoneyIn!!,
                            totalMoneyOut = response.body()?.data?.transaction?.totalMoneyOut!!,
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("GetTransactionsResponseError", response.toString())
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("GetTransactionsException", e.toString())
            }
        }
    }

    fun getGroupedByEntityTransactions() {
        Log.i("LOADING_TRANSACTIONS", "ID: ${uiState.value.userDetails.userId}")
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        val query = transactionService.createUserTransactionQuery(
            userId = uiState.value.userDetails.userId,
            entity = uiState.value.entity,
            categoryId = uiState.value.categoryId,
            budgetId = uiState.value.budgetId,
            transactionType = if(uiState.value.transactionType.lowercase() != "all types") uiState.value.transactionType else null,
            moneyDirection = uiState.value.moneyDirection,
            startDate = LocalDate.parse(uiState.value.startDate),
            endDate = LocalDate.parse(uiState.value.endDate),
            latest = true
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    transactionService.getUserTransactions(query).collect() {transactions ->
                        transformTransactions(transactions.map { transactionWithCategories -> transactionWithCategories.toTransactionItem() })
                    }

                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("failedToLoadGroupedTransactions", e.toString())
                }

            }
//            try {
//                val response = apiRepository.getGroupedByEntityTransactions(
//                    token = uiState.value.userDetails.token,
//                    userId = uiState.value.userDetails.userId,
//                    entity = uiState.value.entity,
//                    categoryId = uiState.value.categoryId,
//                    budgetId = uiState.value.budgetId,
//                    transactionType = if(uiState.value.transactionType.lowercase() != "all types") uiState.value.transactionType else null,
//                    moneyDirection = uiState.value.moneyDirection,
//                    startDate = if(uiState.value.defaultStartDate.isNullOrEmpty()) uiState.value.startDate else uiState.value.defaultStartDate!!,
//                    endDate = if(uiState.value.defaultEndDate.isNullOrEmpty()) uiState.value.endDate else uiState.value.defaultEndDate!!
//                )
//                if(response.isSuccessful) {
//                    _uiState.update {
//                        it.copy(
//                            groupedTransactionItems = response.body()?.data?.transaction?.transactions!!,
//                            totalMoneyIn = response.body()?.data?.transaction?.totalMoneyIn!!,
//                            totalMoneyOut = response.body()?.data?.transaction?.totalMoneyOut!!,
//                            loadingStatus = LoadingStatus.SUCCESS
//                        )
//                    }
//                } else {
//                    _uiState.update {
//                        it.copy(
//                            loadingStatus = LoadingStatus.FAIL,
//                            errorCode = response.code()
//                        )
//                    }
//                    Log.e("GetTransactionsResponseError", response.toString())
//                }
//
//            } catch (e: Exception) {
//                _uiState.update {
//                    it.copy(
//                        loadingStatus = LoadingStatus.FAIL
//                    )
//                }
//                Log.e("GetTransactionsException", e.toString())
//            }
        }
    }

    fun fetchReportAndSave(context: Context, saveUri: Uri?, reportType: String) {
        _uiState.update {
            it.copy(
                downloadingStatus = DownloadingStatus.LOADING
            )
        }
        viewModelScope.launch {
            val query = transactionService.createUserTransactionQuery(
                userId = uiState.value.userDetails.userId,
                entity = uiState.value.entity,
                categoryId = uiState.value.categoryId,
                budgetId = uiState.value.budgetId,
                transactionType = if(uiState.value.transactionType.lowercase() != "all types") uiState.value.transactionType else null,
                moneyDirection = uiState.value.moneyDirection,
                startDate = LocalDate.parse(uiState.value.startDate),
                endDate = LocalDate.parse(uiState.value.endDate),
                latest = true
            )
            withContext(Dispatchers.IO) {
                try {
                    val report = transactionService.generateAllTransactionsReport(
                        query = query,
                        userAccount = userAccountService.getUserAccount(userId = uiState.value.userDetails.userId).first(),
                        reportType = reportType,
                        startDate = uiState.value.startDate,
                        endDate = uiState.value.endDate,
                        context = context
                    )
                    if (report != null && report.isNotEmpty()) {
                        savePdfToUri(context, report, saveUri)
                        _uiState.update {
                            it.copy(
                                downloadingStatus = DownloadingStatus.SUCCESS
                            )
                        }
                    } else {
                        Log.e("REPORT_GENERATION", "PDF Bytes are null or empty")
                        _uiState.update {
                            it.copy(
                                downloadingStatus = DownloadingStatus.FAIL
                            )
                        }
                    }

                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            downloadingStatus = DownloadingStatus.FAIL
                        )
                    }
                    Log.e("REPORT_GENERATION_ERROR_EXCEPTION", "Exception: ${e.toString()}")
                }
            }
        }
    }

    private fun savePdfToUri(context: Context, pdfBytes: ByteArray, uri: Uri?) {
        if (uri == null) {
            Log.e("SAVE_PDF_TO_URI", "Uri is null, cannot save PDF")
            return
        }
        try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(pdfBytes)
                Log.i("SAVE_PDF_TO_URI", "PDF saved successfully to: $uri")
                _uiState.update {
                    it.copy(
                        downLoadUri = uri
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("SAVE_PDF_TO_URI_ERROR", "Exception: ${e.message}")
        }
    }

    fun resetLoadingStatus() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.INITIAL
            )
        }
    }


    fun resetDownloadingStatus() {
        _uiState.update {
            it.copy(
                downloadingStatus = DownloadingStatus.INITIAL
            )
        }
    }

    fun changeComment(comment: String) {
        _uiState.update {
            it.copy(
                comment = comment
            )
        }
    }

    fun getUserDetails() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    userDetails = dbRepository.getUsers().first()[0]
                )
            }
            while(uiState.value.userDetails.userId == 0) {
                delay(1000)
            }
            getTransactions()
        }
    }

    fun transformTransactions(transactions: List<TransactionItem>) {
        // Initialize totalMoneyIn and totalMoneyOut
        var totalMoneyIn = 0.0
        var totalMoneyOut = 0.0

        // Group transactions by entity
        val groupedByEntity = transactions.groupBy { it.entity }

        // Map each group to a SortedTransactionItem
        val sortedTransactionItems = groupedByEntity.map { (entity, groupedTransactions) ->
            val times = groupedTransactions.size
            val timesOut = groupedTransactions.count { it.transactionAmount < 0 }
            val timesIn = groupedTransactions.count { it.transactionAmount > 0 }
            val totalOut = groupedTransactions.filter { it.transactionAmount < 0 }
                .sumOf { it.transactionAmount.absoluteValue }
            val totalIn = groupedTransactions.filter { it.transactionAmount > 0 }
                .sumOf { it.transactionAmount }

            // Update the totals
            totalMoneyIn += totalIn
            totalMoneyOut += totalOut

            val transactionCost = groupedTransactions.sumOf { it.transactionCost }
            val nickName = groupedTransactions.firstOrNull { it.nickName != null }?.nickName

            SortedTransactionItem(
                transactionType = groupedTransactions.first().transactionType,
                times = times,
                timesOut = timesOut,
                timesIn = timesIn,
                totalOut = totalOut,
                totalIn = totalIn,
                transactionCost = transactionCost,
                entity = entity,
                nickName = nickName
            )
        }

        val sortedInDescendingOrder = sortedTransactionItems.sortedByDescending {
            it.totalIn + it.totalOut.absoluteValue
        }

        _uiState.update {
            it.copy(
                groupedTransactionItems = sortedInDescendingOrder,
                totalMoneyIn = totalMoneyIn,
                totalMoneyOut = totalMoneyOut,
                loadingStatus = LoadingStatus.SUCCESS
            )
        }

        // You can use totalMoneyIn and totalMoneyOut as needed in your code
        println("Total Money In: $totalMoneyIn")
        println("Total Money Out: $totalMoneyOut")

    }

    private fun getUserPreferences() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbRepository.getUserPreferences().collect() { preferences ->
                    _uiState.update {
                        it.copy(
                            preferences = preferences!!
                        )
                    }
                }
            }
        }
    }


    init {
        setInitialDates()
        getUserDetails()
        getUserPreferences()

        val selectableTypes = when(moneyDirection) {
            "in" -> {
                listOf(
                    "All types",
                    "Send Money",
                    "Mshwari",
                    "Hustler fund",
                    "Reversal",
                    "KBC Mpesa account",
                    "Deposit",
                )
            }
            "out" -> {
                listOf(
                    "All types",
                    "Send Money",
                    "Pay Bill",
                    "Buy goods and Services",
                    "Pochi la Biashara",
                    "Airtime & Bundles",
                    "Mshwari",
                    "Fuliza",
                    "Hustler fund",
                    "Reversal",
                    "KBC Mpesa account",
                    "Lock savings",
                    "Withdrawal"
                )
            }
            else -> {
                listOf(
                    "All types",
                    "Send Money",
                    "Pay Bill",
                    "Buy goods and Services",
                    "Pochi la Biashara",
                    "Airtime & Bundles",
                    "Mshwari",
                    "Fuliza",
                    "Hustler fund",
                    "Reversal",
                    "KBC Mpesa account",
                    "Deposit",
                    "Withdrawal"
                )
            }
        }



        _uiState.update {
            it.copy(
                defaultTransactionType = transactionType,
                selectableTransactionTypes = selectableTypes,
                transactionType = if(transactionType.isNotNull()) if(transactionType == "All money in") "All types" else if(transactionType == "All money out") "All types" else transactionType!! else "All types",
                moneyDirection = moneyDirection

            )
        }
    }
}