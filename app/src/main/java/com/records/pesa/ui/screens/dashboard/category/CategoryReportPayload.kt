package com.records.pesa.ui.screens.dashboard.category

import kotlinx.serialization.Serializable

@Serializable
data class CategoryReportPayload(
    val userId: Int,
    val categoryIds: List<Int>,
    val reportType: String,
    val startDate: String,
    val lastDate: String
)
