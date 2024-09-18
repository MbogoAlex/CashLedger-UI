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
                            userDetails = userDetails.first(),
                        )
                    }
                }
            }
        }
    }

    fun getAllLocalData() {
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

    fun getItemsNotBackedUp() {
        var totalItems = 0
        totalItems += uiState.value.transactions.size
        totalItems += uiState.value.categories.size
        totalItems += uiState.value.categoryKeywords.size
        totalItems += uiState.value.transactionWithCategoryMappings.size

        Log.d("TOTAL_ITEMS", totalItems.toString())
        Log.d("TOTAL_ITEMS_NOT_BACKED_UP", "${totalItems - uiState.value.userDetails.backedUpItemsSize}")
        val itemsNotBackedUp = totalItems - uiState.value.userDetails.backedUpItemsSize
        _uiState.update {
            it.copy(
                itemsNotBackedUp = if(itemsNotBackedUp < 0) 0 else itemsNotBackedUp,
                transactionsNotBackedUp = uiState.value.transactions.size - uiState.value.userDetails.transactions,
                categoriesNotBackedUp = uiState.value.categories.size - uiState.value.userDetails.categories,
                categoryKeywordsNotBackedUp = uiState.value.categoryKeywords.size - uiState.value.userDetails.categoryKeywords,
                categoryMappingsNotBackedUp = uiState.value.transactionWithCategoryMappings.size - uiState.value.userDetails.categoryMappings
            )
        }
    }

    fun backup() {
        Log.d("backingUpInProcess", "LOADING...")
        _uiState.update {
            it.copy(
                backupStatus = BackupStatus.LOADING
            )
        }

        val transactionsToBackup = uiState.value.transactions.subList(uiState.value.userDetails.transactions, uiState.value.transactions.size)
        val categoriesToBackup = uiState.value.categories.subList(uiState.value.userDetails.categories, uiState.value.categories.size)
        val categoryKeywordsToBackup = uiState.value.categoryKeywords.subList(uiState.value.userDetails.categoryKeywords, uiState.value.categoryKeywords.size)
        val categoryMappingsToBackup = uiState.value.transactionWithCategoryMappings.subList(uiState.value.userDetails.categoryMappings, uiState.value.transactionWithCategoryMappings.size)

        var totalItems = 0
        var insertedItems = 0

        val batchSize = 2000
        val totalTransactionsBatches = (transactionsToBackup.size + batchSize - 1) / batchSize
        val totalCategoriesBatches = (categoriesToBackup.size + batchSize - 1) / batchSize
        val totalCategoryKeywordsBatches = (categoryKeywordsToBackup.size + batchSize - 1) / batchSize
        val totalTransactionWithCategoryMappingsBatches = (categoryMappingsToBackup.size + batchSize - 1) / batchSize

        totalItems += transactionsToBackup.size
        totalItems += categoriesToBackup.size
        totalItems += categoryKeywordsToBackup.size
        totalItems += categoryMappingsToBackup.size

        _uiState.update {
            it.copy(
                totalItems = totalItems,
                itemsInserted = insertedItems
            )
        }
        viewModelScope.launch {
            try {
                val user = client.postgrest["userAccount"].select {
                    filter {
                        eq("id", uiState.value.userDetails.userId)
                    }
                }.decodeSingle<UserAccount>()

                client.postgrest["userAccount"].update(user.copy(
                    backupSet = true
                )) {
                    filter {
                        eq("id", uiState.value.userDetails.userId)
                    }
                }

                dbRepository.updateUser(
                    uiState.value.userDetails.copy(backupSet = true)
                )

                for(i in 0 until totalTransactionsBatches) {
                    val fromIndex = i * batchSize
                    val toIndex = minOf(fromIndex + batchSize, transactionsToBackup.size)
                    val batch = transactionsToBackup.subList(fromIndex, toIndex)
                    // backup transactions
                    client.postgrest["transaction"].upsert(batch, onConflict = "transactionCode")
                    _uiState.update {
                        it.copy(
                            itemsInserted = it.itemsInserted + batch.size
                        )
                    }
                }

                for(i in 0 until totalCategoriesBatches) {
                    val fromIndex = i * batchSize
                    val toIndex = minOf(fromIndex + batchSize, categoriesToBackup.size)
                    val batch = categoriesToBackup.subList(fromIndex, toIndex)
                    // backup categories
                    client.postgrest["transactionCategory"].upsert(batch, onConflict = "id")
                    _uiState.update {
                        it.copy(
                            itemsInserted = it.itemsInserted + batch.size
                        )
                    }
                }

                for(i in 0 until totalCategoryKeywordsBatches) {
                    val fromIndex = i * batchSize
                    val toIndex = minOf(fromIndex + batchSize, categoryKeywordsToBackup.size)
                    val batch = categoryKeywordsToBackup.subList(fromIndex, toIndex)
                    // backup category keywords
                    client.postgrest["categoryKeyword"].upsert(batch, onConflict = "id")
                    _uiState.update {
                        it.copy(
                            itemsInserted = it.itemsInserted + batch.size
                        )
                    }
                }

                for(i in 0 until totalTransactionWithCategoryMappingsBatches) {
                    val fromIndex = i * batchSize
                    val toIndex = minOf(fromIndex + batchSize, categoryMappingsToBackup.size)
                    val batch = categoryMappingsToBackup.subList(fromIndex, toIndex)
                    // backup category keywords
                    // backup transactionCategoryCrossRef
                    client.postgrest["transactionCategoryCrossRef"].upsert(
                        batch,
                        onConflict = "transactionId, categoryId"
                    )


                    _uiState.update {
                        it.copy(
                            itemsInserted = it.itemsInserted + batch.size
                        )
                    }
                }

                val lastBackup = LocalDateTime.now()

                client.postgrest["userAccount"].update(user.copy(
                    lastBackup = lastBackup.toString(),
                    backedUpItemsSize = user.backedUpItemsSize + totalItems,
                    transactions = user.transactions + uiState.value.transactionsNotBackedUp,
                    categories = user.categories + uiState.value.categoriesNotBackedUp,
                    categoryKeywords = user.categoryKeywords + uiState.value.categoryKeywordsNotBackedUp,
                    categoryMappings = user.categoryMappings + uiState.value.categoryMappingsNotBackedUp
                )) {
                    filter {
                        eq("id", uiState.value.userDetails.userId)
                    }
                }

                dbRepository.updateUser(
                    user = uiState.value.userDetails.copy(
                        lastBackup = lastBackup,
                        backedUpItemsSize = user.backedUpItemsSize + totalItems,
                        transactions = user.transactions + uiState.value.transactionsNotBackedUp,
                        categories = user.categories + uiState.value.categoriesNotBackedUp,
                        categoryKeywords = user.categoryKeywords + uiState.value.categoryKeywordsNotBackedUp,
                        categoryMappings = user.categoryMappings + uiState.value.categoryMappingsNotBackedUp
                    )
                )

                Log.d("backUpSuccess", "SUCCESS")

                getItemsNotBackedUp()

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
                Log.e("backUpException", e.toString())
            }

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
        getUserDetails()
//        getAllLocalData()
    }
}