package com.records.pesa.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.records.pesa.CashLedger
import com.records.pesa.R
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.Budget
import com.records.pesa.db.models.BudgetCycleLog
import com.records.pesa.db.models.BudgetRecalcLog
import com.records.pesa.db.models.CategoryKeyword
import com.records.pesa.db.models.DeletedTransaction
import com.records.pesa.db.models.ManualBudgetTransaction
import com.records.pesa.db.models.ManualCategoryMember
import com.records.pesa.db.models.ManualTransaction
import com.records.pesa.db.models.ManualTransactionType
import com.records.pesa.db.models.Transaction
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionCategoryCrossRef
import com.records.pesa.mapper.toTransaction
import com.records.pesa.mapper.toTransactionCategory
import com.records.pesa.models.user.update.UserBackupDataUpdatePayload
import com.records.pesa.network.ApiRepository
import com.records.pesa.service.auth.AuthenticationManager
import com.records.pesa.service.category.CategoryService
import com.records.pesa.service.transaction.TransactionService
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.LocalDateTime

class BackupWorker(
    private val context: Context,
    private val workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    companion object {
        const val CHANNEL_ID = "backup_worker_channel"
        const val NOTIFICATION_ID = 1
    }

    override suspend fun doWork(): Result {
        Log.d("backUpWorker", "Uploading to server")
        val token = inputData.getString("token")
        val userId = inputData.getInt("userId", -1)
        val priorityHigh = inputData.getBoolean("priorityHigh", false)

        val appContext = context.applicationContext as? CashLedger
            ?: return Result.failure() // appContext was not found


        val dbRepository = appContext.container.dbRepository
        val transactionService = appContext.container.transactionService
        val categoryService = appContext.container.categoryService
        val apiRepository = appContext.container.apiRepository
        val authenticationManager = appContext.container.authenticationManager

        // Resolve userId — inputData for on-demand runs, DB fallback for periodic backup.
        // Token is NOT stored here: backup() uses authenticationManager.executeWithAuth which
        // reads the live JWT from UserSession, so user.token (legacy field) is irrelevant.
        val resolvedUserId: Int
        if (userId != -1) {
            resolvedUserId = userId
        } else {
            val user = dbRepository.getUser()?.first()
                ?: run {
                    Log.e("backUpWorker", "No user in DB — cannot backup")
                    return Result.failure()
                }
            resolvedUserId = user.backUpUserId.toInt()
            if (resolvedUserId <= 0) {
                Log.e("backUpWorker", "User has no valid backUpUserId")
                return Result.failure()
            }
        }

        // Set the worker as a foreground service with the initial notification
        setForegroundAsync(createForegroundInfo("Starting backup...", priorityHigh = priorityHigh))

        try {
            backup(
                context = context,
                worker = this,
                dbRepository = dbRepository,
                backUpId = resolvedUserId,
                transactionService = transactionService,
                categoryService = categoryService,
                priorityHigh = priorityHigh,
                apiRepository = apiRepository,
                authenticationManager = authenticationManager
            )
            // Update notification to indicate completion
            updateNotification("Backup completed successfully.", true, priorityHigh)
        } catch (e: Exception) {
            Log.e("backUpException", e.toString())
            updateNotification("Backup failed: $e.", false, priorityHigh)
        }


        return Result.success()
    }

    fun createForegroundInfo(progressMessage: String, isFinal: Boolean = false, priorityHigh: Boolean): ForegroundInfo {
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(if (isFinal) "Backup Completed" else "Backup in Progress")
            .setContentText(progressMessage)
            .setSmallIcon(R.drawable.cashledger_logo)
            .setOngoing(!isFinal) // Mark ongoing for non-final notifications
            .setAutoCancel(isFinal) // Allow dismiss only if final
            .setPriority(if (priorityHigh) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)

        // Include the foreground service type
        return ForegroundInfo(
            NOTIFICATION_ID,
            notificationBuilder.build(),
            FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }



    private fun updateNotification(message: String, isSuccess: Boolean, priorityHigh: Boolean) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setPriority(if (priorityHigh) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .setContentTitle(if (isSuccess) "Backup Successful" else "Backup Failed")
            .setContentText(message)
            .setSmallIcon(R.drawable.cashledger_logo)
            .setOngoing(false) // Allow user to dismiss
            .setAutoCancel(true) // Notification can be swiped away by the user
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

        // Auto-dismiss the notification after 3 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            notificationManager.cancel(NOTIFICATION_ID)
        }, 3000) // 3 seconds
    }

    fun updateProgressNotification(message: String, currentStep: Int, totalSteps: Int, priorityHigh: Boolean) {
        val progress = (currentStep.toFloat() / totalSteps.toFloat() * 100).toInt()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setPriority(if (priorityHigh) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .setContentTitle("Backup in Progress")
            .setContentText("$message ($progress%)")
            .setSmallIcon(R.drawable.cashledger_logo)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true) // Avoid multiple notification sounds
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(priorityHigh: Boolean) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Backup Worker Notifications",
            if (priorityHigh) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification channel for backup worker"
            importance = if (priorityHigh) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_LOW
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

}


