package com.records.pesa.ui.screens.dashboard.category

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.mapper.toResponseTransactionCategory
import com.records.pesa.models.TransactionCategory
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.datastore.DataStoreRepository
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.service.category.CategoryService
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
    val isPremium: Boolean = false,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL,
    val downloadingStatus: DownloadingStatus = DownloadingStatus.INITIAL
)
class CategoriesScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository,
    private val categoryService: CategoryService,
    private val transactionService: TransactionService,
    private val userAccountService: UserAccountService,
    private val dataStoreRepository: DataStoreRepository
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

    fun clearSelectedCategories() {
        selectedCategories.clear()
        _uiState.update {
            it.copy(selectedCategories = selectedCategories)
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
            withContext(Dispatchers.IO) {
                try {
                    categoryService.getAllCategories().collect() {categories ->
                        _uiState.update {
                            it.copy(
                                categories = categories.map { category ->
                                    Log.d("TRANSACTIONS_SIZE", category.toResponseTransactionCategory().transactions.size.toString())
                                    category.toResponseTransactionCategory()
                                },
                                loadingStatus = LoadingStatus.SUCCESS
                            )
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("errorLoadingCategories", e.toString())
                }
            }
//            try {
//
//                val response = apiRepository.getUserCategories(
//                    token = uiState.value.userDetails.token,
//                    userId = uiState.value.userDetails.userId,
//                    categoryId = null,
//                    name = uiState.value.name,
//                    orderBy = uiState.value.orderBy
//                )
//                if(response.isSuccessful) {
//                    _uiState.update {
//                        it.copy(
//                            categories = response.body()?.data?.category!!,
//                            loadingStatus = LoadingStatus.SUCCESS
//                        )
//                    }
//                } else {
//                    _uiState.update {
//                        it.copy(
//                            loadingStatus = LoadingStatus.FAIL
//                        )
//                    }
//                    Log.e("getUserCategoriesErrorResponse", response.toString())
//                }
//            } catch (e: Exception) {
//                _uiState.update {
//                    it.copy(
//                        loadingStatus = LoadingStatus.FAIL
//                    )
//                }
//                Log.e("getUserCategoriesErrorException", e.toString())
//            }
        }
    }

    fun fetchReportAndSave(
        context: Context,
        saveUri: Uri?,
        reportType: String,
        startDate: LocalDate,
        endDate: LocalDate
    ) {
        _uiState.update { it.copy(downloadingStatus = DownloadingStatus.LOADING) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val selectedCategoryIds = uiState.value.selectedCategories
                    // 1 — M-PESA transactions via raw query
                    val query = transactionService.createUserTransactionQueryForMultipleCategories(
                        userId = uiState.value.userDetails.backUpUserId.toInt(),
                        entity = null,
                        categoryIds = selectedCategoryIds,
                        budgetId = null,
                        transactionType = null,
                        startDate = startDate,
                        endDate = endDate,
                        latest = true
                    )
                    val mpesaTxs = transactionService.getTransactionsForReport(query)

                    // 2 — Manual transactions for each selected category
                    val manualTxs = selectedCategoryIds.flatMap { catId ->
                        dbRepository.getManualTransactionsForCategoryOnce(catId)
                    }.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }

                    // 3 — Build combined model list
                    val models = ArrayList<com.records.pesa.service.transaction.function.AllTransactionsReportModel>()
                    for (tx in mpesaTxs) {
                        val cat = tx.categories.joinToString(", ") { it.name }.ifBlank { "-" }
                        val model = com.records.pesa.service.transaction.function.AllTransactionsReportModel()
                        model.datetime = "${tx.transaction.date} ${tx.transaction.time}"
                        model.transactionType = tx.transaction.transactionType
                        model.category = cat
                        model.entity = tx.transaction.entity
                        if (tx.transaction.transactionAmount > 0) {
                            model.moneyIn = "Ksh${tx.transaction.transactionAmount}"
                            model.moneyOut = "-"
                        } else {
                            model.moneyIn = "-"
                            model.moneyOut = "Ksh${kotlin.math.abs(tx.transaction.transactionAmount)}"
                        }
                        model.transactionCost = if (tx.transaction.transactionCost != 0.0)
                            "Ksh${kotlin.math.abs(tx.transaction.transactionCost)}" else "-"
                        models.add(model)
                    }
                    for (tx in manualTxs) {
                        val model = com.records.pesa.service.transaction.function.AllTransactionsReportModel()
                        model.datetime = "${tx.date} ${tx.time ?: ""}"
                        model.transactionType = tx.transactionTypeName
                        model.category = "-"
                        model.entity = tx.memberName
                        if (!tx.isOutflow) {
                            model.moneyIn = "Ksh${tx.amount}"
                            model.moneyOut = "-"
                        } else {
                            model.moneyIn = "-"
                            model.moneyOut = "Ksh${tx.amount}"
                        }
                        model.transactionCost = "-"
                        models.add(model)
                    }
                    // Sort newest first
                    models.sortByDescending { it.datetime }

                    val userAccount = userAccountService.getUserAccount(userId = uiState.value.userDetails.userId).first()
                    val report = transactionService.generateReportFromPrebuiltModels(
                        models = models,
                        userAccount = userAccount,
                        reportType = reportType,
                        startDate = startDate.toString(),
                        endDate = endDate.toString(),
                        context = context
                    )
                    if (report != null && report.isNotEmpty()) {
                        savePdfToUri(context, report, saveUri)
                        _uiState.update { it.copy(downloadingStatus = DownloadingStatus.SUCCESS) }
                    } else {
                        _uiState.update { it.copy(downloadingStatus = DownloadingStatus.FAIL) }
                    }
                } catch (e: Exception) {
                    Log.e("REPORT_GENERATION_ERROR", "Exception: ${e.message}")
                    _uiState.update { it.copy(downloadingStatus = DownloadingStatus.FAIL) }
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
        viewModelScope.launch {
            dataStoreRepository.getUserPreferences().collect { prefs ->
                val premium = prefs.permanent ||
                    (prefs.expiryDate?.isAfter(java.time.LocalDateTime.now()) == true)
                _uiState.update { it.copy(isPremium = premium) }
            }
        }
    }

}