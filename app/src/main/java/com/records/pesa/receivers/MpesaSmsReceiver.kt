package com.records.pesa.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.records.pesa.CashLedger
import com.records.pesa.models.SmsMessage
import com.records.pesa.reusables.SmsProviders
import com.records.pesa.workers.BudgetRecalculationWorker
import com.records.pesa.workers.NotificationHelper
import com.records.pesa.workers.extractAndInsertTransactions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Listens for incoming SMS and immediately saves any M-PESA transaction to Room.
 * Works even when the app is killed — Android guarantees broadcast delivery
 * for statically registered receivers.
 */
class MpesaSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (parts.isNullOrEmpty()) return

        val sender = parts[0].originatingAddress ?: return
        val body = parts.joinToString("") { it.messageBody }

        if (!SmsProviders.isFinancialProvider(sender)) return

        Log.d("MpesaSmsReceiver", "Financial SMS from $sender")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext as? CashLedger ?: return@launch
                val container = appContext.container

                val users = container.dbRepository.getUsers().first()
                if (users.isEmpty()) return@launch
                val user = users[0]

                val userAccount = container.userAccountService
                    .getUserAccountByBackupId(user.backUpUserId).first()
                val categories = container.categoryService.getAllCategories().first()

                val now = Date()
                val smsMessage = SmsMessage(
                    body = body,
                    date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now),
                    time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now),
                    senderAddress = sender
                )

                // Skip if transaction code already exists
                val txCode = Regex("\\b\\w{10}\\b").find(body)?.value?.trim()?.lowercase()
                if (txCode != null) {
                    val latest = container.transactionService.getLatestTransactionCode().first()
                    if (latest?.lowercase() == txCode) {
                        Log.d("MpesaSmsReceiver", "Duplicate $txCode — skipping")
                        return@launch
                    }
                }

                extractAndInsertTransactions(
                    messages = listOf(smsMessage),
                    userAccount = userAccount,
                    categories = categories,
                    transactionsService = container.transactionService
                )

                // Notify with deep link to the saved transaction
                if (txCode != null) {
                    try {
                        val tx = container.transactionService.getTransactionByCode(txCode).first()
                        val amount = abs(tx.transactionAmount)
                        val dir = if (tx.transactionAmount > 0) "received from" else "sent to"
                        NotificationHelper.notifyTransaction(
                            context = context,
                            transactionId = tx.id,
                            title = "New M-PESA Transaction",
                            body = "KES ${String.format("%,.0f", amount)} $dir ${tx.entity}"
                        )
                    } catch (e: Exception) {
                        Log.e("MpesaSmsReceiver", "Notification fetch failed: $e")
                    }
                }

                // Trigger immediate budget recalculation
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "budget_recalc_on_sms",
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<BudgetRecalculationWorker>().build()
                )
                Log.d("MpesaSmsReceiver", "Done — budget recalc enqueued")
            } catch (e: Exception) {
                Log.e("MpesaSmsReceiver", "Error: $e")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
