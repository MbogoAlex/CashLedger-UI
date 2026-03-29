package com.records.pesa.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.records.pesa.db.models.AggregatedTransaction
import com.records.pesa.db.models.DeletedTransaction
import com.records.pesa.db.models.Transaction
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionCategoryCrossRef
import com.records.pesa.db.models.TransactionTypeData
import com.records.pesa.db.models.TransactionWithCategories
import com.records.pesa.models.TodayExpenditure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Locale

@Dao
interface TransactionsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: Transaction): Long
    fun insertTransactionBlocking(transaction: Transaction): Long {
        return runBlocking {
            insertTransaction(transaction)
        }
    }

    @Query("select * from `transaction` where id = :id")
    fun getTransactionById(id: Int): Flow<TransactionWithCategories>

    @Query("select * from `transaction` where id = :id")
    fun getStaticTransactionById(id: Int): Transaction

    @Query("select * from `transaction` where transactionCode = :code")
    fun getTransactionByCode(code: String): Flow<Transaction>

    @Query("select * from `transaction`")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("select * from `transaction` where entity = :entity")
    fun getTransactionByEntity(entity: String): Flow<List<Transaction>>

    @Query("select * from `transaction` where entity = :entity")
    fun getStaticTransactionByEntity(entity: String): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCategoryTransactionMapping(transactionCategoryCrossRef: TransactionCategoryCrossRef): Long

    fun insertCategoryTransactionMappingRunBlocking(transactionCategoryCrossRef: TransactionCategoryCrossRef): Long {
        return runBlocking {
            insertCategoryTransactionMappingRunBlocking(transactionCategoryCrossRef)
        }
    }

    @Query("select * from `transaction` where id = :transactionId")
    fun getTransactionWithCategories(transactionId: Int): Flow<TransactionWithCategories>

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("select transactionCode from `transaction` order by date desc, time desc limit 1")
    fun getLatestTransactionCode(): Flow<String?>

    @Query("select balance from `transaction` order by date desc, time desc limit 1")
    fun getCurrentBalance(): Flow<Double>

    @Query("select * from `transaction` order by date asc limit 1")
    fun getFirstTransaction(): Flow<Transaction>

    @Query("select * from `transaction` where entity = :entity")
    fun getTransactionsByEntity(entity: String): Flow<List<Transaction>>

    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN t.transactionAmount > 0 THEN t.transactionAmount ELSE 0 END), 0) AS totalIn,
            COALESCE(SUM(CASE WHEN t.transactionAmount < 0 THEN ABS(t.transactionAmount) ELSE 0 END), 0) AS totalOut
        FROM `transaction` t
        WHERE t.date = :date
    """)
    fun getTodayExpenditure(date: LocalDate): Flow<TodayExpenditure>

    fun createUserTransactionQuery(
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        latest: Boolean,
        moneyDirection: String?,
        startDate: LocalDate,
        endDate: LocalDate
    ): SupportSQLiteQuery {
        val orderClause = if (latest) "DESC" else "ASC"

        val query = StringBuilder().apply {
            append("SELECT distinct t.* FROM `transaction` t ")
            append("LEFT JOIN transactionCategoryCrossRef tc ON t.id = tc.transactionId ")
            append("LEFT JOIN budget b ON b.categoryId = tc.categoryId ")
            append("WHERE t.userId = ? ") // Assuming `userId` is a column in `transaction`
            append("AND (? IS NULL OR ? = '' OR LOWER(t.entity) = LOWER(?) OR LOWER(t.sender) LIKE '%' || ? || '%' OR LOWER(t.nickName) LIKE '%' || ? || '%' OR LOWER(t.recipient) LIKE '%' || ? || '%') ")
            append("AND (? IS NULL OR tc.categoryId = ?) ")
            append("AND (? IS NULL OR b.id = ?) ")
            append("AND (? IS NULL OR ? = '' OR LOWER(t.transactionType) LIKE '%' || ? || '%') ")
            append("AND (t.date BETWEEN ? AND ?) ")
            if (!moneyDirection.isNullOrEmpty()) {
                if (moneyDirection.equals("in", ignoreCase = true)) {
                    append("AND t.transactionAmount > 0 ")
                } else {
                    append("AND t.transactionAmount <= 0 ")
                }
            }
            append("ORDER BY t.date $orderClause, t.time $orderClause")
        }

        return SimpleSQLiteQuery(
            query.toString(),
            arrayOf(userId, entity, entity, entity, entity, entity, entity, categoryId, categoryId, budgetId, budgetId, transactionType, transactionType, transactionType, startDate.toString(), endDate.toString())
        )
    }

    @Query("delete from `transaction` where id = :id")
    suspend fun deleteTransaction(id: Int)

    @Insert
    suspend fun insertDeletedTransaction(deletedTransaction: DeletedTransaction)

    @Query("select * from deletedTransactions")
    fun getDeletedTransactionEntities(): List<DeletedTransaction>

    fun createUserTransactionQueryByMonthAndYear(
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        latest: Boolean,
        moneyDirection: String?,
        month: String, // The month name (e.g., "January")
        year: Int // The year (e.g., 2024)
    ): SupportSQLiteQuery {
        val orderClause = if (latest) "DESC" else "ASC"

        val query = StringBuilder().apply {
            append("SELECT distinct t.* FROM `transaction` t ")
            append("LEFT JOIN transactionCategoryCrossRef tc ON t.id = tc.transactionId ")
            append("LEFT JOIN budget b ON b.categoryId = tc.categoryId ")
            append("WHERE t.userId = ? ") // Assuming `userId` is a column in `transaction`
            append("AND (? IS NULL OR ? = '' OR LOWER(t.entity) = LOWER(?) OR LOWER(t.sender) LIKE '%' || ? || '%' OR LOWER(t.nickName) LIKE '%' || ? || '%' OR LOWER(t.recipient) LIKE '%' || ? || '%') ")
            append("AND (? IS NULL OR tc.categoryId = ?) ")
            append("AND (? IS NULL OR b.id = ?) ")
            append("AND (? IS NULL OR ? = '' OR LOWER(t.transactionType) LIKE '%' || ? || '%') ")
            append("AND (strftime('%m', t.date) = strftime('%m', ?) AND strftime('%Y', t.date) = ?) ")
            if (!moneyDirection.isNullOrEmpty()) {
                if (moneyDirection.equals("in", ignoreCase = true)) {
                    append("AND t.transactionAmount > 0 ")
                } else {
                    append("AND t.transactionAmount <= 0 ")
                }
            }
            append("ORDER BY t.date $orderClause, t.time $orderClause")
        }

        // Convert the month name to a zero-padded month number (e.g., "January" to "01")
        val monthNumber = SimpleDateFormat("MMMM", Locale.ENGLISH).parse(month)?.let {
            SimpleDateFormat("MM", Locale.ENGLISH).format(it)
        }

        return SimpleSQLiteQuery(
            query.toString(),
            arrayOf(
                userId, entity, entity, entity, entity, entity, entity, categoryId, categoryId, budgetId, budgetId, transactionType, transactionType, transactionType, "$year-$monthNumber-01", year.toString()
            )
        )
    }



    @RawQuery(observedEntities = [Transaction::class, TransactionCategory::class])
    fun getUserTransactions(query: SupportSQLiteQuery): Flow<List<TransactionWithCategories>>

    @RawQuery(observedEntities = [Transaction::class, TransactionCategory::class])
    fun getUserTransactionsFilteredByMonthAndYear(query: SupportSQLiteQuery): Flow<List<TransactionWithCategories>>

    fun createUserTransactionQueryForMultipleCategories(
        userId: Int,
        entity: String?,
        categoryIds: List<Int>?,
        budgetId: Int?,
        transactionType: String?,
        latest: Boolean,
        startDate: LocalDate,
        endDate: LocalDate
    ): SupportSQLiteQuery {
        val orderClause = if (latest) "DESC" else "ASC"

        val query = StringBuilder().apply {
            append("SELECT distinct t.* FROM `transaction` t ")
            append("LEFT JOIN transactionCategoryCrossRef tc ON t.id = tc.transactionId ")
            append("LEFT JOIN budget b ON b.categoryId = tc.categoryId ")
            append("WHERE t.userId = ? ") // Assuming `userId` is a column in `transaction`
            append("AND (? IS NULL OR (? = '' OR LOWER(t.entity) = LOWER(?) OR LOWER(t.sender) LIKE '%' || ? || '%' OR LOWER(t.nickName) LIKE '%' || ? || '%' OR LOWER(t.recipient) LIKE '%' || ? || '%')) ")

            if (!categoryIds.isNullOrEmpty()) {
                append("AND tc.categoryId IN (${categoryIds.joinToString(", ")}) ")
            }

            append("AND (? IS NULL OR b.id = ?) ")
            append("AND (? IS NULL OR LOWER(t.transactionType) = ?) ")
            append("AND (t.date >= ?) ")
            append("AND (t.date <= ?) ")
            append("ORDER BY t.date $orderClause, t.time $orderClause")
        }

        val argsList = mutableListOf<Any?>()
        argsList.add(userId)
        argsList.add(entity)
        argsList.add(entity)
        argsList.add(entity)
        argsList.add(entity)
        argsList.add(entity)
        argsList.add(entity)

        if (categoryIds != null && categoryIds.isNotEmpty()) {
            // No need to add category IDs to args list; they are embedded directly in the query.
        }

        argsList.add(budgetId)
        argsList.add(budgetId)
        argsList.add(transactionType)
        argsList.add(transactionType)
        argsList.add(startDate.toString())
        argsList.add(endDate.toString())

        return SimpleSQLiteQuery(query.toString(), argsList.toTypedArray())
    }

    @RawQuery(observedEntities = [Transaction::class, TransactionCategory::class])
    fun getUserTransactionsForMultipleCategories(query: SupportSQLiteQuery): Flow<List<TransactionWithCategories>>


    @RawQuery(observedEntities = [Transaction::class, TransactionCategory::class])
    fun getStaticUserTransactions(query: SupportSQLiteQuery): List<TransactionWithCategories>

    fun createSortedTransactionsQuery(
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        moneyIn: Boolean,
        orderByAmount: Boolean,
        ascendingOrder: Boolean,
        startDate: LocalDate,
        endDate: LocalDate
    ): SupportSQLiteQuery {
        val orderClause = if (ascendingOrder) "ASC" else "DESC"
        val amountClause = if (orderByAmount) "totalAmount" else "times"

        val query = StringBuilder().apply {
            append("""
            SELECT distinct 
                CASE 
                    WHEN ? THEN t.sender 
                    ELSE t.recipient 
                END AS entity,
                t.nickName, 
                t.transactionType, 
                COUNT(ABS(t.transactionAmount)) AS times, 
                SUM(ABS(t.transactionAmount)) AS totalAmount, 
                SUM(ABS(t.transactionCost)) AS totalCost 
            FROM `transaction` t
            LEFT JOIN transactionCategoryCrossRef tc ON t.id = tc.transactionId
            LEFT JOIN budget b ON tc.categoryId = b.categoryId
            WHERE 
                (? IS NULL OR 
                LOWER(t.entity) = LOWER(?) OR
                LOWER(CASE 
                    WHEN ? THEN t.sender 
                    ELSE t.recipient 
                END) LIKE '%' || LOWER(?) || '%' OR 
                LOWER(t.nickName) LIKE '%' || LOWER(?) || '%') AND
                (? IS NULL OR tc.categoryId = ?) AND
                (? IS NULL OR b.id = ?) AND
                (? IS NULL OR LOWER(t.transactionType) = LOWER(?)) AND
                t.date BETWEEN ? AND ? AND
                ((? AND t.transactionAmount > 0) OR 
                (NOT ? AND t.transactionAmount < 0))
            GROUP BY 
                CASE 
                    WHEN ? THEN t.sender 
                    ELSE t.recipient 
                END,
                t.nickName, 
                t.transactionType
            ORDER BY 
                CASE WHEN ? THEN totalAmount ELSE times END 
                COLLATE NOCASE 
                $orderClause
        """)
        }

        return SimpleSQLiteQuery(
            query.toString(),
            arrayOf(
                moneyIn, entity, moneyIn, entity, entity,
                categoryId, categoryId,
                budgetId, budgetId,
                transactionType, transactionType,
                startDate.toString(), endDate.toString(),
                moneyIn, moneyIn
            )
        )
    }

    @RawQuery(observedEntities = [Transaction::class, TransactionCategory::class])
    fun getSortedTransactions(query: SupportSQLiteQuery): Flow<List<AggregatedTransaction>>

    // ===== Time Period Queries =====
    
    /**
     * Get all transactions between two dates (inclusive)
     */
    @Query("""
        SELECT * FROM `transaction` 
        WHERE date >= :startDate AND date <= :endDate 
        ORDER BY date DESC, time DESC
    """)
    fun getTransactionsBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>>

    /**
     * Get distinct years that have at least one transaction
     * Returns years in descending order (most recent first)
     */
    @Query("""
        SELECT DISTINCT CAST(strftime('%Y', date) AS INTEGER) as year 
        FROM `transaction` 
        ORDER BY year DESC
    """)
    fun getDistinctYearsWithTransactions(): Flow<List<Int>>

    /**
     * Get total money IN for a specific date range
     */
    @Query("""
        SELECT COALESCE(SUM(transactionAmount), 0.0) 
        FROM `transaction` 
        WHERE date >= :startDate 
        AND date <= :endDate 
        AND transactionAmount > 0
    """)
    suspend fun getTotalInForPeriod(startDate: LocalDate, endDate: LocalDate): Double

    /**
     * Get total money OUT for a specific date range (returns positive value)
     */
    @Query("""
        SELECT COALESCE(SUM(ABS(transactionAmount)), 0.0) 
        FROM `transaction` 
        WHERE date >= :startDate 
        AND date <= :endDate 
        AND transactionAmount < 0
    """)
    suspend fun getTotalOutForPeriod(startDate: LocalDate, endDate: LocalDate): Double

    /**
     * Get transaction counts and totals grouped by transaction type for a date range
     * Used for transaction type breakdown
     */
    @Query("""
        SELECT 
            transactionType,
            COUNT(*) as count,
            SUM(ABS(transactionAmount)) as total
        FROM `transaction`
        WHERE date >= :startDate 
        AND date <= :endDate
        GROUP BY transactionType
        ORDER BY total DESC
    """)
    suspend fun getTransactionTypeBreakdown(startDate: LocalDate, endDate: LocalDate): List<TransactionTypeData>

    @Query("""
        SELECT COUNT(*) FROM `transaction`
    """)
    suspend fun getTotalTransactionCount(): Int

    @Query("""
        SELECT COUNT(*) FROM `transaction` t
        LEFT JOIN transactionCategoryCrossRef tc ON t.id = tc.transactionId
        WHERE tc.transactionId IS NULL
    """)
    suspend fun getUncategorizedTransactionCount(): Int

    @Query("""
        SELECT 
            t.entity,
            t.nickName,
            t.transactionType,
            COUNT(*) AS times,
            SUM(ABS(t.transactionAmount)) AS totalAmount,
            SUM(ABS(t.transactionCost)) AS totalCost
        FROM `transaction` t
        LEFT JOIN transactionCategoryCrossRef tc ON t.id = tc.transactionId
        WHERE tc.transactionId IS NULL
        GROUP BY t.entity, t.transactionType
        ORDER BY times DESC
        LIMIT 10
    """)
    suspend fun getTopUncategorizedEntities(): List<AggregatedTransaction>

    @Query("""
        SELECT COALESCE(SUM(ABS(t.transactionAmount)), 0.0)
        FROM `transaction` t
        INNER JOIN transactionCategoryCrossRef ref ON ref.transactionId = t.id
        WHERE ref.categoryId = :categoryId
          AND t.date >= :startDate
          AND t.date <= :endDate
          AND t.transactionAmount < 0
    """)
    fun getOutflowForCategory(
        categoryId: Int,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(t.transactionAmount), 0.0)
        FROM `transaction` t
        INNER JOIN transactionCategoryCrossRef ref ON ref.transactionId = t.id
        WHERE ref.categoryId = :categoryId
          AND t.date >= :startDate
          AND t.date <= :endDate
          AND t.transactionAmount > 0
    """)
    fun getInflowForCategory(
        categoryId: Int,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Double>

    @Query("""
        SELECT t.* FROM `transaction` t
        INNER JOIN transactionCategoryCrossRef ref ON ref.transactionId = t.id
        WHERE ref.categoryId = :categoryId
        ORDER BY t.date DESC
    """)
    fun getTransactionsForCategory(categoryId: Int): Flow<List<Transaction>>

    @Query("""
        SELECT COALESCE(ABS(SUM(t.transactionAmount)), 0.0)
        FROM `transaction` t
        INNER JOIN transactionCategoryCrossRef ref ON ref.transactionId = t.id
        INNER JOIN categoryKeyword ck ON ck.categoryId = :categoryId
        WHERE ref.categoryId = :categoryId
          AND t.date >= :startDate
          AND t.date <= :endDate
          AND t.transactionAmount < 0
          AND (t.sender LIKE '%' || ck.keyword || '%' OR t.recipient LIKE '%' || ck.keyword || '%')
          AND ck.keyword IN (:memberNames)
    """)
    suspend fun getOutflowForCategoryAndMembers(
        categoryId: Int,
        startDate: LocalDate,
        endDate: LocalDate,
        memberNames: List<String>
    ): Double

}