package com.records.pesa

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.records.pesa.container.AppContainer
import com.records.pesa.container.AppContainerImpl
import com.records.pesa.workers.BudgetCheckWorker
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