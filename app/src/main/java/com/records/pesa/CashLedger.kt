package com.records.pesa

import android.app.Application
import com.records.pesa.container.AppContainer
import com.records.pesa.container.AppContainerImpl

class CashLedger: Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl(this)
    }
}