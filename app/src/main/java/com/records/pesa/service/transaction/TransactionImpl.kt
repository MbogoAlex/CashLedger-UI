package com.records.pesa.service.transaction

import com.records.pesa.db.dao.CategoryDao
import com.records.pesa.db.dao.TransactionsDao
import com.records.pesa.db.models.Transaction
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionWithCategories
import com.records.pesa.db.models.UserAccount
import com.records.pesa.models.MessageData
import com.records.pesa.service.transaction.function.TransactionsExtraction
import kotlinx.coroutines.flow.Flow

class TransactionImpl(private val transactionsDao: TransactionsDao, private val categoryDao: CategoryDao): TransactionService {
    override fun extractTransactionDetails(
        messageDto: MessageData,
        userAccount: UserAccount,
        categories: List<TransactionCategory>,
    ): Transaction = TransactionsExtraction().extractTransactionDetails(
        messageDto,
        userAccount,
        transactionsDao,
        categories,
        categoryDao
    )

    override fun getTransactionsByEntity(entity: String): Flow<List<Transaction>> = transactionsDao.getTransactionByEntity(entity = entity)

    override fun getTransactionByCode(transactionCode: String): Flow<Transaction> = transactionsDao.getTransactionByCode(transactionCode)

    override fun getAllTransactions(): Flow<List<Transaction>> = transactionsDao.getAllTransactions()

    override fun getTransactionWithCategories(id: Int): Flow<TransactionWithCategories> = transactionsDao.getTransactionWithCategories(id)
}