suspend fun backup(
    priorityHigh: Boolean,
    context: Context,
    worker: BackupWorker,
    dbRepository: DBRepository,
    backUpId: Int,
    transactionService: TransactionService,
    categoryService: CategoryService,
    apiRepository: ApiRepository,
    authenticationManager: AuthenticationManager
) {


    val userAccount = dbRepository.getUser()?.first()
    worker.setForegroundAsync(worker.createForegroundInfo("Initializing backup...", priorityHigh = priorityHigh))

    val query = transactionService.createUserTransactionQuery(
        userId = backUpId,
        entity = null,
        categoryId = null,
        budgetId = null,
        transactionType = null,
        moneyDirection = null,
        startDate = LocalDate.now().minusYears(10),
        endDate = LocalDate.now(),
        latest = true
    )

    Log.d("backingUpInProcess", "LOADING...")
    val userDetails = dbRepository.getUser()?.first()
    val transactions = transactionService.getUserTransactions(query).first()
    Log.d("transactionsSize", "${transactions.size}, BackupId: $backUpId")
    val categories = categoryService.getAllCategories().first()
    val categoryKeywords = categoryService.getAllCategoryKeywords()
    val transactionCategoryMappings = categoryService.getTransactionCategoryCrossRefs().first()
    val deletedTransactions = transactionService.getDeletedTransactionEntities()
    val budgets = dbRepository.getAllBudgets().first()

    try {
        worker.setForegroundAsync(worker.createForegroundInfo("Backing up user data...", priorityHigh = priorityHigh))

        val totalSteps = 12 // transactions, categories, keywords, mappings, deleted, budgets, tx types, cat members, manual txs, manual budget txs, budget members, cycle logs
        var currentStep = 0

        val transactionsFileParts = mutableListOf<MultipartBody.Part>()
        val categoriesFileParts = mutableListOf<MultipartBody.Part>()
        val categoryKeywordsFileParts = mutableListOf<MultipartBody.Part>()
        val categoryMappingsFileParts = mutableListOf<MultipartBody.Part>()
        val deletedTransactionsFileParts = mutableListOf<MultipartBody.Part>()
        val budgetsFileParts = mutableListOf<MultipartBody.Part>()

        // Transactions
        val transactionsCsv = backupTransactionsToCSV(context, "${backUpId}_transactions.csv", transactions.map { it.toTransaction(backUpId) })
        transactionsCsv?.let {
            transactionsFileParts.add(it.toMultipartBody("file"))
        }

        if(transactionsCsv != null) {
            authenticationManager.executeWithAuth { token ->
                apiRepository.uploadFiles(token, transactionsFileParts)
            }
        }

        currentStep++
        worker.updateProgressNotification("Backing up transactions...", currentStep, totalSteps, priorityHigh)

        // Categories
        val categoriesCsv = backupCategoriesToCSV(context, "${backUpId}_categories.csv", categories.map { it.toTransactionCategory() })
        categoriesCsv?.let {
            categoriesFileParts.add(it.toMultipartBody("file"))
        }

        if (categoriesCsv != null) {
            authenticationManager.executeWithAuth { token ->
                apiRepository.uploadFiles(token, categoriesFileParts)
            }
        }

        currentStep++
        worker.updateProgressNotification("Backing up categories...", currentStep, totalSteps, priorityHigh)

        // Keywords
        val categoryKeywordsCsv = backupCategoryKeywordsToCSV(context, "${backUpId}_categoryKeywords.csv", categoryKeywords)
        categoryKeywordsCsv?.let {
            categoryKeywordsFileParts.add(it.toMultipartBody("file"))
        }
        if (categoryKeywordsCsv != null) {
            authenticationManager.executeWithAuth { token ->
                apiRepository.uploadFiles(token, categoryKeywordsFileParts)
            }
        }
        currentStep++
        worker.updateProgressNotification("Backing up category keywords...", currentStep, totalSteps, priorityHigh)

        // Mappings
        val categoryMappingsCsv = backupCategoryMappingsToCSV(context, "${backUpId}_transactionCategoryCrossRef.csv", transactionCategoryMappings)
        categoryMappingsCsv?.let {
            categoryMappingsFileParts.add(it.toMultipartBody("file"))
        }
        if (categoryMappingsCsv != null) {
            authenticationManager.executeWithAuth { token ->
                apiRepository.uploadFiles(token, categoryMappingsFileParts)
            }
        }
        currentStep++
        worker.updateProgressNotification("Backing up mappings...", currentStep, totalSteps, priorityHigh)

        // Deleted Transactions
        val deletedTransactionsCsv = backupDeletedTransactionsToCSV(context, "${backUpId}_deletedTransactions.csv", deletedTransactions)
        deletedTransactionsCsv?.let {
            deletedTransactionsFileParts.add(it.toMultipartBody("file"))
        }
        if (deletedTransactionsCsv != null) {
            authenticationManager.executeWithAuth { token ->
                apiRepository.uploadFiles(token, deletedTransactionsFileParts)
            }
        }
        currentStep++
        worker.updateProgressNotification("Finalizing backup...", currentStep, totalSteps, priorityHigh)

        // Budgets
        val budgetsCsv = backupBudgetsToCSV(context, "${backUpId}_budgets.csv", budgets)
        budgetsCsv?.let { budgetsFileParts.add(it.toMultipartBody("file")) }
        if (budgetsCsv != null) {
            authenticationManager.executeWithAuth { token ->
                apiRepository.uploadFiles(token, budgetsFileParts)
            }
        }
        currentStep++
        worker.updateProgressNotification("Backing up budgets...", currentStep, totalSteps, priorityHigh)

        // Manual Transaction Types
        val manualTransactionTypes = dbRepository.getAllManualTransactionTypesOnce()
        val manualTxTypesCsv = backupManualTransactionTypesToCSV(context, "${backUpId}_manualTransactionTypes.csv", manualTransactionTypes)
        manualTxTypesCsv?.let {
            val parts = mutableListOf(it.toMultipartBody("file"))
            authenticationManager.executeWithAuth { token ->
                apiRepository.uploadFiles(token, parts)
            }
        }
        currentStep++
        worker.updateProgressNotification("Backing up transaction types...", currentStep, totalSteps, priorityHigh)

        // Manual Category Members
        val manualCategoryMembers = dbRepository.getAllManualCategoryMembersOnce()
        val manualMembersCsv = backupManualCategoryMembersToCSV(context, "${backUpId}_manualCategoryMembers.csv", manualCategoryMembers)
        manualMembersCsv?.let {
            val parts = mutableListOf(it.toMultipartBody("file"))
            authenticationManager.executeWithAuth { token ->
                apiRepository.uploadFiles(token, parts)
            }
        }
        currentStep++
        worker.updateProgressNotification("Backing up category members...", currentStep, totalSteps, priorityHigh)

        // Manual Transactions (category-scoped)
        val manualTransactions = dbRepository.getAllManualTransactionsOnce()
        val manualTxsCsv = backupManualTransactionsToCSV(context, "${backUpId}_manualTransactions.csv", manualTransactions)
        manualTxsCsv?.let {
            val parts = mutableListOf(it.toMultipartBody("file"))
            authenticationManager.executeWithAuth { token ->
                apiRepository.uploadFiles(token, parts)
            }
        }
        currentStep++
        worker.updateProgressNotification("Backing up manual transactions...", currentStep, totalSteps, priorityHigh)

        // Manual Budget Transactions
        val manualBudgetTxs = dbRepository.getAllManualBudgetTransactionsOnce()
        val manualBudgetTxsCsv = backupManualBudgetTransactionsToCSV(context, "${backUpId}_manualBudgetTransactions.csv", manualBudgetTxs)
        manualBudgetTxsCsv?.let {
            val parts = mutableListOf(it.toMultipartBody("file"))
            authenticationManager.executeWithAuth { token ->
                apiRepository.uploadFiles(token, parts)
            }
        }
        currentStep++
        worker.updateProgressNotification("Backing up budget transactions...", currentStep, totalSteps, priorityHigh)

        // Budget Members
        val budgetMembers = dbRepository.getAllBudgetMembersOnce()
        val budgetMembersCsv = backupBudgetMembersToCSV(context, "${backUpId}_budgetMembers.csv", budgetMembers)
        budgetMembersCsv?.let {
            val parts = mutableListOf(it.toMultipartBody("file"))
            authenticationManager.executeWithAuth { token ->
                apiRepository.uploadFiles(token, parts)
            }
        }
        currentStep++
        worker.updateProgressNotification("Backing up budget members...", currentStep, totalSteps, priorityHigh)

        // Budget Cycle Logs (recurring budget history)
        val budgetCycleLogs = dbRepository.getAllBudgetCycleLogsOnce()
        val budgetCycleLogsCsv = backupBudgetCycleLogsToCSV(context, "${backUpId}_budgetCycleLogs.csv", budgetCycleLogs)
        budgetCycleLogsCsv?.let {
            val parts = mutableListOf(it.toMultipartBody("file"))
            authenticationManager.executeWithAuth { token ->
                apiRepository.uploadFiles(token, parts)
            }
        }
        currentStep++
        worker.updateProgressNotification("Backing up budget cycle history...", currentStep, totalSteps, priorityHigh)

        // Budget Recalc Logs (audit trail)
        val budgetRecalcLogs = dbRepository.getAllBudgetRecalcLogsOnce()
        val budgetRecalcLogsCsv = backupBudgetRecalcLogsToCSV(context, "${backUpId}_budgetRecalcLogs.csv", budgetRecalcLogs)
        budgetRecalcLogsCsv?.let {
            val parts = mutableListOf(it.toMultipartBody("file"))
            authenticationManager.executeWithAuth { token ->
                apiRepository.uploadFiles(token, parts)
            }
        }
        currentStep++
        worker.updateProgressNotification("Backing up audit trail...", currentStep, totalSteps, priorityHigh)
        val lastBackup = LocalDateTime.now()
        val totalItems = transactions.size + categories.size + categoryKeywords.size + transactionCategoryMappings.size


//        val userBackupDataUpdatePayload = UserBackupDataUpdatePayload(
//            userId = userId.toString(),
//            lastBackup = lastBackup.toString(),
//            backupItemsSize = totalItems.toString(),
//            transactions = transactions.size.toString(),
//            categories = categories.size.toString(),
//            categoryKeywords = categoryKeywords.size.toString(),
//            categoryMappings = transactionCategoryMappings.size.toString()
//        )
//
//        authenticationManager.executeWithAuth { token ->
//            apiRepository.updateUserProfileBackupData(
//                token = token,
//                userBackupDataUpdatePayload = userBackupDataUpdatePayload
//            )
//        }

        dbRepository.updateUser(
            user = userDetails!!.copy(
                lastBackup = lastBackup,
                backedUpItemsSize = totalItems,
                backupSet = true,
                backupWorkerInitiated = true,
                transactions = transactions.size,
                categories = categories.size,
                categoryKeywords = categoryKeywords.size,
                categoryMappings = transactionCategoryMappings.size
            )
        )

        Log.d("backUpSuccess", "SUCCESS")
        worker.updateProgressNotification("Back up successful", currentStep, totalSteps, priorityHigh)
    } catch (e: Exception) {
        worker.updateProgressNotification("Back failed. Try again later", 0, 0, priorityHigh)
        Log.e("backUpException", e.toString())
    }
}

