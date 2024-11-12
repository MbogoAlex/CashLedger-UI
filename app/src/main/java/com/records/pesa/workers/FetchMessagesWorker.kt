package com.records.pesa.workers

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.records.pesa.CashLedger
import com.records.pesa.container.AppContainerImpl
import com.records.pesa.db.models.CategoryWithTransactions
import com.records.pesa.db.models.UserAccount
import com.records.pesa.mapper.toTransactionCategory
import com.records.pesa.models.MessageData
import com.records.pesa.models.SmsMessage
import com.records.pesa.service.transaction.TransactionService
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FetchMessagesWorker(
    private val context: Context,
    private val params: WorkerParameters,
): CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {

        try {
            val appContext = context.applicationContext as? CashLedger
                ?: return Result.failure() // appContext was not found

            // Ensure AppContainer is initialized
            appContext.container = AppContainerImpl(appContext)

            val transactionService = appContext.container.transactionService
            val categoryService = appContext.container.categoryService
            val userAccountService = appContext.container.userAccountService

            val userId = inputData.getInt("userId", -1)
            val token = inputData.getString("token")
            val paymentStatus = inputData.getBoolean("paymentStatus", false)
            if(userId == -1) {
                return Result.failure()
            }
            val latestTransactionCode = transactionService.getLatestTransactionCode().first()

            fetchSmsMessages(
                context = context,
                transactionService = transactionService,
                userAccount = userAccountService.getUserAccount(userId = userId).first(),
                categories = categoryService.getAllCategories().first(),
                existing = latestTransactionCode
            )

            // Once fetching is done, enqueue the posting work
            val postMessagesRequest = OneTimeWorkRequestBuilder<BackupWorker>()
                .setInputData(workDataOf("userId" to userId, "token" to token))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(postMessagesRequest)
            Log.d("backUpWork", "SUCCESS")

            return Result.success()
        } catch (e: Exception) {
            Log.e("backUpWork", e.toString())
            return Result.failure()
        }


    }
}

fun fetchSmsMessages(context: Context, transactionService: TransactionService, userAccount: UserAccount, categories: List<CategoryWithTransactions>, existing: String?) {
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


    val messagesToSend = filterMessagesToSend(
        messages = messages,
        transactionService = transactionService,
        userAccount = userAccount,
        categories = categories,
        existing = existing
    )
    Log.d("MESSAGES_ADDITION", "ADDED ${messagesToSend.size} MESSAGES")

}

fun filterMessagesToSend(messages: List<SmsMessage>, transactionService: TransactionService, userAccount: UserAccount, categories: List<CategoryWithTransactions>, existing: String?): List<SmsMessage> {
    val messagesToSend = mutableListOf<SmsMessage>()
    val newTransactionCodes = getNewTransactionCodes(messages);
    Log.d("NEW_MESSAGES", "GOT ${newTransactionCodes.size} MESSAGES")

    Log.d("EXISTING", existing.toString())
    if(newTransactionCodes.isNotEmpty() && !existing.isNullOrEmpty()) {
        for(code in newTransactionCodes) {
            Log.d("COMPARISON", "${code["code"]} $existing")
            if(code["code"] == existing.lowercase()) {
                Log.d("BREAK_LOOP", "BREAK")
                break
            }
            messagesToSend.add(code["message"] as SmsMessage)
        }
    } else if(existing.isNullOrEmpty() && newTransactionCodes.isNotEmpty()) {
        messagesToSend.addAll(newTransactionCodes.map { it["message"] as SmsMessage })
        Log.d("NEW_TRANSACTIONS_CODE_SIZE", newTransactionCodes.size.toString())
        Log.d("MESSAGES_TO_SEND_SIZE", messagesToSend.size.toString())
    }
    if(messagesToSend.isNotEmpty()) {
        extractAndInsertTransactions(
            messages = messagesToSend,
            userAccount = userAccount,
            categories = categories,
            transactionsService = transactionService
        )
    }

    return messagesToSend;
}

fun extractAndInsertTransactions(messages: List<SmsMessage>, userAccount: UserAccount, categories: List<CategoryWithTransactions>, transactionsService: TransactionService) {
    Log.d("INSERTION", "Inserting ${messages.size} transactions")
    var count = 0

    for(message in messages) {
        count += 1
        try {
            transactionsService.extractTransactionDetails(message.toMessageData(), userAccount, categories.map { it.toTransactionCategory() })
        } catch (e: Exception) {
            Log.e("transactionInsertException", e.toString())
        }
    }
}

fun SmsMessage.toMessageData(): MessageData = MessageData(
    body = body,
    time = time,
    date = date
)


private fun getNewTransactionCodes(messages: List<SmsMessage>): List<Map<String, Any>> {
    Log.d("ANALYSIS", "ANALYZING ${messages.size} MESSAGES")
    val transactionCodes = mutableListOf<Map<String, Any>>()
    for(message in messages) {
        try {
            val transactionCodeMatcher = Regex("\\b\\w{10}\\b").find(message.body)
            if (transactionCodeMatcher != null) {
                val transactionCode = transactionCodeMatcher.value
                transactionCodes.add(mapOf("code" to transactionCode.trim().lowercase(), "message" to message))
            }
        } catch (e: Exception) {
            Log.e("Exception:", e.toString())
        }
    }
    return transactionCodes;
}