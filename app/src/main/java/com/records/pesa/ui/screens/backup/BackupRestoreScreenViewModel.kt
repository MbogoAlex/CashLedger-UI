package com.records.pesa.ui.screens.backup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencsv.CSVReader
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.Budget
import com.records.pesa.db.models.BudgetCycleLog
import com.records.pesa.db.models.BudgetMember
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
import com.records.pesa.service.auth.AuthenticationManager
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
    val preferences: UserPreferences? = null,
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
    private val authenticationManager: AuthenticationManager,
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
        Log.d("filesRestore", "Starting restore process")
        _uiState.update {
            it.copy(
                restoreStatus = RestoreStatus.LOADING
            )
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val userId = uiState.value.userDetails.dynamoUserId ?: uiState.value.userDetails.phoneNumber.replaceFirstChar { "" }


                    // Download CSV files from Supabase

                    var existingTransactionsCsv: ByteArray? = null
                    var existingCategoriesCsv: ByteArray? = null
                    var existingCategoryKeywordsCsv: ByteArray? = null
                    var existingCategoryMappingsCsv: ByteArray? = null
                    var existingDeletedTransactionsCsv: ByteArray? = null
                    var existingBudgetsCsv: ByteArray? = null

                    var transactions = emptyList<Transaction>()
                    var categories = emptyList<TransactionCategory>()
                    var categoryKeywords = emptyList<CategoryKeyword>()
                    var categoryMappings = emptyList<TransactionCategoryCrossRef>()
                    var deletedTransactions = emptyList<DeletedTransaction>()
                    var budgets = emptyList<Budget>()

                    try {
                        Log.d("filesRestore", "Downloading transactions CSV for userId: $userId")
                        val response = authenticationManager.executeWithAuth { token ->
                            apiRepository.getFile(token, "${userId}_transactions.csv")
                        }
                        existingTransactionsCsv = response?.body()?.bytes()
                        Log.d("filesRestore", "Downloaded transactions CSV, size: ${existingTransactionsCsv?.size ?: 0} bytes")
                        if (existingTransactionsCsv != null) {
                            Log.d("filesRestore", "Starting to parse transactions CSV...")
                            transactions = parseTransactionsCsv(existingTransactionsCsv!!)
                            Log.d("filesRestore", "Successfully parsed ${transactions.size} transactions")
                        } else {
                            Log.e("filesRestore", "Transactions CSV is null after download")
                        }
                    } catch (e: Exception) {
                        Log.e("filesRestore", "Transactions CSV error: ${e.message}")
                        Log.e("filesRestore", "Stack trace: ${e.stackTraceToString()}")
                    }

                    try {
                        val response = authenticationManager.executeWithAuth { token ->
                            apiRepository.getFile(token, "${userId}_categories.csv")
                        }
                        existingCategoriesCsv = response?.body()?.bytes()
                        if (existingCategoriesCsv != null) {
                            categories = parseTransactionCategoriesCsv(existingCategoriesCsv!!)
                            Log.d("filesRestore", categories.toString())
                        }
                    } catch (e: Exception) {
                        Log.e("filesRestore", "Categories CSV not found: ${e.message}")
                    }

                    try {
                        val response = authenticationManager.executeWithAuth { token ->
                            apiRepository.getFile(token, "${userId}_categoryKeywords.csv")
                        }
                        existingCategoryKeywordsCsv = response?.body()?.bytes()
                        if (existingCategoryKeywordsCsv != null) {
                            categoryKeywords = parseCategoryKeywordsCsv(existingCategoryKeywordsCsv!!)
                        }
                    } catch (e: Exception) {
                        Log.e("filesRestore", "Category Keywords CSV not found: ${e.message}")
                    }

                    try {
                        val response = authenticationManager.executeWithAuth { token ->
                            apiRepository.getFile(token, "${userId}_transactionCategoryCrossRef.csv")
                        }
                        existingCategoryMappingsCsv = response?.body()?.bytes()
                        if (existingCategoryMappingsCsv != null) {
                            categoryMappings = parseTransactionCategoryCrossRefCsv(existingCategoryMappingsCsv!!)
                        }
                    } catch (e: Exception) {
                        Log.e("filesRestore", "Category Mappings CSV not found: ${e.message}")
                    }

                    try {
                        val response = authenticationManager.executeWithAuth { token ->
                            apiRepository.getFile(token, "${userId}_deletedTransactions.csv")
                        }
                        existingDeletedTransactionsCsv = response?.body()?.bytes()
                        if (existingDeletedTransactionsCsv != null) {
                            deletedTransactions = parseDeletedTransactionsCsv(existingDeletedTransactionsCsv!!)
                        }
                    } catch (e: Exception) {
                        Log.e("filesRestore", "Deleted Transactions CSV not found: ${e.message}")
                    }

                    try {
                        val response = authenticationManager.executeWithAuth { token ->
                            apiRepository.getFile(token, "${userId}_budgets.csv")
                        }
                        existingBudgetsCsv = response?.body()?.bytes()
                        if (existingBudgetsCsv != null) {
                            budgets = parseBudgetsCsv(existingBudgetsCsv!!)
                            Log.d("filesRestore", "Parsed ${budgets.size} budgets")
                        }
                    } catch (e: Exception) {
                        Log.e("filesRestore", "Budgets CSV not found: ${e.message}")
                    }

                    // Update the total number of items to restore
                    _uiState.update {
                        it.copy(
                            totalItemsToRestore = transactions.size + categories.size + categoryKeywords.size + categoryMappings.size + deletedTransactions.size + budgets.size
                        )
                    }

                    // Insert Transactions into Room
                    Log.d("filesRestore_insert", "Inserting ${transactions.size} transactions into database")
                    var insertedCount = 0
                    var insertFailedCount = 0
                    
                    for ((index, transaction) in transactions.withIndex()) {
                        try {
                            if (index < 5 || index % 100 == 0) {
                                Log.d("filesRestore_insert", "Inserting transaction $index: ID=${transaction.id}, code=${transaction.transactionCode}, userId=${transaction.userId}")
                            }
                            
                            transactionService.insertTransaction(transaction)
                            insertedCount++
                            
                            _uiState.update {
                                it.copy(
                                    totalItemsRestored = uiState.value.totalItemsRestored + 1
                                )
                            }
                        } catch (e: Exception) {
                            insertFailedCount++
                            Log.e("filesRestore_insert", "Failed to insert transaction $index (ID=${transaction.id}): ${e.message}")
                            if (insertFailedCount <= 5) {
                                Log.e("filesRestore_insert", "Stack trace: ${e.stackTraceToString()}")
                            }
                        }
                    }
                    
                    Log.d("filesRestore_insert", "Transaction insertion complete: $insertedCount inserted, $insertFailedCount failed")

                    // Insert Categories into Room
                    for (category in categories) {
                        Log.d("filesRestore, Restoring_data_category: ", category.toString())
                        categoryService.insertTransactionCategory(category)
                        _uiState.update {
                            it.copy(
                                totalItemsRestored = uiState.value.totalItemsRestored + 1
                            )
                        }
                    }

                    // Insert Category Keywords into Room
                    for (categoryKeyword in categoryKeywords) {
                        Log.d("filesRestore: ", "categoryKeyword")
                        categoryService.insertCategoryKeyword(categoryKeyword)
                        _uiState.update {
                            it.copy(
                                totalItemsRestored = uiState.value.totalItemsRestored + 1
                            )
                        }
                    }

                    // Insert TransactionCategoryCrossRef into Room
                    Log.d("filesRestore_mapping", "Inserting ${categoryMappings.size} category mappings")
                    var mappingInsertedCount = 0
                    var mappingFailedCount = 0
                    
                    for ((index, categoryMapping) in categoryMappings.withIndex()) {
                        try {
                            if (index < 5) {
                                Log.d("filesRestore_mapping", "Mapping $index: transactionId=${categoryMapping.transactionId}, categoryId=${categoryMapping.categoryId}")
                            }
                            
                            categoryService.insertTransactionCategoryCrossRef(categoryMapping)
                            mappingInsertedCount++
                            
                            _uiState.update {
                                it.copy(
                                    totalItemsRestored = uiState.value.totalItemsRestored + 1
                                )
                            }
                        } catch (e: Exception) {
                            mappingFailedCount++
                            if (mappingFailedCount <= 5) {
                                Log.e("filesRestore_mapping", "Failed to insert mapping $index (txId=${categoryMapping.transactionId}, catId=${categoryMapping.categoryId}): ${e.message}")
                            }
                        }
                    }
                    
                    Log.d("filesRestore_mapping", "Mapping insertion complete: $mappingInsertedCount inserted, $mappingFailedCount failed")

                    for(deletedTransaction in deletedTransactions) {
                        try {
                            transactionService.insertDeletedTransaction(deletedTransaction)
                            _uiState.update {
                                it.copy(
                                    totalItemsRestored = uiState.value.totalItemsRestored + 1
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("filesRestore, insertException", "deletedTransaction $e")
                        }
                    }

                    // Insert Budgets into Room
                    for (budget in budgets) {
                        try {
                            dbRepository.insertBudget(budget)
                            _uiState.update {
                                it.copy(totalItemsRestored = uiState.value.totalItemsRestored + 1)
                            }
                        } catch (e: Exception) {
                            Log.e("filesRestore", "Failed to insert budget ${budget.id}: ${e.message}")
                        }
                    }

                    // Fetch and restore budget members
                    try {
                        val budgetMembersResponse = authenticationManager.executeWithAuth { token ->
                            apiRepository.getFile(token, "${userId}_budgetMembers.csv")
                        }
                        val budgetMembersCsv = budgetMembersResponse?.body()?.bytes()
                        if (budgetMembersCsv != null) {
                            val budgetMembers = parseBudgetMembersCsv(budgetMembersCsv)
                            for (member in budgetMembers) {
                                try {
                                    dbRepository.insertBudgetMembers(listOf(member))
                                } catch (e: Exception) {
                                    Log.e("filesRestore", "Failed to insert budget member: ${e.message}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("filesRestore", "Budget members restore failed: ${e.message}")
                    }

                    // Fetch and restore budget cycle logs (recurring budget history)
                    try {
                        val cycleLogsResponse = authenticationManager.executeWithAuth { token ->
                            apiRepository.getFile(token, "${userId}_budgetCycleLogs.csv")
                        }
                        val cycleLogsCsv = cycleLogsResponse?.body()?.bytes()
                        if (cycleLogsCsv != null) {
                            val cycleLogs = parseBudgetCycleLogsCsv(cycleLogsCsv)
                            for (log in cycleLogs) {
                                try {
                                    dbRepository.insertBudgetCycleLog(log)
                                } catch (e: Exception) {
                                    Log.e("filesRestore", "Failed to insert cycle log: ${e.message}")
                                }
                            }
                            Log.d("filesRestore", "Restored ${cycleLogs.size} budget cycle logs")
                        }
                    } catch (e: Exception) {
                        // Cycle logs are optional — older backups won't have this file
                        Log.d("filesRestore", "No budget cycle logs to restore (may be older backup): ${e.message}")
                    }

                    // Update restore status on completion
                    if (uiState.value.totalItemsRestored == uiState.value.totalItemsToRestore) {

                        while(uiState.value.preferences == null) {
                            delay(1000)
                        }
                        dbRepository.updateUserPreferences(
                            userReferences = uiState.value.preferences!!.copy(
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
                    Log.e("filesRestore, itemsRestoreException", e.toString())
                }
            }
        }
    }

    private fun parseTransactionsCsv(csvData: ByteArray): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val reader = CSVReader(InputStreamReader(csvData.inputStream()))
        val rows = reader.readAll()
        
        Log.d("filesRestore_parse", "Total rows in CSV: ${rows.size}")
        Log.d("filesRestore_parse", "Header: ${rows.firstOrNull()?.joinToString(",")}")
        
        var successCount = 0
        var failCount = 0

        for ((index, row) in rows.drop(1).withIndex()) { // Skip header row
            try {
                if (index < 3) {
                    Log.d("filesRestore_parse", "Row $index: date='${row[5]}', time='${row[6]}'")
                }
                
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
                    userId = row[13].toLong()
                )
                transactions.add(transaction)
                successCount++
                
                if (index < 3) {
                    Log.d("filesRestore_parse", "Successfully parsed transaction $index: ${transaction.transactionCode}")
                }
            } catch (e: Exception) {
                failCount++
                Log.e("filesRestore_parse", "Failed to parse row $index: ${e.message}")
                Log.e("filesRestore_parse", "Row data: ${row.joinToString(",")}")
                if (failCount <= 5) {
                    Log.e("filesRestore_parse", "Stack trace: ${e.stackTraceToString()}")
                }
            }
        }
        
        Log.d("filesRestore_parse", "Parsing complete: $successCount success, $failCount failed")
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

    private fun parseBudgetsCsv(csvData: ByteArray): List<Budget> {
        val budgets = mutableListOf<Budget>()
        val reader = CSVReader(InputStreamReader(csvData.inputStream()))
        val rows = reader.readAll()

        for (row in rows.drop(1)) { // Skip header row
            try {
                // Support both old format (no startDate, col 5=limitDate) and new format (col 6=startDate, col 7=limitDate)
                val hasStartDate = row.size >= 12
                val budget = Budget(
                    id = row[0].toInt(),
                    name = row[1],
                    active = row[2].toBoolean(),
                    expenditure = row[3].toDouble(),
                    budgetLimit = row[4].toDouble(),
                    createdAt = LocalDateTime.parse(row[5]),
                    startDate = if (hasStartDate) LocalDate.parse(row[6])
                                else LocalDateTime.parse(row[5]).toLocalDate().withDayOfMonth(1),
                    limitDate = LocalDate.parse(if (hasStartDate) row[7] else row[6]),
                    limitReached = row[if (hasStartDate) 8 else 7].toBoolean(),
                    limitReachedAt = row.getOrNull(if (hasStartDate) 9 else 8)?.takeIf { it.isNotBlank() }?.let { LocalDateTime.parse(it) },
                    exceededBy = row[if (hasStartDate) 10 else 9].toDouble(),
                    categoryId = row[if (hasStartDate) 11 else 10].toIntOrNull(),
                    alertThreshold = row.getOrNull(if (hasStartDate) 12 else 11)?.toIntOrNull() ?: 80,
                    // Recurrence fields (cols 13-16, absent in older backups — default to non-recurring)
                    isRecurring = row.getOrNull(13)?.toBoolean() ?: false,
                    recurrenceType = row.getOrNull(14)?.takeIf { it.isNotBlank() },
                    recurrenceIntervalDays = row.getOrNull(15)?.toIntOrNull(),
                    cycleNumber = row.getOrNull(16)?.toIntOrNull() ?: 1,
                )
                budgets.add(budget)
            } catch (e: Exception) {
                Log.e("filesRestore", "Failed to parse budget row: ${e.message}")
            }
        }
        return budgets
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

    private fun parseBudgetMembersCsv(csvData: ByteArray): List<BudgetMember> {
        val members = mutableListOf<BudgetMember>()
        try {
            val reader = CSVReader(InputStreamReader(csvData.inputStream()))
            val rows = reader.readAll()
            if (rows.size <= 1) return members
            for (i in 1 until rows.size) {
                try {
                    val row = rows[i]
                    members.add(BudgetMember(
                        id = row[0].toInt(),
                        budgetId = row[1].toInt(),
                        memberName = row[2]
                    ))
                } catch (e: Exception) {
                    Log.e("filesRestore", "Failed to parse budget member row: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("filesRestore", "Failed to parse budget members CSV: ${e.message}")
        }
        return members
    }

    private fun parseBudgetCycleLogsCsv(csvData: ByteArray): List<BudgetCycleLog> {
        val logs = mutableListOf<BudgetCycleLog>()
        try {
            val reader = CSVReader(InputStreamReader(csvData.inputStream()))
            val rows = reader.readAll()
            if (rows.size <= 1) return logs
            for (i in 1 until rows.size) {
                try {
                    val row = rows[i]
                    logs.add(BudgetCycleLog(
                        id = row[0].toInt(),
                        budgetId = row[1].toInt(),
                        budgetName = row[2],
                        cycleNumber = row[3].toInt(),
                        cycleStartDate = LocalDate.parse(row[4]),
                        cycleEndDate = LocalDate.parse(row[5]),
                        budgetLimit = row[6].toDouble(),
                        finalExpenditure = row[7].toDouble(),
                        limitReached = row[8].toBoolean(),
                        exceededBy = row[9].toDouble(),
                        closedAt = LocalDateTime.parse(row[10])
                    ))
                } catch (e: Exception) {
                    Log.e("filesRestore", "Failed to parse cycle log row: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("filesRestore", "Failed to parse budget cycle logs CSV: ${e.message}")
        }
        return logs
    }

    private fun getUserPreferences() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbRepository.getUserPreferences()?.collect() { preferences ->
                    _uiState.update {
                        it.copy(
                            preferences = preferences
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