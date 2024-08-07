package com.records.pesa.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.records.pesa.models.SmsMessage
import com.records.pesa.network.ApiService

class PostMessagesWorker(
    private val context: Context,
    private val workerParameters: WorkerParameters
): CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        val token = inputData.getString("token")
        val userId = inputData.getInt("userId", -1)
        if(userId == -1) {
            return Result.failure()
        }
        val messagesJson = workerParameters.inputData.getString(WorkerKeys.MESSAGES)
        val messagesToSend: List<SmsMessage> = Gson().fromJson(
            messagesJson,
            object : TypeToken<List<SmsMessage>>() {}.type
        )
        if(messagesToSend.isNotEmpty()) {
            val allMessagesSent = postMessagesInBatches(token!!, userId, messagesToSend)
            if(allMessagesSent) {
                return Result.success()
            }
        }

        return Result.success()
    }

}

suspend fun postMessagesInBatches(token: String, userId: Int, messages: List<SmsMessage>): Boolean {

    val batchSize = 1000
    val totalBatches = (messages.size + batchSize - 1) / batchSize

    var messagesSent = 0
    var allMessagesSent = false
    for (i in 0 until totalBatches) {
        val fromIndex = i * batchSize
        val toIndex = minOf(fromIndex + batchSize, messages.size)
        val batch = messages.subList(fromIndex, toIndex)
        messagesSent += batch.size
        allMessagesSent = postMessages(token, userId, batch, messagesSent == messages.size)
    }
    return allMessagesSent
}

suspend fun postMessages(token: String, userId: Int, messages: List<SmsMessage>, allMessagesSent: Boolean): Boolean {
    Log.i("SENDING", " ${messages.size} messages")
    try {
        val response = ApiService.instance.postMessages(
            token = "Bearer $token",
            messages = messages,
            id = userId
        )

        if (response.isSuccessful) {
            return allMessagesSent

        }

    } catch (e: Exception) {
        return false
    }
    return allMessagesSent
}