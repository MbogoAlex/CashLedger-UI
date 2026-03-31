package com.records.pesa.workers

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
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
import com.records.pesa.reusables.SmsProviders
import com.records.pesa.service.transaction.TransactionService
import com.records.pesa.workers.NotificationHelper
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FetchMessagesWorker(
    private val context: Context,
    private val params: WorkerParameters,
): CoroutineWorker(context, params) {

    companion object {
        const val KEY_SMS_BODY   = "smsBody"
        const val KEY_SMS_SENDER = "smsSender"
    }

    override suspend fun doWork(): Result {

        try {
            val appContext = context.applicationContext as? CashLedger
                ?: run {
                    Log.e("CashLedger_SMS", "Worker: applicationContext is not CashLedger — retrying")
                    return Result.retry()
                }

            // Always use the live container (never re-init — it's already initialised by CashLedger.onCreate)
            val transactionService = appContext.container.transactionService
            val categoryService = appContext.container.categoryService
            val userAccountService = appContext.container.userAccountService
            val dbRepository = appContext.container.dbRepository

            // Read user from DB directly — never depend on inputData which can be lost
            val users = dbRepository.getUsers().first()
            if (users.isEmpty()) {
                Log.e("CashLedger_SMS", "Worker: no users in DB — retrying")
                return Result.retry()
            }
            val user = users[0]
            Log.d("CashLedger_SMS", "Worker: running for userId=${user.userId} backUpUserId=${user.backUpUserId}")

            val existingCodes = transactionService.getAllTransactionCodes().first().map { it.lowercase() }.toHashSet()
            Log.d("CashLedger_SMS", "Worker: ${existingCodes.size} codes already in DB")

            val userAccount = userAccountService.getUserAccount(userId = user.userId).first()
            val categories  = categoryService.getAllCategories().first()

            // Two paths:
            // 1. Real-time (from receiver): SMS body is in inputData — process it directly,
            //    no inbox read needed, no timing race with the Messages app.
            // 2. Manual sync (from Dashboard button): no inputData — do a full inbox scan.
            val inlineSmsBody   = inputData.getString(KEY_SMS_BODY)
            val inlineSmsSender = inputData.getString(KEY_SMS_SENDER) ?: ""

            val inserted: List<SmsMessage> = if (!inlineSmsBody.isNullOrBlank()) {
                Log.d("CashLedger_SMS", "Worker: processing inline SMS from broadcast")
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val now = Date()
                val sms = SmsMessage(
                    body = inlineSmsBody,
                    date = dateFormat.format(now),
                    time = timeFormat.format(now),
                    senderAddress = inlineSmsSender
                )
                filterMessagesToSend(
                    messages = listOf(sms),
                    transactionService = transactionService,
                    userAccount = userAccount,
                    categories = categories,
                    existingCodes = existingCodes
                )
            } else {
                Log.d("CashLedger_SMS", "Worker: no inline SMS — scanning full inbox")
                fetchSmsMessages(
                    context = context,
                    transactionService = transactionService,
                    userAccount = userAccount,
                    categories = categories,
                    existingCodes = existingCodes
                )
            }

            Log.d("CashLedger_SMS", "Worker: inserted ${inserted.size} new transaction(s)")

            // Fire a notification for each newly inserted transaction
            if (inserted.isNotEmpty()) {
                val codeRegex = Regex("\\b\\w{10}\\b")
                for (sms in inserted) {
                    try {
                        // Use original case — DB stores the code as-is from the SMS
                        val code = codeRegex.find(sms.body)?.value ?: continue
                        val tx = transactionService.getTransactionByCode(code).first()
                        val amount = String.format("%,.2f", Math.abs(tx.transactionAmount))
                        val body = if (tx.transactionAmount < 0) {
                            "Ksh $amount to ${tx.entity}"
                        } else {
                            "Ksh $amount from ${tx.entity}"
                        }
                        Log.d("CashLedger_SMS", "Worker: notifying — ${tx.transactionType} $body")
                        NotificationHelper.notifyTransaction(
                            context = context,
                            transactionId = tx.id,
                            title = "M-PESA ${tx.transactionType}",
                            body = body
                        )
                        TransactionInsertedEvent.emit(tx.id)
                    } catch (e: Exception) {
                        Log.w("CashLedger_SMS", "Worker: notification skipped: $e")
                    }
                }
            }

            // Trigger backup (cloud sync) only when network is available — separate concern
            val token = user.token
            if (token.isNotEmpty()) {
                val postMessagesRequest = OneTimeWorkRequestBuilder<BackupWorker>()
                    .setInputData(workDataOf("userId" to user.backUpUserId.toInt(), "token" to token))
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
                // Use enqueueUniqueWork so this merges with / replaces any pending change-triggered backup
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "change_triggered_backup",
                    ExistingWorkPolicy.REPLACE,
                    postMessagesRequest
                )
            }

            Log.d("CashLedger_SMS", "Worker: complete ✓")
            return Result.success()
        } catch (e: Exception) {
            Log.e("CashLedger_SMS", "Worker: doWork crashed — $e")
            return Result.retry()
        }


    }
}

fun fetchSmsMessages(context: Context, transactionService: TransactionService, userAccount: UserAccount, categories: List<CategoryWithTransactions>, existingCodes: HashSet<String>): List<SmsMessage> {
    val messages = mutableListOf<SmsMessage>()
    val uri = Uri.parse("content://sms/inbox")
    val projection = arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.ADDRESS)

    // Use comprehensive provider patterns for filtering
    val (whereClause, whereArgs) = SmsProviders.createSmsFilterQuery()
    val sortOrder = "${Telephony.Sms.DATE} DESC"

    val cursor = context.contentResolver.query(uri, projection, whereClause, whereArgs, sortOrder)

    cursor?.use {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (it.moveToNext()) {
            val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
            val dateMillis = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
            val senderAddress = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))

            val date = Date(dateMillis)
            val formattedDate = dateFormat.format(date)
            val formattedTime = timeFormat.format(date)

            // Verify this is a financial provider before adding
            if (SmsProviders.isFinancialProvider(senderAddress)) {
                messages.add(SmsMessage(body, formattedDate, formattedTime, senderAddress = senderAddress))
            }
        }
    }

    val messagesToSend = filterMessagesToSend(
        messages = messages,
        transactionService = transactionService,
        userAccount = userAccount,
        categories = categories,
        existingCodes = existingCodes
    )
    Log.d("MESSAGES_ADDITION", "ADDED ${messagesToSend.size} MESSAGES")
    return messagesToSend
}

fun filterMessagesToSend(messages: List<SmsMessage>, transactionService: TransactionService, userAccount: UserAccount, categories: List<CategoryWithTransactions>, existingCodes: HashSet<String>): List<SmsMessage> {
    val newTransactionCodes = getNewTransactionCodes(messages)
    Log.d("CashLedger_SMS", "filterMessagesToSend: ${messages.size} SMS parsed, ${newTransactionCodes.size} have codes, ${existingCodes.size} already in DB")

    // Keep only messages whose code is not already in the DB — O(1) per lookup
    val messagesToSend = newTransactionCodes
        .filter { it["code"] as String !in existingCodes }
        .map { it["message"] as SmsMessage }

    Log.d("CashLedger_SMS", "filterMessagesToSend: ${messagesToSend.size} new transaction(s) to insert")

    if (messagesToSend.isNotEmpty()) {
        extractAndInsertTransactions(
            messages = messagesToSend,
            userAccount = userAccount,
            categories = categories,
            transactionsService = transactionService
        )
    }

    return messagesToSend
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