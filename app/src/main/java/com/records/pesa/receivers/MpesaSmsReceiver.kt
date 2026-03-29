package com.records.pesa.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.records.pesa.workers.BudgetRecalculationWorker
import com.records.pesa.workers.FetchMessagesWorker

/**
 * Listens for incoming SMS and immediately hands off to WorkManager.
 *
 * The SMS body is extracted directly from the broadcast intent extras and passed
 * to FetchMessagesWorker as inputData — no inbox read needed, no timing race.
 * This is the same approach used by apps like Truecaller.
 *
 * The manual "Sync" button on the Dashboard triggers the same worker WITHOUT
 * inputData, causing it to fall back to a full inbox scan.
 */
class MpesaSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val sender = parts?.firstOrNull()?.originatingAddress ?: "unknown"
        // Multi-part SMS: concatenate all parts into one body
        val body = parts?.joinToString("") { it.messageBody } ?: ""

        Log.d("CashLedger_SMS", "SMS_RECEIVED from '$sender' — body length ${body.length}")

        WorkManager.getInstance(context)
            .beginUniqueWork(
                "mpesa_capture_chain",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<FetchMessagesWorker>()
                    .setInputData(workDataOf(
                        FetchMessagesWorker.KEY_SMS_BODY   to body,
                        FetchMessagesWorker.KEY_SMS_SENDER to sender
                    ))
                    .build()
            )
            .then(OneTimeWorkRequestBuilder<BudgetRecalculationWorker>().build())
            .enqueue()

        Log.d("CashLedger_SMS", "WorkManager chain enqueued with inline SMS body")
    }
}
