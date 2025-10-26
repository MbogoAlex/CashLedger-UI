package com.records.pesa.mapper

import com.records.pesa.db.models.AggregatedTransaction
import com.records.pesa.db.models.Transaction
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionWithCategories
import com.records.pesa.models.transaction.IndividualSortedTransactionItem
import com.records.pesa.models.transaction.ItemCategory
import com.records.pesa.models.transaction.TransactionItem
import java.time.LocalDate
import java.time.LocalTime

fun TransactionWithCategories.toTransactionItem(): TransactionItem = TransactionItem(
    transactionId = transaction.id,
    transactionCode = transaction.transactionCode,
    transactionType = transaction.transactionType,
    transactionAmount = transaction.transactionAmount,
    transactionCost = transaction.transactionCost,
    date = transaction.date.toString(),
    time = transaction.time.toString(),
    sender = transaction.sender,
    nickName = transaction.nickName,
    recipient = transaction.recipient,
    entity = transaction.entity,
    balance = transaction.balance,
    comment = transaction.comment,
    categories = categories.map { it.toItemCategory() }
)

fun TransactionCategory.toItemCategory(): ItemCategory = ItemCategory(
    id = id,
    name = name
)

fun AggregatedTransaction.toIndividualSortedTransactionItem(): IndividualSortedTransactionItem = IndividualSortedTransactionItem(
    transactionType = transactionType,
    times = times,
    amount = totalAmount,
    nickName = nickName,
    name = entity,
    transactionCost = totalCost
)

fun TransactionItem.toTransaction(userId: Int): Transaction = Transaction(
    id = transactionId!!,
    transactionCode = transactionCode,
    transactionType = transactionType,
    transactionAmount = transactionAmount,
    transactionCost = transactionCost,
    date = LocalDate.parse(date),
    time = LocalTime.parse(time),
    sender = sender,
    recipient = recipient,
    nickName = nickName,
    comment = comment,
    balance = balance,
    entity = entity,
    userId = userId.toLong()
)

fun TransactionWithCategories.toTransaction(userId: Int): Transaction = Transaction(
    id = transaction.id,
    transactionCode = transaction.transactionCode,
    transactionType = transaction.transactionType,
    transactionAmount = transaction.transactionAmount,
    transactionCost = transaction.transactionCost,
    date = transaction.date,
    time = transaction.time,
    sender = transaction.sender,
    recipient = transaction.recipient,
    nickName = transaction.nickName,
    comment = transaction.comment,
    balance = transaction.balance,
    entity = transaction.entity,
    userId = userId.toLong()
)

fun Transaction.toTransactionItem(): TransactionItem = TransactionItem(
    transactionId = id,
    transactionCode = transactionCode,
    transactionType = transactionType,
    transactionAmount = transactionAmount,
    transactionCost = transactionCost,
    date = date.toString(),
    time = time.toString(),
    sender = sender,
    recipient = recipient,
    nickName = nickName,
    comment = comment,
    balance = balance,
    entity = entity,
    categories = emptyList()
)
