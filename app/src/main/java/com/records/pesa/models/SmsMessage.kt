package com.records.pesa.models

import kotlinx.serialization.Serializable

@Serializable
data class SmsMessage(
    val body: String,
    val date: String,
    val time: String,
    val receivingPhoneNumber: String? = null,
    val carrierName: String? = null,
    val simSlotIndex: Int = -1,
    val subscriptionId: Int = -1,
    val senderAddress: String? = null
)

@Serializable
data class MessagesResponseBody(
    val statusCode: Int,
    val message: String,
    val data: MessagesDt
)

@Serializable
data class MessagesDt(
    val message: List<MessageData>
)

@Serializable
data class MessageData(
    var body: String,
    var date: String,
    var time: String,
    )
