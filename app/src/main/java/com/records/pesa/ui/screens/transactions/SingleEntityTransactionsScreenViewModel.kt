package com.records.pesa.ui.screens.transactions

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
        viewModelScope.launch {
            try {
                val response = apiRepository.getTransactions(
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId,
                    entity = uiState.value.entity,
                    categoryId = uiState.value.categoryId,
                    budgetId = uiState.value.budgetId,
                    transactionType = if(uiState.value.transactionType.lowercase() != "all types") uiState.value.transactionType else null,
                    latest = true,
                    moneyDirection = if(moneyDirection != "null" && moneyDirection != null && moneyDirection != "all") moneyDirection else null,
                    startDate = uiState.value.startDate,
                    endDate = uiState.value.endDate
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
                    Log.e("GET_TRANSACTIONS_ERROR_RESPONSE", response.toString())
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("GET_TRANSACTIONS_EXCEPTION", e.toString())
            }
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

    fun fetchReportAndSave(context: Context, saveUri: Uri?) {

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
                    moneyDirection = if(moneyDirection != "null" && moneyDirection != null && moneyDirection != "all") moneyDirection else null,
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
                                downloadingStatus = DownloadingStatus.FAIL,
                                errorCode = response.code()
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