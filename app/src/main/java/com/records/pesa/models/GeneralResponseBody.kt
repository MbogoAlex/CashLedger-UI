package com.records.pesa.models

import kotlinx.serialization.Serializable

@Serializable
data class GeneralResponseBody(
    val success: Boolean,
    val message: String
)
