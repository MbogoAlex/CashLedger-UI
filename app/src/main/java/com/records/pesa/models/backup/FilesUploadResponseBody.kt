package com.records.pesa.models.backup

import kotlinx.serialization.Serializable

@Serializable
data class FilesUploadResponseBody(
    val statusCode: Int,
    val message: String,
    val data: String
)
