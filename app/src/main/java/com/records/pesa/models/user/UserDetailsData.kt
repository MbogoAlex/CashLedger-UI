package com.records.pesa.models.user

import com.records.pesa.models.SmsMessage
import com.records.pesa.models.TransactionItem
import com.records.pesa.models.payment.SubscriptionDetails
import kotlinx.serialization.Serializable

@Serializable
data class UserDetailsData(
    val id: Int,
    val fname: String?,
    val lname: String?,
    val email: String?,
    val phoneNumber: String,
    val messages: List<SmsMessage>,
    val transactions: List<TransactionItem>,
    val payments: List<SubscriptionDetails>
)