fun File.toMultipartBody(partName: String): MultipartBody.Part {
    val requestBody = this.asRequestBody("text/csv".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(partName, this.name, requestBody)
}



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
                "id", "createdAt", "updatedAt", "name", "contains", "updatedTimes", "deletedAt"
            ))

            categoriesToBackup.forEach { category ->
                csvPrinter.printRecord(
                    category.id, category.createdAt, category.updatedAt,
                    category.name, category.contains.joinToString(","),
                    category.updatedTimes, category.deletedAt
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

fun backupBudgetsToCSV(context: Context, fileName: String, budgetsToBackup: List<Budget>): File? {
    try {
        val file = getInternalStorageFile(context, fileName)
        FileWriter(file).use { writer ->
            val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                "id", "name", "active", "expenditure", "budgetLimit",
                "createdAt", "startDate", "limitDate", "limitReached", "limitReachedAt",
                "exceededBy", "categoryId", "alertThreshold",
                "isRecurring", "recurrenceType", "recurrenceIntervalDays", "cycleNumber", "deletedAt"
            ))
            budgetsToBackup.forEach { budget ->
                csvPrinter.printRecord(
                    budget.id, budget.name, budget.active, budget.expenditure,
                    budget.budgetLimit, budget.createdAt, budget.startDate, budget.limitDate,
                    budget.limitReached, budget.limitReachedAt, budget.exceededBy,
                    budget.categoryId, budget.alertThreshold,
                    budget.isRecurring, budget.recurrenceType, budget.recurrenceIntervalDays, budget.cycleNumber,
                    budget.deletedAt
                )
            }
            csvPrinter.flush()
        }
        Log.d("backupBudgetsToCSV", "Budgets backup saved to ${file.absolutePath}")
        return file
    } catch (e: Exception) {
        Log.e("backupBudgetsToCSV", "Error saving budgets backup: ${e.message}")
        return null
    }
}

