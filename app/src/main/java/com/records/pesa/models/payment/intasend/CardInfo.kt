package com.records.pesa.models.payment.intasend

import kotlinx.serialization.Serializable

@Serializable
data class CardInfo(
    val bin_country: String?,
    val card_type: String?
)