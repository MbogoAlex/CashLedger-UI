package com.records.pesa.ui.screens.backup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.mapper.toTransaction
import com.records.pesa.mapper.toTransactionCategory
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.payment.supabase.SupabaseCategoryKeyword
import com.records.pesa.models.payment.supabase.SupabaseTransaction
import com.records.pesa.models.payment.supabase.SupabaseTransactionCategory
import com.records.pesa.models.payment.supabase.SupabaseTransactionCategoryCrossRef
import com.records.pesa.models.payment.supabase.mapper.toCategoryKeyword
import com.records.pesa.models.payment.supabase.mapper.toSupabaseCategoryKeywords
import com.records.pesa.models.payment.supabase.mapper.toSupabaseTransaction
import com.records.pesa.models.payment.supabase.mapper.toSupabaseTransactionCategory
import com.records.pesa.models.payment.supabase.mapper.toSupabaseTransactionCategoryCrossRefs
import com.records.pesa.models.payment.supabase.mapper.toTransaction
import com.records.pesa.models.payment.supabase.mapper.toTransactionCategory
import com.records.pesa.models.payment.supabase.mapper.toTransactionCategoryCrossRef
import com.records.pesa.models.user.UserAccount
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
import java.time.LocalDate
import java.time.LocalDateTime

data class BackupScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val backupSet: Boolean = false,
    val paymentStatus: Boolean = false,
    val transactionsNotBackedUp: Int = 0,
    val transactions: List<SupabaseTransaction> = emptyList(),
    val categories: List<SupabaseTransactionCategory> = emptyList(),
    val categoryKeywords: List<SupabaseCategoryKeyword> = emptyList(),
    val totalItems: Int = 0,
    val itemsInserted: Int = 0,
    val backupMessage: String = "",
    val transactionWithCategoryMappings: List<SupabaseTransactionCategoryCrossRef> = emptyList(),
    val backupStatus: BackupStatus = BackupStatus.INITIAL,
    val restoreStatus: RestoreStatus = RestoreStatus.INITIAL
)

