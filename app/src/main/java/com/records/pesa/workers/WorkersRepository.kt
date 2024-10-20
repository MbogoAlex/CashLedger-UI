package com.records.pesa.workers

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration

interface WorkersRepository {
    suspend fun fetchAndBackupTransactions(
        token: String,
        userId: Int,
        paymentStatus: Boolean
    )
}

class WorkersRepositoryImpl(context: Context): WorkersRepository {
    private val workManager = WorkManager.getInstance(context)
    override suspend fun fetchAndBackupTransactions(token: String, userId: Int, paymentStatus: Boolean) {

        workManager.cancelUniqueWork("fetch_and_backup_transactions_periodic")

        try {
            Log.d("backUpWork", "Fetching messages, user - $userId, token - $token")
            // Periodic work request for fetching messages
            val fetchMessagesPeriodicRequest = PeriodicWorkRequestBuilder<FetchMessagesWorker>(Duration.ofHours(4))
                .setInputData(workDataOf("userId" to userId, "token" to token, "paymentStatus" to paymentStatus))
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