fun backupManualTransactionTypesToCSV(context: Context, fileName: String, types: List<ManualTransactionType>): File? {
    try {
        val file = getInternalStorageFile(context, fileName)
        FileWriter(file).use { writer ->
            val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("id", "name", "isOutflow", "isCustom", "createdAt"))
            types.forEach { t ->
                csvPrinter.printRecord(t.id, t.name, if (t.isOutflow) 1 else 0, if (t.isCustom) 1 else 0, t.createdAt)
            }
            csvPrinter.flush()
        }
        return file
    } catch (e: Exception) {
        Log.e("backupManualTxTypes", "Error: ${e.message}")
        return null
    }
}

fun backupManualCategoryMembersToCSV(context: Context, fileName: String, members: List<ManualCategoryMember>): File? {
    try {
        val file = getInternalStorageFile(context, fileName)
        FileWriter(file).use { writer ->
            val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("id", "categoryId", "name", "createdAt", "deletedAt"))
            members.forEach { m ->
                csvPrinter.printRecord(m.id, m.categoryId, m.name, m.createdAt, m.deletedAt)
            }
            csvPrinter.flush()
        }
        return file
    } catch (e: Exception) {
        Log.e("backupManualMembers", "Error: ${e.message}")
        return null
    }
}

fun backupManualTransactionsToCSV(context: Context, fileName: String, txs: List<ManualTransaction>): File? {
    try {
        val file = getInternalStorageFile(context, fileName)
        FileWriter(file).use { writer ->
            val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                "id", "categoryId", "memberName", "transactionTypeName", "isOutflow", "amount", "description", "date", "createdAt", "deletedAt"
            ))
            txs.forEach { tx ->
                csvPrinter.printRecord(
                    tx.id, tx.categoryId, tx.memberName, tx.transactionTypeName,
                    if (tx.isOutflow) 1 else 0, tx.amount, tx.description, tx.date, tx.createdAt, tx.deletedAt
                )
            }
            csvPrinter.flush()
        }
        return file
    } catch (e: Exception) {
        Log.e("backupManualTxs", "Error: ${e.message}")
        return null
    }
}

