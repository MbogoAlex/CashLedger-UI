package com.records.pesa.workers

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
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
}

class WorkersRepositoryImpl(context: Context): WorkersRepository {
    private val workManager = WorkManager.getInstance(context)
    override suspend fun fetchAndBackupTransactions(token: String, userId: Int, paymentStatus: Boolean, priorityHigh: Boolean,) {

        workManager.cancelUniqueWork("fetch_and_backup_transactions_periodic")

        try {
            Log.d("backUpWork", "Fetching messages, user - $userId, token - $token")

            // Define constraints for network availability (any network)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // Allows work on any network type
                .build()
            // Periodic work request for fetching messages
                val fetchMessagesPeriodicRequest = PeriodicWorkRequestBuilder<FetchMessagesWorker>(Duration.ofHours(12))
                    .setInputData(workDataOf("userId" to userId, "token" to token, "paymentStatus" to paymentStatus, "priorityHigh" to priorityHigh))
                    .setConstraints(constraints)
    //                .setInitialDelay(Duration.ofSeconds(10))
                    .build()

            // Enqueue the periodic work request
            workManager.enqueueUniquePeriodicWork(
                "fetch_and_backup_transactions_periodic",
                ExistingPeriodicWorkPolicy.UPDATE,
                fetchMessagesPeriodicRequest
            )
            Log.d("backUpWork", "Backup initiated")
        } catch (e: Exception) {
            Log.e("backUpWork", e.toString())
        }
    }

}
