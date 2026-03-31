package com.records.pesa.workers

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration

interface WorkersRepository {
    suspend fun fetchAndBackupTransactions(
        token: String,
        userId: Int,
        paymentStatus: Boolean,
        priorityHigh: Boolean,
    )

    suspend fun submitSmsMessages(
        userId: Long,
        userPhone: String
    )
    
    suspend fun runSafaricomMigration()

    /** Debounced one-shot backup triggered after a local DB change. */
    fun triggerBackupAfterChange(token: String, userId: Int)

    /** Schedules (or keeps) a 4-hour periodic full backup. */
    fun startPeriodicBackup()
}

class WorkersRepositoryImpl(context: Context): WorkersRepository {
    private val workManager = WorkManager.getInstance(context)
    override suspend fun fetchAndBackupTransactions(token: String, userId: Int, paymentStatus: Boolean, priorityHigh: Boolean,) {

        try {
            Log.d("backUpWork", "Scheduling fetch_and_backup_transactions_periodic")

            // No network constraint — SMS reading is local; BackupWorker handles the network part
            val fetchMessagesPeriodicRequest = PeriodicWorkRequestBuilder<FetchMessagesWorker>(Duration.ofHours(6))
                    .build()

            // KEEP: don't cancel existing work — if it's running, let it finish
            workManager.enqueueUniquePeriodicWork(
                "fetch_and_backup_transactions_periodic",
                ExistingPeriodicWorkPolicy.KEEP,
                fetchMessagesPeriodicRequest
            )
            Log.d("backUpWork", "Periodic SMS fetch scheduled")

            // Also ensure the 4-hour periodic backup is running
            startPeriodicBackup()
        } catch (e: Exception) {
            Log.e("backUpWork", e.toString())
        }
    }

    override suspend fun submitSmsMessages(userId: Long, userPhone: String) {
        try {
            Log.d("SmsSubmissionWork", "Starting SMS submission for user $userId")

            // Define constraints for network availability
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Create one-time work request for SMS submission
            val smsSubmissionRequest = OneTimeWorkRequestBuilder<SmsSubmissionWorker>()
                .setInputData(
                    workDataOf(
                        SmsSubmissionWorker.KEY_USER_ID to userId,
                        SmsSubmissionWorker.KEY_USER_PHONE to userPhone
                    )
                )
                .setConstraints(constraints)
                .build()

            // Enqueue the work request
            workManager.enqueueUniqueWork(
                "sms_submission_${userId}",
                ExistingWorkPolicy.REPLACE,
                smsSubmissionRequest
            )

            Log.d("SmsSubmissionWork", "SMS submission work enqueued for user $userId")

        } catch (e: Exception) {
            Log.e("SmsSubmissionWork", "Error enqueueing SMS submission work", e)
        }
    }
    
    override suspend fun runSafaricomMigration() {
        try {
            Log.d("SafaricomMigration", "Starting SafaricomMigration migration for masked phone numbers")

            // Create one-time work request for migration
            val migrationRequest = OneTimeWorkRequestBuilder<TransactionMigrationWorker>()
                .build()

            // Enqueue the work request
            workManager.enqueueUniqueWork(
                "safaricom_masked_phone_migration",
                ExistingWorkPolicy.REPLACE,
                migrationRequest
            )

            Log.d("SafaricomMigration", "Migration work enqueued")

        } catch (e: Exception) {
            Log.e("SafaricomMigration", "Error enqueueing migration work", e)
        }
    }

    override fun startPeriodicBackup() {
        try {
            val request = PeriodicWorkRequestBuilder<BackupWorker>(Duration.ofHours(4))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            workManager.enqueueUniquePeriodicWork(
                "periodic_backup",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d("backUpWork", "4-hour periodic backup scheduled")
        } catch (e: Exception) {
            Log.e("backUpWork", "Error scheduling periodic backup", e)
        }
    }

    override fun triggerBackupAfterChange(token: String, userId: Int) {
        try {
            val request = OneTimeWorkRequestBuilder<BackupWorker>()
                .setInitialDelay(30, java.util.concurrent.TimeUnit.SECONDS)
                .setInputData(workDataOf("token" to token, "userId" to userId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            workManager.enqueueUniqueWork(
                "change_triggered_backup",
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d("backUpWork", "Change-triggered backup scheduled (30s debounce)")
        } catch (e: Exception) {
            Log.e("backUpWork", "Error scheduling change-triggered backup", e)
        }
    }

}