class BackupScreenViewModel(
    private val dbRepository: DBRepository,
    private val transactionService: TransactionService,
    private val categoryService: CategoryService,
): ViewModel() {
    private val _uiState = MutableStateFlow(BackupScreenUiState())
    val uiState: StateFlow<BackupScreenUiState> = _uiState.asStateFlow()

    fun getUserDetails() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbRepository.getUsers().collect() {userDetails ->
                    _uiState.update {
                        it.copy(
                            userDetails = userDetails.first()
                        )
                    }
                }
            }
        }
    }

    private fun getAllLocalData() {
        viewModelScope.launch {
            while (uiState.value.userDetails.userId == 0) {
                delay(1000)
            }
            getLocalTransactions()
            getLocalCategories()
            getCategoryKeywords()
            getLocalTransactionCategoryMappings()
        }
    }

    private fun getLocalTransactions() {
        val query = transactionService.createUserTransactionQuery(
            userId = uiState.value.userDetails.userId,
            entity = null,
            categoryId = null,
            budgetId = null,
            transactionType = null,
            moneyDirection = null,
            startDate = LocalDate.now().minusYears(10),
            endDate = LocalDate.now(),
            latest = true
        )

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val transactions = transactionService.getUserTransactions(query = query).first()
                _uiState.update {
                    it.copy(
                        transactions = transactions.map { transaction -> transaction.toTransaction(userId = uiState.value.userDetails.userId).toSupabaseTransaction() }
                    )
                }
            }
        }
    }

    private fun getLocalCategories() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val categories = categoryService.getAllCategories().first()
                _uiState.update {
                    it.copy(
                        categories = categories.map { category -> category.toTransactionCategory().toSupabaseTransactionCategory(userId = uiState.value.userDetails.userId) }
                    )
                }
            }
        }
    }

    private fun getCategoryKeywords() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val categoryKeywords = categoryService.getAllCategoryKeywords()
                _uiState.update {
                    it.copy(
                        categoryKeywords = categoryKeywords.toSupabaseCategoryKeywords(userId = uiState.value.userDetails.userId)
                    )
                }
            }
        }
    }

    private fun getLocalTransactionCategoryMappings() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val transactionCategoryMappings = categoryService.getTransactionCategoryCrossRefs().first()
                _uiState.update {
                    it.copy(
                        transactionWithCategoryMappings = transactionCategoryMappings.toSupabaseTransactionCategoryCrossRefs(userId = uiState.value.userDetails.userId)
                    )
                }
            }
        }
    }

    fun backup() {
        _uiState.update {
            it.copy(
                backupStatus = BackupStatus.LOADING
            )
        }
        var totalItems = 0
        var insertedItems = 0
        totalItems += uiState.value.transactions.size
        totalItems += uiState.value.categories.size
        totalItems += uiState.value.categoryKeywords.size
        totalItems += uiState.value.transactionWithCategoryMappings.size
        val batchSize = 20
        val totalTransactionsBatches = (uiState.value.transactions.size + batchSize - 1) / batchSize
        val totalCategoriesBatches = (uiState.value.categories.size + batchSize - 1) / batchSize
        val totalCategoryKeywordsBatches = (uiState.value.categoryKeywords.size + batchSize - 1) / batchSize
        val totalTransactionWithCategoryMappingsBatches = (uiState.value.transactionWithCategoryMappings.size + batchSize - 1) / batchSize
        _uiState.update {
            it.copy(
                totalItems = totalItems,
                itemsInserted = insertedItems
            )
        }
        viewModelScope.launch {
            val user = client.postgrest["userAccount"].select {
                filter {
                    eq("id", uiState.value.userDetails.userId)
                }
            }.decodeSingle<UserAccount>()
            try {
                for(i in 0 until totalTransactionsBatches) {
                    val fromIndex = i * totalTransactionsBatches
                    val toIndex = minOf(fromIndex + totalTransactionsBatches, uiState.value.transactions.size)
                    val batch = uiState.value.transactions.subList(fromIndex, toIndex)
                    // backup transactions
                    client.postgrest["transaction"].upsert(batch, onConflict = "transactionCode")
                    _uiState.update {
                        it.copy(
                            itemsInserted = it.itemsInserted + batch.size
                        )
                    }
                }

                for(i in 0 until totalCategoriesBatches) {
                    val fromIndex = i * totalCategoriesBatches
                    val toIndex = minOf(fromIndex + totalCategoriesBatches, uiState.value.categories.size)
                    val batch = uiState.value.categories.subList(fromIndex, toIndex)
                    // backup categories
                    client.postgrest["transactionCategory"].upsert(batch, onConflict = "id")
                    _uiState.update {
                        it.copy(
                            itemsInserted = it.itemsInserted + batch.size
                        )
                    }
                }

                for(i in 0 until totalCategoryKeywordsBatches) {
                    val fromIndex = i * totalCategoryKeywordsBatches
                    val toIndex = minOf(fromIndex + totalCategoryKeywordsBatches, uiState.value.categoryKeywords.size)
                    val batch = uiState.value.categoryKeywords.subList(fromIndex, toIndex)
                    // backup category keywords
                    client.postgrest["categoryKeyword"].upsert(batch, onConflict = "id")
                    _uiState.update {
                        it.copy(
                            itemsInserted = it.itemsInserted + batch.size
                        )
                    }
                }

                for(i in 0 until totalTransactionWithCategoryMappingsBatches) {
                    val fromIndex = i * totalTransactionWithCategoryMappingsBatches
                    val toIndex = minOf(fromIndex + totalTransactionWithCategoryMappingsBatches, uiState.value.transactionWithCategoryMappings.size)
                    val batch = uiState.value.transactionWithCategoryMappings.subList(fromIndex, toIndex)
                    // backup category keywords
                    // backup transactionCategoryCrossRef
                    client.postgrest["transactionCategoryCrossRef"].upsert(batch, onConflict = "id")
                    _uiState.update {
                        it.copy(
                            itemsInserted = it.itemsInserted + batch.size
                        )
                    }
                }

                val lastBackup = LocalDateTime.now()

                client.postgrest["userAccount"].update(user.copy(lastBackup = lastBackup.toString())) {
                    filter {
                        eq("userId", uiState.value.userDetails.userId)
                    }
                }

                dbRepository.updateUser(
                    user = uiState.value.userDetails.copy(
                        lastBackup = lastBackup
                    )
                )

                _uiState.update {
                    it.copy(
                        backupStatus = BackupStatus.SUCCESS
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        backupStatus = BackupStatus.FAIL
                    )
                }
            }

        }
    }

    fun restore() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                   // restore transactions
                    val transactions = client.postgrest["transaction"].select {
                        filter {
                            eq("userId", uiState.value.userDetails.userId)
                        }
                    }.decodeList<SupabaseTransaction>()
                    for(transaction in transactions.map { it.toTransaction() }) {
                        transactionService.insertTransaction(transaction)
                    }

                    // restore categories
                    val categories = client.postgrest["transactionCategory"].select{
                        filter {
                            eq("userId", uiState.value.userDetails.userId)
                        }
                    }.decodeList<SupabaseTransactionCategory>()
                    for(category in categories.map { it.toTransactionCategory() }) {
                        categoryService.insertTransactionCategory(category)
                    }

                    // restore category keywords
                    val categoryKeywords = client.postgrest["categoryKeyword"].select {
                        filter {
                            eq("userId", uiState.value.userDetails.userId)
                        }
                    }.decodeList<SupabaseCategoryKeyword>()

                    for(categoryKeyword in categoryKeywords.map { it.toCategoryKeyword() }) {
                        categoryService.insertCategoryKeyword(categoryKeyword)
                    }

                    // restore transactionCategoryCrossRef
                    val categoryMappings = client.postgrest["transactionCategoryCrossRef"].select {
                        filter {
                            eq("userId", uiState.value.userDetails.userId)
                        }

                    }.decodeList<SupabaseTransactionCategoryCrossRef>()

                    for(categoryMapping in categoryMappings.map { it.toTransactionCategoryCrossRef() }) {
                        categoryService.insertTransactionCategoryCrossRef(categoryMapping)
                    }

                } catch (e: Exception) {

                }
            }
        }
    }

    init {
        getUserDetails()
        getAllLocalData()
    }
}