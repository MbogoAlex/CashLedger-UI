package com.records.pesa.workers

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import com.records.pesa.models.SmsMessage
import com.records.pesa.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FetchMessagesWorker(
    private val context: Context,
    private val params: WorkerParameters,
): CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val latestTransactionCode = getLatestTransactionCode()
        val messagesToSend = fetchSmsMessages(context, latestTransactionCode)

        val messagesJson = Gson().toJson(messagesToSend)  //

        return withContext(Dispatchers.IO) {
            Result.success(
                workDataOf(
                    WorkerKeys.MESSAGES to messagesJson,
                )
            )
        }
    }
}

suspend fun getLatestTransactionCode(): String? {
    val response = ApiService.instance.getLatestTransactionCode(
        userId = 1
    )
    return if(response.isSuccessful && response.body()?.data?.transaction!!.isNotEmpty()) {
        response.body()?.data?.transaction?.get(0)
    } else {
        null
    }
}

fun fetchSmsMessages(context: Context, latestTransactionCode: String?): List<SmsMessage> {
    val messages = mutableListOf<SmsMessage>()
    val uri = Uri.parse("content://sms/inbox")
    val projection = arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE)
    val selection = "${Telephony.Sms.ADDRESS} LIKE ?"
    val selectionArgs = arrayOf("%MPESA%")
    val sortOrder = "${Telephony.Sms.DATE} DESC"

    val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)

    cursor?.use {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (it.moveToNext()) {
            val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
            val dateMillis = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
            val date = Date(dateMillis)
            val formattedDate = dateFormat.format(date)
            val formattedTime = timeFormat.format(date)
            messages.add(SmsMessage(body, formattedDate, formattedTime))
        }
    }


    val messagesToSend = filterMessagesToSend(messages, latestTransactionCode?.strip()?.lowercase())
    return messagesToSend;
}

private fun filterMessagesToSend(messages: List<SmsMessage>, latestTransactionCode: String?): List<SmsMessage> {
    Log.d("EXISTING", latestTransactionCode.toString())
    var i = 0
    val messagesToSend = mutableListOf<SmsMessage>()
    val newTransactionCodes = getNewTransactionCodes(messages);
    if(newTransactionCodes.isNotEmpty() && latestTransactionCode != null) {
        for(code in newTransactionCodes) {
            Log.d("COMPARISON", "${code["code"]} $latestTransactionCode")
            if(code["code"] == latestTransactionCode) {
                Log.d("BREAK_LOOP", "BREAK")
                break
            }
            messagesToSend.add(code["message"] as SmsMessage)
        }

    } else if(latestTransactionCode == null && newTransactionCodes.isNotEmpty()) {
        messagesToSend.addAll(newTransactionCodes.map { it["message"] as SmsMessage })
    }
    Log.d("ADDING", "$i messages")
    Log.d("MESSAGES_TO_SEND", "$i messages")
    return messagesToSend;
}

private fun getNewTransactionCodes(messages: List<SmsMessage>): List<Map<String, Any>> {
    val transactionCodes = mutableListOf<Map<String, Any>>()
    for(message in messages) {
        try {
            val transactionCodeMatcher = Regex("\\b\\w{10}\\b").find(message.body)
            if (transactionCodeMatcher != null) {
                val transactionCode = transactionCodeMatcher.value
                transactionCodes.add(mapOf("code" to transactionCode.strip().lowercase(), "message" to message))
            }
        } catch (e: Exception) {
            Log.e("Exception:", e.toString())
        }
    }
    return transactionCodes;
}