fun backupManualBudgetTransactionsToCSV(context: Context, fileName: String, txs: List<ManualBudgetTransaction>): File? {
    try {
        val file = getInternalStorageFile(context, fileName)
        FileWriter(file).use { writer ->
            val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                "id", "budgetId", "amount", "description", "date", "createdAt"
            ))
            txs.forEach { tx ->
                csvPrinter.printRecord(tx.id, tx.budgetId, tx.amount, tx.description, tx.date, tx.createdAt)
            }
            csvPrinter.flush()
        }
        return file
    } catch (e: Exception) {
        Log.e("backupManualBudgetTxs", "Error: ${e.message}")
        return null
    }
}

fun backupBudgetMembersToCSV(context: Context, fileName: String, members: List<com.records.pesa.db.models.BudgetMember>): File? {
    return try {
        val file = getInternalStorageFile(context, fileName)
        FileWriter(file).use { writer ->
            val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("id", "budgetId", "memberName"))
            members.forEach { m ->
                csvPrinter.printRecord(m.id, m.budgetId, m.memberName)
            }
            csvPrinter.flush()
        }
        file
    } catch (e: Exception) {
        Log.e("backupBudgetMembers", "Error: ${e.message}")
        null
    }
}

fun backupBudgetCycleLogsToCSV(context: Context, fileName: String, logs: List<BudgetCycleLog>): File? {
    return try {
        val file = getInternalStorageFile(context, fileName)
        FileWriter(file).use { writer ->
            val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                "id", "budgetId", "budgetName", "cycleNumber",
                "cycleStartDate", "cycleEndDate", "budgetLimit",
                "finalExpenditure", "limitReached", "exceededBy", "closedAt"
            ))
            logs.forEach { log ->
                csvPrinter.printRecord(
                    log.id, log.budgetId, log.budgetName, log.cycleNumber,
                    log.cycleStartDate, log.cycleEndDate, log.budgetLimit,
                    log.finalExpenditure, log.limitReached, log.exceededBy, log.closedAt
                )
            }
            csvPrinter.flush()
        }
        Log.d("backupBudgetCycleLogs", "Cycle logs backed up: ${logs.size} records")
        file
    } catch (e: Exception) {
        Log.e("backupBudgetCycleLogs", "Error: ${e.message}")
        null
    }
}

fun backupBudgetRecalcLogsToCSV(context: Context, fileName: String, logs: List<BudgetRecalcLog>): File? {
    return try {
        val file = getInternalStorageFile(context, fileName)
        FileWriter(file).use { writer ->
            val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                "id", "budgetId", "budgetName", "timestamp",
                "oldExpenditure", "newExpenditure", "thresholdCrossed",
                "cycleStartDate", "cycleEndDate"
            ))
            logs.forEach { log ->
                csvPrinter.printRecord(
                    log.id, log.budgetId, log.budgetName, log.timestamp,
                    log.oldExpenditure, log.newExpenditure, log.thresholdCrossed,
                    log.cycleStartDate, log.cycleEndDate
                )
            }
            csvPrinter.flush()
        }
        Log.d("backupBudgetRecalcLogs", "Recalc logs backed up: ${logs.size} records")
        file
    } catch (e: Exception) {
        Log.e("backupBudgetRecalcLogs", "Error: ${e.message}")
        null
    }
}
