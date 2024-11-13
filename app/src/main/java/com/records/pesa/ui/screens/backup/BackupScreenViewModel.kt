package com.records.pesa.ui.screens.backup

import android.content.Context
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
import com.records.pesa.models.payment.supabase.payload.SupabaseCategoryKeywordPayload
import com.records.pesa.models.payment.supabase.payload.SupabaseTransactionCategoryCrossRefPayload
import com.records.pesa.models.payment.supabase.payload.SupabaseTransactionCategoryPayload
import com.records.pesa.models.payment.supabase.payload.SupabaseTransactionPayload
import com.records.pesa.models.payment.supabase.payload.payloadMapper.toSupabaseCategoryKeywordsPayload
import com.records.pesa.models.payment.supabase.payload.payloadMapper.toSupabaseTransactionCategoryCrossRefsPayload
import com.records.pesa.models.payment.supabase.payload.payloadMapper.toSupabaseTransactionCategoryPayload
import com.records.pesa.models.payment.supabase.payload.payloadMapper.toSupabaseTransactionPayload
import com.records.pesa.models.user.UserAccount
import com.records.pesa.network.SupabaseClient.client
import com.records.pesa.service.category.CategoryService
import com.records.pesa.service.transaction.TransactionService
import com.records.pesa.workers.WorkersRepository
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.ceil

data class BackupScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val backupSet: Boolean = false,
    val paymentStatus: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val categories: List<TransactionCategory> = emptyList(),
    val categoryKeywords: List<CategoryKeyword> = emptyList(),
    val deletedTransactions: List<DeletedTransaction> = emptyList(),
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
    val transactionWithCategoryMappings: List<TransactionCategoryCrossRef> = emptyList(),
    val backupStatus: BackupStatus = BackupStatus.INITIAL,
    val restoreStatus: RestoreStatus = RestoreStatus.INITIAL
)

