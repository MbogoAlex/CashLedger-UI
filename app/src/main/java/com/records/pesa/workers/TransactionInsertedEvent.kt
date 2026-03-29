package com.records.pesa.workers

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Singleton event bus: Worker emits a newly inserted transactionId here;
 * DashboardScreenViewModel collects it and triggers navigation.
 */
object TransactionInsertedEvent {
    private val _flow = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val flow: SharedFlow<Int> = _flow.asSharedFlow()

    fun emit(transactionId: Int) {
        _flow.tryEmit(transactionId)
    }
}
