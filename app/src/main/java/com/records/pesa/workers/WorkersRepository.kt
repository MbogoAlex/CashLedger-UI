package com.records.pesa.workers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration

interface WorkersRepository {
    suspend fun fetchAndPostMessages(userId: Int)
}

class WorkersRepositoryImpl(context: Context): WorkersRepository {
    private val workManager = WorkManager.getInstance(context)
    override suspend fun fetchAndPostMessages(userId: Int) {
        // Periodic work request for fetching messages
        val fetchMessagesPeriodicRequest = PeriodicWorkRequestBuilder<FetchMessagesWorker>(Duration.ofHours(12))
            .setInputData(workDataOf("userId" to userId))
            .setInitialDelay(Duration.ofHours(12))
            .build()

        // Enqueue the periodic work request
        workManager.enqueueUniquePeriodicWork(
            "fetch_and_post_messages_periodic",
            ExistingPeriodicWorkPolicy.REPLACE,
            fetchMessagesPeriodicRequest
        )
    }

}