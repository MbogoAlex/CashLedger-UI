package com.records.pesa.ui.screens.transactions

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.yml.charts.common.extensions.isNotNull
import com.records.pesa.db.DBRepository
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.transaction.SortedTransactionItem
import com.records.pesa.models.transaction.TransactionEditPayload
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.TransactionScreenTab
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate



data class TransactionsScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val transactions: List<TransactionItem> = emptyList(),
    val moneyInTransactions: List<TransactionItem> = emptyList(),
    val moneyOutTransactions: List<TransactionItem> = emptyList(),
    val moneyInSorted: List<SortedTransactionItem> = emptyList(),
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
    private val dbRepository: DBRepository
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
        viewModelScope.launch {
            try {
                val response = apiRepository.getTransactions(
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId,
                    entity = uiState.value.entity,
                    categoryId = uiState.value.categoryId,
                    budgetId = uiState.value.budgetId,
                    transactionType = if(uiState.value.transactionType.lowercase() != "all types") uiState.value.transactionType else null,
                    moneyDirection = uiState.value.moneyDirection,
                    latest = true,
                    startDate = if(uiState.value.defaultStartDate.isNullOrEmpty()) uiState.value.startDate else uiState.value.defaultStartDate,
                    endDate = if(uiState.value.defaultEndDate.isNullOrEmpty()) uiState.value.endDate else uiState.value.defaultEndDate
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            transactions = response.body()?.data?.transaction?.transactions!!,
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
        viewModelScope.launch {
            try {
                val response = apiRepository.getGroupedByEntityTransactions(
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId,
                    entity = uiState.value.entity,
                    categoryId = uiState.value.categoryId,
                    budgetId = uiState.value.budgetId,
                    transactionType = if(uiState.value.transactionType.lowercase() != "all types") uiState.value.transactionType else null,
                    moneyDirection = uiState.value.moneyDirection,
                    startDate = if(uiState.value.defaultStartDate.isNullOrEmpty()) uiState.value.startDate else uiState.value.defaultStartDate!!,
                    endDate = if(uiState.value.defaultEndDate.isNullOrEmpty()) uiState.value.endDate else uiState.value.defaultEndDate!!
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            moneyInSorted = response.body()?.data?.transaction?.transactions!!,
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

    fun fetchReportAndSave(context: Context, saveUri: Uri?, reportType: String) {
        _uiState.update {
            it.copy(
                downloadingStatus = DownloadingStatus.LOADING
            )
        }
        viewModelScope.launch {
            try {
                val response = apiRepository.getAllTransactionsReport(
                    userId = uiState.value.userDetails.userId,
                    token = uiState.value.userDetails.token,
                    entity = uiState.value.entity,
                    categoryId = uiState.value.categoryId,
                    budgetId = uiState.value.budgetId,
                    transactionType = if(uiState.value.transactionType.lowercase() != "all types") uiState.value.transactionType else null,
                    moneyDirection = uiState.value.moneyDirection,
                    reportType = reportType,
                    startDate = uiState.value.startDate,
                    endDate = uiState.value.endDate,
                )
                if (response.isSuccessful) {
                    val pdfBytes = response.body()?.bytes()
                    if (pdfBytes != null && pdfBytes.isNotEmpty()) {
                        savePdfToUri(context, pdfBytes, saveUri)
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
                } else {
                    Log.e("REPORT_GENERATION_ERROR_RESPONSE", "Response not successful: $response")
                    _uiState.update {
                        it.copy(
                            downloadingStatus = DownloadingStatus.FAIL
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("REPORT_GENERATION_ERROR_EXCEPTION", "Exception: ${e.message}")
                _uiState.update {
                    it.copy(
                        downloadingStatus = DownloadingStatus.FAIL
                    )
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

    init {
        setInitialDates()
        getUserDetails()

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