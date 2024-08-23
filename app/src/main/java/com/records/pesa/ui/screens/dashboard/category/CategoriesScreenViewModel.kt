package com.records.pesa.ui.screens.dashboard.category

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.TransactionCategory
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.TransactionScreenTab
import com.records.pesa.ui.screens.transactions.TransactionsScreenDestination
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CategoriesScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val categories: List<TransactionCategory> = emptyList(),
    val name: String = "",
    val orderOptions: List<String> = listOf("latest", "amount"),
    val orderBy: String = "latest",
    val startDate: String = "2024-06-15",
    val endDate: String = "2024-06-15",
    val selectedCategories: List<Int> = emptyList(),
    val downLoadUri: Uri? = null,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL,
    val downloadingStatus: DownloadingStatus = DownloadingStatus.INITIAL
)
class CategoriesScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(CategoriesScreenUiState())
    val uiState: StateFlow<CategoriesScreenUiState> = _uiState.asStateFlow()

    private val selectedCategories = mutableStateListOf<Int>()

    fun setInitialDates() {
        val currentDate = LocalDate.now()
        val firstDayOfMonth = currentDate.withDayOfMonth(1)
        var startDate = firstDayOfMonth
        var endDate = currentDate
        _uiState.update {
            it.copy(
                startDate = startDate.toString(),
                endDate = endDate.toString(),
            )
        }
    }

    fun changeStartDate(startDate: LocalDate) {
        _uiState.update {
            it.copy(
                startDate = startDate.toString(),
            )
        }

    }

    fun changeEndDate(endDate: LocalDate) {
        _uiState.update {
            it.copy(
                endDate = endDate.toString(),
            )
        }

    }

    fun addCategoryId(id: Int) {
        selectedCategories.add(id)
        _uiState.update {
            it.copy(
                selectedCategories = selectedCategories
            )
        }
    }

    fun removeCategoryId(id: Int) {
        selectedCategories.remove(id)
        _uiState.update {
            it.copy(
                selectedCategories = selectedCategories
            )
        }
    }

    fun updateOrderBy(orderBy: String) {
        _uiState.update {
            it.copy(
                orderBy = orderBy
            )
        }
        getUserCategories()
    }

    fun updateName(name: String) {
        _uiState.update {
            it.copy(
                name = name
            )
        }
        getUserCategories()
    }

    fun clearName() {
        _uiState.update {
            it.copy(
                name = ""
            )
        }
        getUserCategories()
    }

    fun getUserCategories() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        viewModelScope.launch {
            try {
                val response = apiRepository.getUserCategories(
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId,
                    categoryId = null,
                    name = uiState.value.name,
                    orderBy = uiState.value.orderBy
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            categories = response.body()?.data?.category!!,
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("getUserCategoriesErrorResponse", response.toString())
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("getUserCategoriesErrorException", e.toString())
            }
        }
    }

    fun fetchReportAndSave(context: Context, saveUri: Uri?, reportType: String) {
        val categoryReportPayload = CategoryReportPayload(
            userId = uiState.value.userDetails.userId,
            categoryIds = uiState.value.selectedCategories,
            reportType = reportType,
            startDate = uiState.value.startDate,
            lastDate = uiState.value.endDate
        )
        _uiState.update {
            it.copy(
                downloadingStatus = DownloadingStatus.LOADING
            )
        }
        viewModelScope.launch {
            try {
                val response = apiRepository.generateReportForMultipleCategories(
                    token = uiState.value.userDetails.token,
                    categoryReportPayload = categoryReportPayload
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

    private fun getUserDetails() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    userDetails = dbRepository.getUsers().first()[0]
                )
            }
            while (uiState.value.userDetails.userId == 0) {
                delay(1000)
            }
            getUserCategories()
        }
    }

    fun resetDownloadingStatus() {
        _uiState.update {
            it.copy(
                downloadingStatus = DownloadingStatus.INITIAL
            )
        }
    }

    init {
        getUserDetails()
        setInitialDates()
    }

}