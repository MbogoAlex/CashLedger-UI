package com.records.pesa.models.payment.intasend

import kotlinx.serialization.Serializable

@Serializable
data class IntasendPaymentStatusResponseBodyData(
    val invoice: InvoiceData,
    val meta: MetaData
)