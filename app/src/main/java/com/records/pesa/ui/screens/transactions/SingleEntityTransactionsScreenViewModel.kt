package com.records.pesa.ui.screens.transactions

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.mapper.toTransactionItem
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.transactions
import com.records.pesa.service.transaction.TransactionService
import com.records.pesa.service.userAccount.UserAccountService
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
import kotlin.math.abs
import kotlin.math.absoluteValue

data class SingleEntityTransactionsScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val userId: String = "",
    val entity: String = "",
    val transactionType: String = "",
    val times: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val moneyIn: Boolean = true,
    val totalMoneyIn: Double = 0.0,
    val totalMoneyOut: Double = 0.0,
    val categoryId: Int? = null,
    val budgetId: Int? = null,
    val errorCode: Int = 0,
    val downLoadUri: Uri? = null,
    val transactions: List<TransactionItem> = emptyList(),
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL,
    val downloadingStatus: DownloadingStatus = DownloadingStatus.INITIAL
)
class SingleEntityTransactionsScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository,
    private val transactionService: TransactionService,
    private val userAccountService: UserAccountService
): ViewModel() {
    private val _uiState = MutableStateFlow(SingleEntityTransactionsScreenUiState())
    val uiState: StateFlow<SingleEntityTransactionsScreenUiState> = _uiState.asStateFlow()

    private val moneyDirection: String? = savedStateHandle[SingleEntityTransactionsScreenDestination.moneyDirection]

    fun loadStartupData() {
        _uiState.update {
            it.copy(
                userId = savedStateHandle[SingleEntityTransactionsScreenDestination.userId] ?: "",
                entity = savedStateHandle[SingleEntityTransactionsScreenDestination.entity] ?: "",
                transactionType = savedStateHandle[SingleEntityTransactionsScreenDestination.transactionType] ?: "",
                times = savedStateHandle[SingleEntityTransactionsScreenDestination.times] ?: "",
                startDate = savedStateHandle[SingleEntityTransactionsScreenDestination.startDate] ?: "",
                endDate = savedStateHandle[SingleEntityTransactionsScreenDestination.endDate] ?: "",
                moneyIn = moneyDirection == "in",
            )
        }
        getUserDetails()
    }

    fun getTransactions() {
        Log.d("MONEY_IN", moneyDirection.toString())
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        val query = transactionService.createUserTransactionQuery(
            userId = uiState.value.userDetails.backUpUserId.toInt(),
            entity = uiState.value.entity,
            categoryId = uiState.value.categoryId,
            budgetId = uiState.value.budgetId,
            transactionType = if(uiState.value.transactionType.lowercase() != "all types") uiState.value.transactionType else null,
            moneyDirection = if(moneyDirection != "null" && moneyDirection != null && moneyDirection != "all") moneyDirection else null,
            startDate = LocalDate.parse(uiState.value.startDate),
            endDate = LocalDate.parse(uiState.value.endDate),
            latest = true
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                transactionService.getUserTransactions(query).collect() {transactions ->
                    _uiState.update {
                        it.copy(
                            transactions = transactions.map { transaction -> transaction.toTransactionItem() },
                            totalMoneyIn = transactions.map { transactionWithCategories ->  transactionWithCategories.toTransactionItem() }.filter { transaction ->  transaction.transactionAmount > 0}.sumOf { transaction -> transaction.transactionAmount },
                            totalMoneyOut = transactions.map { transactionWithCategories -> transactionWithCategories.toTransactionItem() }.filter { transaction ->  transaction.transactionAmount < 0}.sumOf { transaction -> transaction.transactionAmount.absoluteValue },
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                }
            }
//            try {
//                val response = apiRepository.getTransactions(
//                    token = uiState.value.userDetails.token,
//                    userId = uiState.value.userDetails.userId,
//                    entity = uiState.value.entity,
//                    categoryId = uiState.value.categoryId,
//                    budgetId = uiState.value.budgetId,
//                    transactionType = if(uiState.value.transactionType.lowercase() != "all types") uiState.value.transactionType else null,
//                    latest = true,
//                    moneyDirection = if(moneyDirection != "null" && moneyDirection != null && moneyDirection != "all") moneyDirection else null,
//                    startDate = uiState.value.startDate,
//                    endDate = uiState.value.endDate
//                )
//
//                if(response.isSuccessful) {
//                    _uiState.update {
//                        it.copy(
//                            transactions = response.body()?.data?.transaction?.transactions!!,
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
//                    Log.e("GET_TRANSACTIONS_ERROR_RESPONSE", response.toString())
//                }
//
//            } catch (e: Exception) {
//                _uiState.update {
//                    it.copy(
//                        loadingStatus = LoadingStatus.FAIL
//                    )
//                }
//                Log.e("GET_TRANSACTIONS_EXCEPTION", e.toString())
//            }
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

    fun fetchReportAndSave(context: Context, saveUri: Uri?, reportType: String) {
        _uiState.update {
            it.copy(
                downloadingStatus = DownloadingStatus.LOADING
            )
        }
        viewModelScope.launch {
            val query = transactionService.createUserTransactionQuery(
                userId = uiState.value.userDetails.backUpUserId.toInt(),
                entity = uiState.value.entity,
                categoryId = uiState.value.categoryId,
                budgetId = uiState.value.budgetId,
                transactionType = if(uiState.value.transactionType.lowercase() != "all types") uiState.value.transactionType else null,
                moneyDirection = if(moneyDirection != "null" && moneyDirection != null && moneyDirection != "all") moneyDirection else null,
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

    fun resetDownloadingStatus() {
        _uiState.update {
            it.copy(
                downloadingStatus = DownloadingStatus.INITIAL
            )
        }
    }

    fun resetLoadingStatus() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.INITIAL
            )
        }
    }

    init {
        loadStartupData()
    }

}