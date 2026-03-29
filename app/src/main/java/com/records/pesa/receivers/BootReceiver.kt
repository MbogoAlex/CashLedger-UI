package com.records.pesa.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.records.pesa.workers.BudgetCheckWorker
import com.records.pesa.workers.BudgetRecalculationWorker
import com.records.pesa.workers.FetchMessagesWorker
import com.records.pesa.workers.SubscriptionExpiryWorker
import java.util.concurrent.TimeUnit

/**
 * Re-registers all periodic WorkManager jobs after device reboot.
 * Required because WorkManager periodic work does not survive a reboot
 * on some OEM devices (Samsung, Tecno, Xiaomi) without explicit rescheduling.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d("BootReceiver", "Device booted — re-registering background workers")

        val wm = WorkManager.getInstance(context)

        wm.enqueueUniquePeriodicWork(
            "fetch_and_backup_transactions_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<FetchMessagesWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
        )

        wm.enqueueUniquePeriodicWork(
            "budget_check",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BudgetCheckWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
        )

        wm.enqueueUniquePeriodicWork(
            "budget_recalc",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BudgetRecalculationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
        )

        wm.enqueueUniquePeriodicWork(
            "subscription_expiry_check",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SubscriptionExpiryWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
        )
    }
}
