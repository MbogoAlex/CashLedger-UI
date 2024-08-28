package com.records.pesa.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.records.pesa.db.models.Transaction
import com.records.pesa.db.models.TransactionCategoryCrossRef
import com.records.pesa.db.models.TransactionWithCategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking

@Dao
interface TransactionsDao {
    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long
    fun insertTransactionBlocking(transaction: Transaction): Long {
        return runBlocking {
            insertTransaction(transaction)
        }
    }

    @Query("select * from `transaction` where id = :id")
    fun getTransactionById(id: Int): Flow<Transaction>

    @Query("select * from `transaction` where id = :id")
    fun getStaticTransactionById(id: Int): Transaction

    @Query("select * from `transaction` where transactionCode = :code")
    fun getTransactionByCode(code: String): Flow<Transaction>

    @Query("select * from `transaction`")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("select * from `transaction` where entity = :entity")
    fun getTransactionByEntity(entity: String): Flow<List<Transaction>>

    @Insert
    fun insertCategoryTransactionMapping(transactionCategoryCrossRef: TransactionCategoryCrossRef): Long

    fun insertCategoryTransactionMappingRunBlocking(transactionCategoryCrossRef: TransactionCategoryCrossRef): Long {
        return runBlocking {
            insertCategoryTransactionMappingRunBlocking(transactionCategoryCrossRef)
        }
    }

    @Query("select * from `transaction` where id = :transactionId")
    fun getTransactionWithCategories(transactionId: Int): Flow<TransactionWithCategories>

}