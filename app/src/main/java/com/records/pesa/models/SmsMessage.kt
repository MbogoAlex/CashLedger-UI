package com.records.pesa.models

import kotlinx.serialization.Serializable

@Serializable
data class SmsMessage(
    val body: String,
    val date: String,
    val time: String
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