class BackupScreenViewModel(
    private val dbRepository: DBRepository,
    private val transactionService: TransactionService,
    private val categoryService: CategoryService,
    private val workersRepository: WorkersRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(BackupScreenUiState())
    val uiState: StateFlow<BackupScreenUiState> = _uiState.asStateFlow()

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

    fun getAllLocalData() {
        viewModelScope.launch {
            while (uiState.value.userDetails.userId == 0) {
                delay(1000)
            }
            getLocalTransactions()
            getLocalCategories()
            getCategoryKeywords()
            getLocalTransactionCategoryMappings()
            getDeletedTransactionItems()
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
                        transactions = transactions.map { transaction -> transaction.toTransaction(userId = uiState.value.userDetails.userId) }
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
                        categories = categories.map { category -> category.toTransactionCategory() }
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
                        categoryKeywords = categoryKeywords
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
                        transactionWithCategoryMappings = transactionCategoryMappings
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
        totalItems += uiState.value.deletedTransactions.size

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

    fun backup(context: Context) {
        Log.d("backingUpInProcess", "LOADING...")
        _uiState.update {
            it.copy(
                backupStatus = BackupStatus.LOADING
            )
        }

        Log.d("addingBatchTransactions", "FROM ${uiState.value.userDetails.transactions}")
        Log.d("addingBatchTransactions", "TO ${uiState.value.transactions.size}")

        val transactionsToBackup = uiState.value.transactions
        val categoriesToBackup = uiState.value.categories
        val categoryKeywordsToBackup = uiState.value.categoryKeywords
        val categoryMappingsToBackup = uiState.value.transactionWithCategoryMappings
        val deletedTransactions = uiState.value.deletedTransactions

        Log.d("categoriesToBackup", categoriesToBackup.toString())

        var totalItems = 0
        var insertedItems = 0

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
                    filter { eq("id", uiState.value.userDetails.userId) }
                }.decodeSingle<UserAccount>()

                val userId = uiState.value.userDetails.userId

                val bucketName = "cashLedgerBackup"




                val bucket = client.storage[bucketName]


                // Backup data to CSV with proper CSV formatting
                val transactionsCsv = backupTransactionsToCSV(context, "${userId}_transactions.csv", transactionsToBackup)
                val categoriesCsv = backupCategoriesToCSV(context, "${userId}_categories.csv", categoriesToBackup)
                val categoryKeywordsCsv = backupCategoryKeywordsToCSV(context,"${userId}_categoryKeywords.csv", categoryKeywordsToBackup)
                val categoryMappingsCsv = backupCategoryMappingsToCSV(context, "${userId}_transactionCategoryCrossRef.csv", categoryMappingsToBackup)
                val deletedTransactionsCsv = backupDeletedTransactionsToCSV(context, "${userId}_deletedTransactions.csv", deletedTransactions)


                if(transactionsCsv != null) {
                    bucket.upload("${userId}_transactions.csv", transactionsCsv, true)
                    _uiState.update {
                        it.copy(
                            itemsInserted = uiState.value.itemsInserted + transactionsToBackup.size
                        )
                    }
                }

                if(categoriesCsv != null) {
                    bucket.upload("${userId}_categories.csv", categoriesCsv, true)
                    _uiState.update {
                        it.copy(
                            itemsInserted = uiState.value.itemsInserted + categoriesToBackup.size
                        )
                    }
                }

                if(categoryKeywordsCsv != null) {
                    bucket.upload("${userId}_categoryKeywords.csv", categoryKeywordsCsv, true)
                    _uiState.update {
                        it.copy(
                            itemsInserted = uiState.value.itemsInserted + categoryKeywordsToBackup.size
                        )
                    }
                }

                if(categoryMappingsCsv != null) {
                    bucket.upload("${userId}_transactionCategoryCrossRef.csv", categoryMappingsCsv, true)
                    _uiState.update {
                        it.copy(
                            itemsInserted = uiState.value.itemsInserted + categoryMappingsToBackup.size
                        )
                    }
                }

                if(deletedTransactionsCsv != null) {
                    bucket.upload("${userId}_deletedTransactions.csv", deletedTransactionsCsv, true)
                    _uiState.update {
                        it.copy(
                            itemsInserted = uiState.value.itemsInserted + deletedTransactions.size
                        )
                    }
                }

                val lastBackup = LocalDateTime.now()

                // Update user account with backup details
                client.postgrest["userAccount"].update(user.copy(
                    lastBackup = lastBackup.toString(),
                    backedUpItemsSize = totalItems,
                    backupSet = true,
                    transactions = transactionsToBackup.size,
                    categories = categoriesToBackup.size,
                    categoryKeywords = categoryKeywordsToBackup.size,
                    categoryMappings = categoryMappingsToBackup.size
                )) {
                    filter { eq("id", userId) }
                }

                if(!uiState.value.userDetails.backupSet) {
                    workersRepository.fetchAndBackupTransactions(
                        token = "dala",
                        userId = uiState.value.userDetails.userId,
                        paymentStatus = true,
                        priorityHigh = true
                    )
                }

                dbRepository.updateUser(
                    user = uiState.value.userDetails.copy(
                        lastBackup = lastBackup,
                        backedUpItemsSize = totalItems,
                        backupSet = true,
                        backupWorkerInitiated = true,
                        transactions = transactionsToBackup.size,
                        categories = categoriesToBackup.size,
                        categoryKeywords = categoryKeywordsToBackup.size,
                        categoryMappings = categoryMappingsToBackup.size
                    )
                )

                Log.d("backUpSuccess", "SUCCESS")

                getItemsNotBackedUp()

                _uiState.update {
                    it.copy(
                        itemsNotBackedUp = 0,
                        backupStatus = BackupStatus.SUCCESS,
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

    fun getDeletedTransactionItems() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val deletedTransactions = transactionService.getDeletedTransactionEntities()
                _uiState.update {
                    it.copy(
                        deletedTransactions = deletedTransactions
                    )
                }
            }
        }
    }

    fun parseTransactionsCsv(csvData: ByteArray): List<Transaction> {
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

    fun parseTransactionCategoriesCsv(csvData: ByteArray): List<TransactionCategory> {
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


    fun parseCategoryKeywordsCsv(csvData: ByteArray): List<CategoryKeyword> {
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

    fun parseTransactionCategoryCrossRefCsv(csvData: ByteArray): List<TransactionCategoryCrossRef> {
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

    // Helper function to get a writable internal storage file
    fun getInternalStorageFile(context: Context, fileName: String): File {
        return File(context.filesDir, fileName)
    }

    // Helper function to backup transactions data to CSV
    fun backupTransactionsToCSV(context: Context, fileName: String, transactionsToBackup: List<Transaction>): File? {
        try {
            val file = getInternalStorageFile(context, fileName)
            FileWriter(file).use { writer ->
                val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                    "id", "transactionCode", "transactionType", "transactionAmount",
                    "transactionCost", "date", "time", "sender", "recipient",
                    "nickName", "comment", "balance", "entity", "userId"
                ))

                transactionsToBackup.forEach { transaction ->
                    csvPrinter.printRecord(
                        transaction.id, transaction.transactionCode, transaction.transactionType,
                        transaction.transactionAmount, transaction.transactionCost, transaction.date,
                        transaction.time, transaction.sender, transaction.recipient, transaction.nickName,
                        transaction.comment, transaction.balance, transaction.entity, transaction.userId
                    )
                }
                csvPrinter.flush()
            }
            Log.d("backupTransactionsToCSV", "Transactions backup saved to ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e("backupTransactionsToCSV", "Error saving transactions backup: ${e.message}")
            return null
        }
    }

    // Helper function to backup categories data to CSV
    fun backupCategoriesToCSV(context: Context, fileName: String, categoriesToBackup: List<TransactionCategory>): File? {
        try {
            val file = getInternalStorageFile(context, fileName)
            FileWriter(file).use { writer ->
                val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                    "id", "createdAt", "updatedAt", "name", "contains", "updatedTimes"
                ))

                categoriesToBackup.forEach { category ->
                    csvPrinter.printRecord(
                        category.id, category.createdAt, category.updatedAt,
                        category.name, category.contains.joinToString(","),
                        category.updatedTimes
                    )
                }
                csvPrinter.flush()
            }
            Log.d("backupCategoriesToCSV", "Categories backup saved to ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e("backupCategoriesToCSV", "Error saving categories backup: ${e.message}")
            return null
        }
    }

    // Helper function to backup category keywords data to CSV
    fun backupCategoryKeywordsToCSV(context: Context, fileName: String, categoryKeywordsToBackup: List<CategoryKeyword>): File? {
        try {
            val file = getInternalStorageFile(context, fileName)
            FileWriter(file).use { writer ->
                val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                    "id", "keyword", "nickName", "categoryId"
                ))

                categoryKeywordsToBackup.forEach { keyword ->
                    csvPrinter.printRecord(
                        keyword.id, keyword.keyword, keyword.nickName, keyword.categoryId
                    )
                }
                csvPrinter.flush()
            }
            Log.d("backupCategoryKeywordsToCSV", "Category Keywords backup saved to ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e("backupCategoryKeywordsToCSV", "Error saving category keywords backup: ${e.message}")
            return null
        }
    }

    // Helper function to backup category mappings data to CSV
    fun backupCategoryMappingsToCSV(context: Context, fileName: String, categoryMappingsToBackup: List<TransactionCategoryCrossRef>): File? {
        try {
            val file = getInternalStorageFile(context, fileName)
            FileWriter(file).use { writer ->
                val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                    "id", "transactionId", "categoryId"
                ))

                categoryMappingsToBackup.forEach { mapping ->
                    csvPrinter.printRecord(
                        mapping.id, mapping.transactionId, mapping.categoryId
                    )
                }
                csvPrinter.flush()
            }
            Log.d("backupCategoryMappingsToCSV", "Transaction Category Mappings backup saved to ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e("backupCategoryMappingsToCSV", "Error saving transaction category mappings backup: ${e.message}")
            return null
        }
    }

    fun backupDeletedTransactionsToCSV(context: Context, fileName: String, deletedTransactions: List<DeletedTransaction>): File? {
        try {
            val file = getInternalStorageFile(context, fileName)
            FileWriter(file).use { writer ->
                val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                    "id", "entity"
                ))

                deletedTransactions.forEach { transaction ->
                    csvPrinter.printRecord(
                        transaction.id, transaction.entity
                    )
                }
                csvPrinter.flush()
            }
            Log.d("backupDeletedTransactionsToCSV", "Transactions backup saved to ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e("backupDeletedTransactionsToCSV", "Error saving transactions backup: ${e.message}")
            return null
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