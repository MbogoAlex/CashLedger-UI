package com.records.pesa.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.records.pesa.CashLedger
import com.records.pesa.R
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.CategoryKeyword
import com.records.pesa.db.models.DeletedTransaction
import com.records.pesa.db.models.Transaction
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionCategoryCrossRef
import com.records.pesa.mapper.toTransaction
import com.records.pesa.mapper.toTransactionCategory
import com.records.pesa.models.user.UserAccount
import com.records.pesa.network.SupabaseClient.client
import com.records.pesa.service.category.CategoryService
import com.records.pesa.service.transaction.TransactionService
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.coroutines.flow.first
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
        val token = inputData.getString("token")
        val userId = inputData.getInt("userId", -1)

        val appContext = context.applicationContext as? CashLedger
            ?: return Result.failure() // appContext was not found


        val dbRepository = appContext.container.dbRepository
        val transactionService = appContext.container.transactionService
        val categoryService = appContext.container.categoryService

        if (userId == -1) {
            return Result.failure()
        }

        // Set the worker as a foreground service with the initial notification
        setForegroundAsync(createForegroundInfo("Starting backup..."))

        try {
            backup(
                context = context,
                worker = this,
                dbRepository = dbRepository,
                userId = userId,
                transactionService = transactionService,
                categoryService = categoryService
            )
            // Update notification to indicate completion
            updateNotification("Backup completed successfully.", true)
        } catch (e: Exception) {
            Log.e("backUpException", e.toString())
            updateNotification("Backup failed: $e.", false)
        }


        return Result.success()
    }

    fun createForegroundInfo(progressMessage: String, isFinal: Boolean = false): ForegroundInfo {
        createNotificationChannel(NotificationManager.IMPORTANCE_LOW) // Set to low to remove sound

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(if (isFinal) "Backup Completed" else "Backup in Progress")
            .setContentText(progressMessage)
            .setSmallIcon(R.drawable.cashledger_logo)
            .setProgress(0, 0, true) // Indeterminate progress for ongoing tasks
            .setOngoing(true) // Allow dismiss if final
            .setAutoCancel(true) // Allow user to swipe away only if final
            .setOnlyAlertOnce(true) // Prevent multiple alerts
            .setSound(null) // Disable sound
            .setVibrate(null) // Disable vibration

        return ForegroundInfo(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun updateNotification(message: String, isSuccess: Boolean) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(if (isSuccess) "Backup Successful" else "Backup Failed")
            .setContentText(message)
            .setSmallIcon(R.drawable.cashledger_logo)
            .setOngoing(false) // Allow user to dismiss
            .setAutoCancel(true) // Notification can be swiped away by the user
            .setSound(null) // Disable sound
            .setVibrate(null) // Disable vibration
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun updateProgressNotification(message: String, currentStep: Int, totalSteps: Int) {
        val progress = (currentStep.toFloat() / totalSteps.toFloat() * 100).toInt()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Backup in Progress")
            .setContentText("$message ($progress%)")
            .setSmallIcon(R.drawable.cashledger_logo)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true) // Avoid multiple notification sounds
            .setSound(null) // Disable sound
            .setVibrate(null) // Disable vibration
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(importance: Int) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Backup Worker Notifications",
            importance // Use IMPORTANCE_LOW or IMPORTANCE_DEFAULT for no sound
        ).apply {
            description = "Notification channel for backup worker"
            setSound(null, null) // Disable sound
            enableVibration(false) // Disable vibration
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

suspend fun backup(
    context: Context,
    worker: BackupWorker,
    dbRepository: DBRepository,
    userId: Int,
    transactionService: TransactionService,
    categoryService: CategoryService
) {


    worker.setForegroundAsync(worker.createForegroundInfo("Initializing backup..."))

    val query = transactionService.createUserTransactionQuery(
        userId = userId,
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
    val userDetails = dbRepository.getUser(userId = userId).first()
    val transactions = transactionService.getUserTransactions(query).first()
    val categories = categoryService.getAllCategories().first()
    val categoryKeywords = categoryService.getAllCategoryKeywords()
    val transactionCategoryMappings = categoryService.getTransactionCategoryCrossRefs().first()
    val deletedTransactions = transactionService.getDeletedTransactionEntities()

    try {
        worker.setForegroundAsync(worker.createForegroundInfo("Backing up user data..."))

        val user = client.postgrest["userAccount"].select {
            filter { eq("id", userId) }
        }.decodeSingle<UserAccount>()

        val bucketName = "cashLedgerBackup"
        val bucket = client.storage[bucketName]

        val totalSteps = 5 // Define the total number of tasks
        var currentStep = 0

        // Transactions
        val transactionsCsv = backupTransactionsToCSV(context, "${userId}_transactions.csv", transactions.map { it.toTransaction(userId) })
        if (transactionsCsv != null) {
            bucket.upload("${userId}_transactions.csv", transactionsCsv, true)
        }
        currentStep++
        worker.updateProgressNotification("Backing up transactions...", currentStep, totalSteps)

        // Categories
        val categoriesCsv = backupCategoriesToCSV(context, "${userId}_categories.csv", categories.map { it.toTransactionCategory() })
        if (categoriesCsv != null) {
            bucket.upload("${userId}_categories.csv", categoriesCsv, true)
        }
        currentStep++
        worker.updateProgressNotification("Backing up categories...", currentStep, totalSteps)

        // Keywords
        val categoryKeywordsCsv = backupCategoryKeywordsToCSV(context, "${userId}_categoryKeywords.csv", categoryKeywords)
        if (categoryKeywordsCsv != null) {
            bucket.upload("${userId}_categoryKeywords.csv", categoryKeywordsCsv, true)
        }
        currentStep++
        worker.updateProgressNotification("Backing up category keywords...", currentStep, totalSteps)

        // Mappings
        val categoryMappingsCsv = backupCategoryMappingsToCSV(context, "${userId}_transactionCategoryCrossRef.csv", transactionCategoryMappings)
        if (categoryMappingsCsv != null) {
            bucket.upload("${userId}_transactionCategoryCrossRef.csv", categoryMappingsCsv, true)
        }
        currentStep++
        worker.updateProgressNotification("Backing up mappings...", currentStep, totalSteps)

        // Deleted Transactions
        val deletedTransactionsCsv = backupDeletedTransactionsToCSV(context, "${userId}_deletedTransactions.csv", deletedTransactions)
        if (deletedTransactionsCsv != null) {
            bucket.upload("${userId}_deletedTransactions.csv", deletedTransactionsCsv, true)
        }
        currentStep++
        worker.updateProgressNotification("Finalizing backup...", currentStep, totalSteps)

        // Final Step: Update User Account
        val lastBackup = LocalDateTime.now()
        val totalItems = transactions.size + categories.size + categoryKeywords.size + transactionCategoryMappings.size

        client.postgrest["userAccount"].update(user.copy(
            lastBackup = lastBackup.toString(),
            backedUpItemsSize = totalItems,
            backupSet = true,
            transactions = transactions.size,
            categories = categories.size,
            categoryKeywords = categoryKeywords.size,
            categoryMappings = transactionCategoryMappings.size
        )) {
            filter { eq("id", userId) }
        }

        dbRepository.updateUser(
            user = userDetails.copy(
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
        worker.updateProgressNotification("Back up successful", currentStep, totalSteps)
    } catch (e: Exception) {
        Log.e("backUpException", e.toString())
    }
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
