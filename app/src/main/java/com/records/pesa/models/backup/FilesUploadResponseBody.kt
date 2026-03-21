package com.records.pesa.models.backup

import kotlinx.serialization.Serializable

@Serializable
data class FilesUploadResponseBody(
    val success: Boolean,
    val message: String,
    val data: String
)
