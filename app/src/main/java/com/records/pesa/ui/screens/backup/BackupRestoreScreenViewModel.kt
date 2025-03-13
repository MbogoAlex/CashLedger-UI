package com.records.pesa.ui.screens.backup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencsv.CSVReader
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.CategoryKeyword
import com.records.pesa.db.models.DeletedTransaction
import com.records.pesa.db.models.Transaction
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionCategoryCrossRef
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.db.models.userPreferences
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.supabase.SupabaseCategoryKeyword
import com.records.pesa.models.supabase.SupabaseTransaction
import com.records.pesa.models.supabase.SupabaseTransactionCategory
import com.records.pesa.models.supabase.SupabaseTransactionCategoryCrossRef
import com.records.pesa.network.ApiRepository

import com.records.pesa.service.category.CategoryService
import com.records.pesa.service.transaction.TransactionService
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class BackupRestoreScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val preferences: UserPreferences = userPreferences,
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
    private val apiRepository: ApiRepository,
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
                    val userId = uiState.value.userDetails.userId


                    // Download CSV files from Supabase

                    var existingTransactionsCsv: ByteArray? = null
                    var existingCategoriesCsv: ByteArray? = null
                    var existingCategoryKeywordsCsv: ByteArray? = null
                    var existingCategoryMappingsCsv: ByteArray? = null
                    var existingDeletedTransactionsCsv: ByteArray? = null

                    var transactions = emptyList<Transaction>()
                    var categories = emptyList<TransactionCategory>()
                    var categoryKeywords = emptyList<CategoryKeyword>()
                    var categoryMappings = emptyList<TransactionCategoryCrossRef>()
                    var deletedTransactions = emptyList<DeletedTransaction>()

                    try {
                        existingTransactionsCsv = apiRepository.getFile("${userId}_transactions.csv").body()?.bytes()

                        transactions = parseTransactionsCsv(existingTransactionsCsv!!)
                    } catch (e: Exception) {
                        Log.e("FileNotFound", "Transactions CSV not found: ${e.message}")
                    }

                    try {
                        existingCategoriesCsv = apiRepository.getFile("${userId}_categories.csv").body()?.bytes()
                        categories = parseTransactionCategoriesCsv(existingCategoriesCsv!!)
                        Log.d("found_categories", categories.toString())
                    } catch (e: Exception) {
                        Log.e("FileNotFound", "Categories CSV not found: ${e.message}")
                    }

                    try {
                        existingCategoryKeywordsCsv = apiRepository.getFile("${userId}_categoryKeywords.csv").body()?.bytes()

                        categoryKeywords = parseCategoryKeywordsCsv(existingCategoryKeywordsCsv!!)
                    } catch (e: Exception) {
                        Log.e("FileNotFound", "Category Keywords CSV not found: ${e.message}")
                    }

                    try {
                        existingCategoryMappingsCsv = apiRepository.getFile("${userId}_transactionCategoryCrossRef.csv").body()?.bytes()
                        categoryMappings = parseTransactionCategoryCrossRefCsv(existingCategoryMappingsCsv!!)
                    } catch (e: Exception) {
                        Log.e("FileNotFound", "Category Mappings CSV not found: ${e.message}")
                    }

                    try {
                        existingDeletedTransactionsCsv = apiRepository.getFile("${userId}_deletedTransactions.csv").body()?.bytes()
                        deletedTransactions = parseDeletedTransactionsCsv(existingDeletedTransactionsCsv!!)
                    } catch (e: Exception) {
                        Log.e("FileNotFound", "Deleted Transactions CSV not found: ${e.message}")
                    }

                    // Update the total number of items to restore
                    _uiState.update {
                        it.copy(
                            totalItemsToRestore = transactions.size + categories.size + categoryKeywords.size + categoryMappings.size + deletedTransactions.size
                        )
                    }

                    // Insert Transactions into Room
                    Log.d("Restoring_data_size: ", transactions.size.toString())
                    for (transaction in transactions) {
                        Log.d("Restoring_data: ", "Transaction ID: ${transaction.id}")

                        transactionService.insertTransaction(transaction)
                        _uiState.update {
                            it.copy(
                                totalItemsRestored = uiState.value.totalItemsRestored + 1
                            )
                        }
                    }

                    // Insert Categories into Room
                    for (category in categories) {
                        Log.d("Restoring_data_category: ", category.toString())
                        categoryService.insertTransactionCategory(category)
                        _uiState.update {
                            it.copy(
                                totalItemsRestored = uiState.value.totalItemsRestored + 1
                            )
                        }
                    }

                    // Insert Category Keywords into Room
                    for (categoryKeyword in categoryKeywords) {
                        Log.d("Restoring_data: ", "categoryKeyword")
                        categoryService.insertCategoryKeyword(categoryKeyword)
                        _uiState.update {
                            it.copy(
                                totalItemsRestored = uiState.value.totalItemsRestored + 1
                            )
                        }
                    }

                    // Insert TransactionCategoryCrossRef into Room
                    for (categoryMapping in categoryMappings) {
                        Log.d("Restoring_data: ", "categoryMapping")
                        try {
                            categoryService.insertTransactionCategoryCrossRef(categoryMapping)
                            _uiState.update {
                                it.copy(
                                    totalItemsRestored = uiState.value.totalItemsRestored + 1
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("insertException", "categoryMapping $e")
                        }
                    }

                    for(deletedTransaction in deletedTransactions) {
                        try {
                            transactionService.insertDeletedTransaction(deletedTransaction)
                            _uiState.update {
                                it.copy(
                                    totalItemsRestored = uiState.value.totalItemsRestored + 1
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("insertException", "deletedTransaction $e")
                        }
                    }

                    // Update restore status on completion
                    if (uiState.value.totalItemsRestored == uiState.value.totalItemsToRestore) {

                        dbRepository.updateUserPreferences(
                            userReferences = uiState.value.preferences.copy(
                                restoredData = true,
                                lastRestore = LocalDateTime.now()
                            )
                        )

                        _uiState.update {
                            it.copy(
                                restoreStatus = RestoreStatus.SUCCESS
                            )
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

    private fun parseTransactionsCsv(csvData: ByteArray): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val reader = CSVReader(InputStreamReader(csvData.inputStream()))
        val rows = reader.readAll()

        for (row in rows.drop(1)) { // Skip header row
            val transaction = Transaction(
                id = row[0].toInt(),
                transactionCode = row[1],
                transactionType = row[2],
                transactionAmount = row[3].toDouble(),
                transactionCost = row[4].toDouble(),
                date = LocalDate.parse(row[5]),
                time = LocalTime.parse(row[6]),
                sender = row[7],
                recipient = row[8],
                nickName = row.getOrNull(9),
                comment = row.getOrNull(10),
                balance = row[11].toDouble(),
                entity = row[12],
                userId = row[13].toInt()
            )
            transactions.add(transaction)
        }
        return transactions
    }

    private fun parseTransactionCategoriesCsv(csvData: ByteArray): List<TransactionCategory> {
        val categories = mutableListOf<TransactionCategory>()
        val reader = CSVReader(InputStreamReader(csvData.inputStream()))
        val rows = reader.readAll()

        for (row in rows.drop(1)) { // Skip header row
            val containsValue = row.getOrNull(4) ?: "" // Handle potential null or missing value
            val containsList = if (containsValue.isNotEmpty()) {
                containsValue.split(",")
            } else {
                emptyList()
            }
            Log.d("category_row", row.toString())
            Log.d("category_row 0", row[0].toString())
            Log.d("category_row 1", row[1].toString())
            Log.d("category_row 2", row[2].toString())
            Log.d("category_row 3", row[3].toString())
            Log.d("category_row 4", row[4].toString())
            Log.d("category_row 5", row.getOrNull(5).toString())
            val category = TransactionCategory(
                id = row[0].toInt(),                     // ID
                createdAt = LocalDateTime.parse(row[1]), // createdAt
                updatedAt = LocalDateTime.parse(row[2]), // updatedAt
                name = row[3],                           // name
                contains = containsList,                 // Split by comma or return empty list
                updatedTimes = row.getOrNull(5)?.toDouble() // updatedTimes (optional)
            )
            categories.add(category)
        }
        return categories
    }

    private fun parseDeletedTransactionsCsv(csvData: ByteArray): List<DeletedTransaction> {
        val deletedTransactions = mutableListOf<DeletedTransaction>()
        val reader = CSVReader(InputStreamReader(csvData.inputStream()))
        val rows = reader.readAll()

        for (row in rows.drop(1)) { // Skip header row
            val deletedTransaction = DeletedTransaction(
                id = row[0].toInt(),
                entity = row[1],
            )
            deletedTransactions.add(deletedTransaction)
        }
        return deletedTransactions
    }


    private fun parseCategoryKeywordsCsv(csvData: ByteArray): List<CategoryKeyword> {
        val keywords = mutableListOf<CategoryKeyword>()
        val reader = CSVReader(InputStreamReader(csvData.inputStream()))
        val rows = reader.readAll()

        for (row in rows.drop(1)) { // Skip header row
            val keyword = CategoryKeyword(
                id = row[0].toInt(),
                keyword = row[1],
                nickName = row.getOrNull(2),
                categoryId = row[3].toInt()
            )
            keywords.add(keyword)
        }
        return keywords
    }

    private fun parseTransactionCategoryCrossRefCsv(csvData: ByteArray): List<TransactionCategoryCrossRef> {
        val crossRefs = mutableListOf<TransactionCategoryCrossRef>()
        val reader = CSVReader(InputStreamReader(csvData.inputStream()))
        val rows = reader.readAll()

        for (row in rows.drop(1)) { // Skip header row
            Log.d("category_mapping_data", row.toString())
            Log.d("category_mapping_data 0", row[0].toIntOrNull().toString())
            Log.d("category_mapping_data 1", row[1].toIntOrNull().toString())
            Log.d("category_mapping_data 2", row[2].toIntOrNull().toString())
            val crossRef = TransactionCategoryCrossRef(
                id = row[0].toIntOrNull(), // `id` might be nullable
                transactionId = row[1].toInt(),
                categoryId = row[2].toInt()
            )
            crossRefs.add(crossRef)
        }
        return crossRefs
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
        getUserPreferences()
    }
}