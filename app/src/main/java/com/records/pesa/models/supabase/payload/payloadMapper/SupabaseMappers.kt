package com.records.pesa.models.supabase.payload.payloadMapper

import com.records.pesa.db.models.CategoryKeyword
import com.records.pesa.db.models.Transaction
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionCategoryCrossRef
import com.records.pesa.models.supabase.SupabaseCategoryKeyword
import com.records.pesa.models.supabase.SupabaseTransaction
import com.records.pesa.models.supabase.SupabaseTransactionCategory
import com.records.pesa.models.supabase.SupabaseTransactionCategoryCrossRef
import com.records.pesa.models.supabase.payload.SupabaseCategoryKeywordPayload
import com.records.pesa.models.supabase.payload.SupabaseTransactionCategoryCrossRefPayload
import com.records.pesa.models.supabase.payload.SupabaseTransactionCategoryPayload
import com.records.pesa.models.supabase.payload.SupabaseTransactionPayload
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun Transaction.toSupabaseTransactionPayload(): SupabaseTransactionPayload = SupabaseTransactionPayload (
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


fun List<Transaction>.toSupabaseTransactionsPayload(): List<SupabaseTransactionPayload> = map { it.toSupabaseTransactionPayload() }

fun TransactionCategory.toSupabaseTransactionCategoryPayload(userId: Int): SupabaseTransactionCategoryPayload = SupabaseTransactionCategoryPayload(
    name = name,
    contains = contains,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    updatedTimes = updatedTimes?.toInt(),
    userId = userId
)


fun List<TransactionCategory>.toSupabaseTransactionCategories(userId: Int): List<SupabaseTransactionCategoryPayload> = map { it.toSupabaseTransactionCategoryPayload(userId = userId) }


fun CategoryKeyword.toSupabaseCategoryKeywordPayload(userId: Int): SupabaseCategoryKeywordPayload = SupabaseCategoryKeywordPayload(
    keyword = keyword,
    nickName = nickName,
    categoryId = categoryId,
    userId = userId
)


fun List<CategoryKeyword>.toSupabaseCategoryKeywordsPayload(userId: Int): List<SupabaseCategoryKeywordPayload> = map { it.toSupabaseCategoryKeywordPayload(userId = userId) }


fun TransactionCategoryCrossRef.toSupabaseTransactionCategoryCrossRefPayload(userId: Int): SupabaseTransactionCategoryCrossRefPayload = SupabaseTransactionCategoryCrossRefPayload(
    transactionId = transactionId,
    categoryId = categoryId,
    userId = userId
)


fun List<TransactionCategoryCrossRef>.toSupabaseTransactionCategoryCrossRefsPayload(userId: Int): List<SupabaseTransactionCategoryCrossRefPayload> =
    map { it.toSupabaseTransactionCategoryCrossRefPayload(userId = userId) }



