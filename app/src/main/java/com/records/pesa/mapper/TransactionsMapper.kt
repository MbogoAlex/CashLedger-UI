package com.records.pesa.mapper

import com.records.pesa.db.models.AggregatedTransaction
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionWithCategories
import com.records.pesa.models.transaction.IndividualSortedTransactionItem
import com.records.pesa.models.transaction.ItemCategory
import com.records.pesa.models.transaction.TransactionItem

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