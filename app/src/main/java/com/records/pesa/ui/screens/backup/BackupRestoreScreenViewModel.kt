package com.records.pesa.ui.screens.backup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.payment.supabase.SupabaseCategoryKeyword
import com.records.pesa.models.payment.supabase.SupabaseTransaction
import com.records.pesa.models.payment.supabase.SupabaseTransactionCategory
import com.records.pesa.models.payment.supabase.SupabaseTransactionCategoryCrossRef
import com.records.pesa.models.payment.supabase.mapper.toCategoryKeyword
import com.records.pesa.models.payment.supabase.mapper.toTransaction
import com.records.pesa.models.payment.supabase.mapper.toTransactionCategory
import com.records.pesa.models.payment.supabase.mapper.toTransactionCategoryCrossRef
import com.records.pesa.network.SupabaseClient.client
import com.records.pesa.service.category.CategoryService
import com.records.pesa.service.transaction.TransactionService
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BackupRestoreScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val transactions: List<SupabaseTransaction> = emptyList(),
    val categories: List<SupabaseTransactionCategory> = emptyList(),
    val categoryKeywords: List<SupabaseCategoryKeyword> = emptyList(),
    val totalItems: Int = 0,
    val itemsInserted: Int = 0,
    val backupMessage: String = "",
    val itemsNotBackedUp: Int = 0,
    val transactionsNotBackedUp: Int = 0,
    val categoriesNotBackedUp: Int = 0,
    val categoryKeywordsNotBackedUp: Int = 0,
    val categoryMappingsNotBackedUp: Int = 0,
    val totalItemsToRestore: Int = 0,
    val totalItemsRestored: Int = 0,
    val transactionWithCategoryMappings: List<SupabaseTransactionCategoryCrossRef> = emptyList(),
    val backupStatus: BackupStatus = BackupStatus.INITIAL,
    val restoreStatus: RestoreStatus = RestoreStatus.INITIAL
)

class BackupRestoreScreenViewModel(
    private val dbRepository: DBRepository,
    private val transactionService: TransactionService,
    private val categoryService: CategoryService,
): ViewModel() {

    private val _uiState = MutableStateFlow(BackupRestoreScreenUiState())
    val uiState: StateFlow<BackupRestoreScreenUiState> = _uiState.asStateFlow()

    fun getUserDetails() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbRepository.getUsers().collect() {userDetails ->
                    _uiState.update {
                        it.copy(
                            userDetails = if(userDetails.isNotEmpty()) userDetails.first() else UserDetails(),
                        )
                    }
                }

            }
        }
    }

    fun initializeData() {
        getUserDetails()
        viewModelScope.launch {
            while (uiState.value.userDetails.userId == 0) {
                delay(1000)
            }
            restore()
        }
    }

    fun restore() {
        _uiState.update {
            it.copy(
                restoreStatus = RestoreStatus.LOADING
            )
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // restore transactions
                    val transactions = client.postgrest["transaction"].select {
                        filter {
                            eq("userId", uiState.value.userDetails.userId)
                        }
                    }.decodeList<SupabaseTransaction>()

                    Log.d("LOADED_ITEMS", "transactions: ${transactions.size}")

                    // restore categories
                    val categories = client.postgrest["transactionCategory"].select{
                        filter {
                            eq("userId", uiState.value.userDetails.userId)
                        }
                    }.decodeList<SupabaseTransactionCategory>()

                    Log.d("LOADED_ITEMS", "categories: ${categories.size}")

                    // restore category keywords
                    val categoryKeywords = client.postgrest["categoryKeyword"].select {
                        filter {
                            eq("userId", uiState.value.userDetails.userId)
                        }
                    }.decodeList<SupabaseCategoryKeyword>()

                    Log.d("LOADED_ITEMS", "categoryKeywords: ${categoryKeywords.size}")

                    // restore transactionCategoryCrossRef
                    val categoryMappings = client.postgrest["transactionCategoryCrossRef"].select {
                        filter {
                            eq("userId", uiState.value.userDetails.userId)
                        }

                    }.decodeList<SupabaseTransactionCategoryCrossRef>()

                    Log.d("LOADED_ITEMS", "categoryMappings: ${categoryMappings.size}")

                    _uiState.update {
                        it.copy(
                            totalItemsToRestore = transactions.size + categories.size + categoryKeywords.size + categoryMappings.size
                        )
                    }

                    Log.d("LOADED_ITEMS", uiState.value.totalItemsToRestore.toString())

                    for(transaction in transactions.map { it.toTransaction() }) {
                        transactionService.insertTransaction(transaction)
                        _uiState.update {
                            it.copy(
                                totalItemsRestored = uiState.value.totalItemsRestored + 1
                            )
                        }
                    }


                    for(category in categories.map { it.toTransactionCategory() }) {
                        categoryService.insertTransactionCategory(category)
                        _uiState.update {
                            it.copy(
                                totalItemsRestored = uiState.value.totalItemsRestored + 1
                            )
                        }
                    }

                    for(categoryKeyword in categoryKeywords.map { it.toCategoryKeyword() }) {
                        categoryService.insertCategoryKeyword(categoryKeyword)
                        _uiState.update {
                            it.copy(
                                totalItemsRestored = uiState.value.totalItemsRestored + 1
                            )
                        }
                    }

                    for(categoryMapping in categoryMappings.map { it.toTransactionCategoryCrossRef() }) {
                        categoryService.insertTransactionCategoryCrossRef(categoryMapping)
                        _uiState.update {
                            it.copy(
                                totalItemsRestored = uiState.value.totalItemsRestored + 1
                            )
                        }
                    }

                    when(uiState.value.totalItemsRestored == uiState.value.totalItemsToRestore) {
                        true -> {
                            _uiState.update {
                                it.copy(
                                    restoreStatus = RestoreStatus.SUCCESS
                                )
                            }
                        }
                        false -> {

                        }
                    }


                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            restoreStatus = RestoreStatus.FAIL
                        )
                    }
                    Log.e("itemsRestoreException", e.toString())
                }
            }
        }
    }

    fun resetStatus() {
        _uiState.update {
            it.copy(
                backupStatus = BackupStatus.INITIAL,
                restoreStatus = RestoreStatus.INITIAL
            )
        }
    }

    init {
        initializeData()
    }
}