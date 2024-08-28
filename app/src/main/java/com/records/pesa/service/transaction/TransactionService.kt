package com.records.pesa.service.transaction

import com.records.pesa.db.dao.CategoryDao
import com.records.pesa.db.dao.TransactionsDao
import com.records.pesa.db.models.Transaction
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionWithCategories
import com.records.pesa.db.models.UserAccount
import com.records.pesa.models.MessageData
import kotlinx.coroutines.flow.Flow

interface TransactionService {
    fun extractTransactionDetails(messageDto: MessageData, userAccount: UserAccount, categories: List<TransactionCategory>): Transaction
    fun getTransactionsByEntity(entity: String): Flow<List<Transaction>>
    fun getTransactionByCode(transactionCode: String): Flow<Transaction>
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionWithCategories(id: Int): Flow<TransactionWithCategories>
}