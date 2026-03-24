package com.records.pesa

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.records.pesa.container.AppContainer
import com.records.pesa.container.AppContainerImpl
import com.records.pesa.workers.BudgetCheckWorker
import com.records.pesa.workers.BudgetRecalculationWorker
import com.records.pesa.workers.FetchMessagesWorker
import com.records.pesa.workers.NotificationHelper
import java.util.concurrent.TimeUnit

class CashLedger: Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl(this)

        NotificationHelper.createChannel(this)

        val budgetCheckRequest = PeriodicWorkRequestBuilder<BudgetCheckWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "budget_check",
            ExistingPeriodicWorkPolicy.KEEP,
            budgetCheckRequest
        )

        // Budget recalculation — runs every 15 min (WorkManager minimum) so
        // expenditure figures are always fresh before the user opens a budget screen
        val budgetRecalcRequest = PeriodicWorkRequestBuilder<BudgetRecalculationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "budget_recalc",
            ExistingPeriodicWorkPolicy.KEEP,
            budgetRecalcRequest
        )

        // Catchup SMS fetch — runs every 6 hours regardless of network.
        // No inputData dependency so it survives WorkManager DB resets on OEM devices.
        val smsFetchRequest = PeriodicWorkRequestBuilder<FetchMessagesWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "fetch_and_backup_transactions_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            smsFetchRequest
        )
    }

    companion object {
        private var instance: CashLedger? = null

        fun getInstance(): CashLedger {
            return instance!!
        }
    }

    init {
        instance = this
    }
}