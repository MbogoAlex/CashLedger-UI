package com.records.pesa.models.supabase.mapper

import com.records.pesa.db.models.CategoryKeyword
import com.records.pesa.db.models.Transaction
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionCategoryCrossRef
import com.records.pesa.models.supabase.SupabaseCategoryKeyword
import com.records.pesa.models.supabase.SupabaseTransaction
import com.records.pesa.models.supabase.SupabaseTransactionCategory
import com.records.pesa.models.supabase.SupabaseTransactionCategoryCrossRef
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun Transaction.toSupabaseTransaction(): SupabaseTransaction = SupabaseTransaction (
    id = id,
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
    userId = userId
)

fun SupabaseTransaction.toTransaction(): Transaction = Transaction (
    id = id,
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
    userId = userId
)

fun List<Transaction>.toSupabaseTransactions(): List<SupabaseTransaction> = map { it.toSupabaseTransaction() }
fun List<SupabaseTransaction>.toTransactions(): List<Transaction> = map { it.toTransaction() }

fun TransactionCategory.toSupabaseTransactionCategory(userId: Int): SupabaseTransactionCategory = SupabaseTransactionCategory(
    id = id,
    name = name,
    contains = contains,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    updatedTimes = updatedTimes?.toInt(),
    userId = userId
)

fun SupabaseTransactionCategory.toTransactionCategory(): TransactionCategory = TransactionCategory(
    id = id,
    name = name,
    contains = contains,
    createdAt = LocalDateTime.parse(createdAt),
    updatedAt = LocalDateTime.parse(updatedAt),
    updatedTimes = updatedTimes?.toDouble()
)

fun List<TransactionCategory>.toSupabaseTransactionCategories(userId: Int): List<SupabaseTransactionCategory> = map { it.toSupabaseTransactionCategory(userId = userId) }

fun List<SupabaseTransactionCategory>.toTransactionCategories(): List<TransactionCategory> = map { it.toTransactionCategory() }

fun CategoryKeyword.toSupabaseCategoryKeyword(userId: Int): SupabaseCategoryKeyword = SupabaseCategoryKeyword(
    id = id,
    keyword = keyword,
    nickName = nickName,
    categoryId = categoryId,
    userId = userId
)

fun SupabaseCategoryKeyword.toCategoryKeyword(): CategoryKeyword = CategoryKeyword(
    id = id,
    keyword = keyword,
    nickName = nickName,
    categoryId = categoryId
)

fun List<CategoryKeyword>.toSupabaseCategoryKeywords(userId: Int): List<SupabaseCategoryKeyword> = map { it.toSupabaseCategoryKeyword(userId = userId) }

fun List<SupabaseCategoryKeyword>.toCategoryKeywords(): List<CategoryKeyword> = map { it.toCategoryKeyword() }

fun SupabaseTransactionCategoryCrossRef.toTransactionCategoryCrossRef(): TransactionCategoryCrossRef = TransactionCategoryCrossRef(
    id = id,
    transactionId = transactionId,
    categoryId = categoryId
)

fun TransactionCategoryCrossRef.toSupabaseTransactionCategoryCrossRef(userId: Int): SupabaseTransactionCategoryCrossRef = SupabaseTransactionCategoryCrossRef(
    id = id!!,
    transactionId = transactionId,
    categoryId = categoryId,
    userId = userId
)

fun List<SupabaseTransactionCategoryCrossRef>.toTransactionCategoryCrossRefs(): List<TransactionCategoryCrossRef> = map { it.toTransactionCategoryCrossRef() }

fun List<TransactionCategoryCrossRef>.toSupabaseTransactionCategoryCrossRefs(userId: Int): List<SupabaseTransactionCategoryCrossRef> = map { it.toSupabaseTransactionCategoryCrossRef(userId = userId) }


