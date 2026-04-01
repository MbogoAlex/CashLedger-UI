package com.records.pesa.db

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.records.pesa.db.dao.BudgetDao
import com.records.pesa.db.dao.BudgetMemberDao
import com.records.pesa.db.dao.BudgetCycleLogDao
import com.records.pesa.db.dao.BudgetRecalcLogDao
import com.records.pesa.db.dao.ManualBudgetTransactionDao
import com.records.pesa.db.dao.ManualCategoryMemberDao
import com.records.pesa.db.dao.ManualTransactionDao
import com.records.pesa.db.dao.ManualTransactionTypeDao
import com.records.pesa.db.dao.TransactionsDao
import com.records.pesa.db.models.AggregatedTransaction
import com.records.pesa.db.models.Budget
import com.records.pesa.db.models.BudgetCycleLog
import com.records.pesa.db.models.BudgetMember
import com.records.pesa.db.models.BudgetRecalcLog
import com.records.pesa.db.models.ManualBudgetTransaction
import com.records.pesa.db.models.ManualCategoryMember
import com.records.pesa.db.models.ManualTransaction
import com.records.pesa.db.models.ManualTransactionType
import com.records.pesa.db.models.Transaction
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionTypeData
import com.records.pesa.db.models.DeletedCrossRef
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.db.models.UserSession
import com.records.pesa.db.models.ChatMessage
import com.records.pesa.db.models.CategoryKeyword
import com.records.pesa.models.dbModel.AppLaunchStatus
import com.records.pesa.models.dbModel.UserDetails
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

interface DBRepository {
    suspend fun insertUser(user: UserDetails)
    suspend fun updateUser(user: UserDetails)
    fun getUser(): Flow<UserDetails?>?
    suspend fun deleteUser(userId: Int)
    fun getUsers(): Flow<List<UserDetails>>

    suspend fun insertSession(userSession: UserSession)

    suspend fun updateSession(userSession: UserSession)

    fun getSession(): Flow<UserSession?>?

    suspend fun deleteSessions();
    suspend fun insertAppLaunchStatus(appLaunchStatus: AppLaunchStatus)
    suspend fun updateAppLaunchStatus(appLaunchStatus: AppLaunchStatus)
    fun getAppLaunchStatus(id: Int): Flow<AppLaunchStatus?>?
    suspend fun deleteAllFromUser()
    suspend fun deleteUserPreferences();

    suspend fun insertUserPreferences(userReferences: UserPreferences)

    suspend fun updateUserPreferences(userReferences: UserPreferences)

    fun getUserPreferences(): Flow<UserPreferences?>?

    suspend fun deleteCategoryMappings()


    suspend fun deleteCategoryKeywords()


    suspend fun deleteCategories()

    suspend fun deleteTransactions()

    suspend fun resetTransactionPK()


    suspend fun resetCategoryPK()

    suspend fun resetCategoryKeywordPK()

    suspend fun resetCategoryMappingsPK()

    suspend fun deleteTransaction(id: Int)

    suspend fun deleteCategory(id: Int, deletedAt: LocalDateTime = LocalDateTime.now())
    // One-shot lookup for restore merge (includes soft-deleted rows)
    suspend fun getCategoryByIdOnce(id: Int): TransactionCategory?

    suspend fun deleteCategoryKeyword(id: Int)

    suspend fun deleteTransactionFromCategoryMapping(transactionId: Int)
    suspend fun deleteCategoryKeywordByCategoryId(categoryId: Int)
    suspend fun deleteFromCategoryKeywordByCategoryId(categoryId: Int)

    suspend fun deleteFromCategoryMappingByCategoryId(categoryId: Int)
    suspend fun deleteCategoryKeywordByKeywordId(keywordId: Int)
    suspend fun deleteTransactionFromSpecificCategory(categoryId: Int, transactionId: Int)
    suspend fun insertDeletedCrossRef(categoryId: Int, transactionId: Int)
    suspend fun isDeletedCrossRef(transactionId: Int, categoryId: Int): Boolean
    
    // Time Period Selector methods
    fun getDistinctYearsWithTransactions(): Flow<List<Int>>    fun getTransactionsBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>>    suspend fun getTotalInForPeriod(startDate: LocalDate, endDate: LocalDate): Double
    suspend fun getTotalOutForPeriod(startDate: LocalDate, endDate: LocalDate): Double
    suspend fun getTransactionTypeBreakdown(startDate: LocalDate, endDate: LocalDate): List<TransactionTypeData>
    suspend fun getTotalTransactionCount(): Int
    suspend fun getUncategorizedTransactionCount(): Int
    suspend fun getTopUncategorizedEntities(): List<AggregatedTransaction>

    // Budget operations
    suspend fun insertBudget(budget: Budget): Long
    suspend fun updateBudget(budget: Budget)
    suspend fun updateBudgetExpenditure(id: Int, expenditure: Double, limitReached: Boolean, exceededBy: Double)
    suspend fun deleteBudget(budget: Budget)
    suspend fun deleteBudgetById(id: Int, deletedAt: LocalDateTime = LocalDateTime.now())
    fun getBudgetById(id: Int): Flow<Budget?>
    suspend fun getBudgetByIdOnce(id: Int): Budget?
    fun getBudgetsByCategoryId(categoryId: Int): Flow<List<Budget>>
    fun getActiveBudgets(): Flow<List<Budget>>
    fun getAllBudgets(): Flow<List<Budget>>

    // Local spending computation
    fun getOutflowForCategory(categoryId: Int, startDate: LocalDate, endDate: LocalDate): Flow<Double>
    fun getInflowForCategory(categoryId: Int, startDate: LocalDate, endDate: LocalDate): Flow<Double>

    // Budget audit log
    suspend fun insertBudgetRecalcLog(log: BudgetRecalcLog)
    fun getLogsForBudget(budgetId: Int): Flow<List<BudgetRecalcLog>>
    suspend fun getAllBudgetRecalcLogsOnce(): List<BudgetRecalcLog>

    // Budget cycle history (completed recurring cycles)
    suspend fun insertBudgetCycleLog(log: BudgetCycleLog)
    fun getBudgetCycleLogs(budgetId: Int): Flow<List<BudgetCycleLog>>
    suspend fun getBudgetCycleLogsOnce(budgetId: Int): List<BudgetCycleLog>
    suspend fun deleteBudgetCycleLogs(budgetId: Int)
    suspend fun getAllBudgetCycleLogsOnce(): List<BudgetCycleLog>

    // Manual budget transactions
    suspend fun insertManualTransaction(tx: ManualBudgetTransaction): Long
    fun getManualTransactionsForBudget(budgetId: Int): Flow<List<ManualBudgetTransaction>>
    suspend fun sumManualTransactionsForBudget(budgetId: Int): Double
    suspend fun deleteManualTransaction(id: Int)
    suspend fun getAllManualBudgetTransactionsOnce(): List<ManualBudgetTransaction>

    // Transaction types
    fun getAllManualTransactionTypes(): Flow<List<ManualTransactionType>>
    suspend fun getAllManualTransactionTypesOnce(): List<ManualTransactionType>
    suspend fun insertManualTransactionType(type: ManualTransactionType): Long
    suspend fun deleteCustomManualTransactionType(id: Int)

    // Category members
    fun getManualMembersForCategory(categoryId: Int): Flow<List<ManualCategoryMember>>
    suspend fun getAllManualCategoryMembersOnce(): List<ManualCategoryMember>
    suspend fun insertManualCategoryMember(member: ManualCategoryMember): Long
    suspend fun deleteManualCategoryMember(id: Int, deletedAt: LocalDateTime = LocalDateTime.now())
    suspend fun updateManualCategoryMemberName(id: Int, newName: String)

    // Manual transactions (category-scoped)
    fun getManualTransactionsForCategory(categoryId: Int): Flow<List<ManualTransaction>>
    suspend fun getManualTransactionsForCategoryOnce(categoryId: Int): List<ManualTransaction>
    fun getManualTransactionById(id: Int): Flow<ManualTransaction>
    suspend fun insertManualCategoryTransaction(tx: ManualTransaction): Long
    suspend fun deleteManualCategoryTransaction(id: Int, deletedAt: LocalDateTime = LocalDateTime.now())
    suspend fun updateManualCategoryTransaction(tx: ManualTransaction)
    suspend fun deleteManualTransactionsByMember(categoryId: Int, memberName: String)
    suspend fun updateManualTransactionMemberName(categoryId: Int, oldName: String, newName: String)
    suspend fun sumManualOutflowForCategory(categoryId: Int): Double
    suspend fun sumManualOutflowForCategoryInPeriod(categoryId: Int, startDate: java.time.LocalDate, endDate: java.time.LocalDate): Double
    suspend fun getAllManualTransactionsOnce(): List<ManualTransaction>

    // Transactions for category (M-PESA)
    fun getTransactionsForCategory(categoryId: Int): Flow<List<Transaction>>

    // Budget members
    fun getBudgetMembers(budgetId: Int): Flow<List<BudgetMember>>
    suspend fun getBudgetMembersOnce(budgetId: Int): List<BudgetMember>
    suspend fun insertBudgetMembers(members: List<BudgetMember>)
    suspend fun deleteBudgetMembers(budgetId: Int)
    suspend fun getAllBudgetMembersOnce(): List<BudgetMember>
    suspend fun getOutflowForCategoryAndMembers(categoryId: Int, startDate: LocalDate, endDate: LocalDate, memberNames: List<String>): Double
    suspend fun sumManualOutflowForCategoryAndMembers(categoryId: Int, startDate: LocalDate, endDate: LocalDate, memberNames: List<String>): Double

    // Chat message operations
    suspend fun insertChatMessage(message: ChatMessage): Long
    fun getMessagesForUser(userId: Int): Flow<List<ChatMessage>>
    suspend fun getMessagesForUserOnce(userId: Int): List<ChatMessage>
    suspend fun clearChatForUser(userId: Int)
    suspend fun deleteChatMessage(id: Int)
    suspend fun updateChatConsentGiven(value: Boolean)

    // AI context data
    suspend fun getAllTransactionsOnce(): List<Transaction>
    suspend fun getAllCategoriesOnce(): List<TransactionCategory>
    suspend fun getKeywordsForCategoryOnce(categoryId: Int): List<CategoryKeyword>
    suspend fun getAllBudgetsOnce(): List<Budget>
}

class DBRepositoryImpl(
    private val appDao: AppDao,
    private val transactionsDao: TransactionsDao,
    private val budgetDao: BudgetDao,
    private val budgetRecalcLogDao: BudgetRecalcLogDao,
    private val budgetCycleLogDao: BudgetCycleLogDao,
    private val manualBudgetTransactionDao: ManualBudgetTransactionDao,
    private val manualTransactionTypeDao: ManualTransactionTypeDao,
    private val manualCategoryMemberDao: ManualCategoryMemberDao,
    private val manualTransactionDao: ManualTransactionDao,
    private val budgetMemberDao: BudgetMemberDao
): DBRepository {
    override suspend fun insertUser(user: UserDetails) = appDao.insertUser(user)

    override suspend fun updateUser(user: UserDetails) = appDao.updateUser(user)

    override fun getUser(): Flow<UserDetails?>? {
        return try {
            appDao.getUser()
        } catch (e: Exception) {
            null
        }
    }
    override suspend fun deleteUser(userId: Int) = appDao.deleteUser(userId)

    override fun getUsers(): Flow<List<UserDetails>> = appDao.getUsers()
    override suspend fun insertSession(userSession: UserSession) =
        appDao.insertSession(userSession = userSession)

    override suspend fun updateSession(userSession: UserSession) =
        appDao.updateSession(
            userSession = userSession
        )

    override fun getSession(): Flow<UserSession?>? {
        return try {
            appDao.getSession()
        } catch (e: Exception) {
            null
        }
    }


    override suspend fun deleteSessions() =
        appDao.deleteSessions()

    override suspend fun insertAppLaunchStatus(appLaunchStatus: AppLaunchStatus) = appDao.insertAppLaunchStatus(appLaunchStatus)

    override suspend fun updateAppLaunchStatus(appLaunchStatus: AppLaunchStatus) = appDao.updateAppLaunchStatus(appLaunchStatus)

    override fun getAppLaunchStatus(id: Int): Flow<AppLaunchStatus?>? {
        return try {
            appDao.getAppLaunchStatus(id)
        } catch (e: Exception) {
            null
        }
    }
    override suspend fun deleteAllFromUser() = appDao.deleteAllFromUser()
    override suspend fun deleteUserPreferences() = appDao.deleteUserPreferences()

    override suspend fun insertUserPreferences(userReferences: UserPreferences) = appDao.insertUserPreferences(userReferences)

    override suspend fun updateUserPreferences(userReferences: UserPreferences) = appDao.updateUserPreferences(userReferences)

    override fun getUserPreferences(): Flow<UserPreferences?>? {
        return try {
            appDao.getUserPreferences()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun deleteCategoryMappings() = appDao.deleteCategoryMappings()

    override suspend fun deleteCategoryKeywords() = appDao.deleteCategoryKeywords()

    override suspend fun deleteCategories() = appDao.deleteCategories()

    override suspend fun deleteTransactions() = appDao.deleteTransactions()
    override suspend fun resetTransactionPK()  = appDao.resetTransactionPK()

    override suspend fun resetCategoryPK() = appDao.resetCategoryPK()

    override suspend fun resetCategoryKeywordPK() = appDao.resetCategoryKeywordPK()

    override suspend fun resetCategoryMappingsPK() = appDao.resetCategoryMappingsPK()
    override suspend fun deleteTransaction(id: Int) = appDao.deleteTransaction(id)

    override suspend fun deleteCategory(id: Int, deletedAt: LocalDateTime) = appDao.deleteCategory(id, deletedAt)
    override suspend fun getCategoryByIdOnce(id: Int): TransactionCategory? = appDao.getCategoryByIdOnce(id)

    override suspend fun deleteCategoryKeyword(id: Int) = appDao.deleteCategoryKeyword(id)

    override suspend fun deleteTransactionFromCategoryMapping(transactionId: Int) = appDao.deleteTransactionFromCategoryMapping(transactionId)
    override suspend fun deleteCategoryKeywordByCategoryId(categoryId: Int) = appDao.deleteCategoryKeywordByCategoryId(categoryId)
    override suspend fun deleteFromCategoryKeywordByCategoryId(categoryId: Int) = appDao.deleteCategoryKeywordByCategoryId(categoryId)
    override suspend fun deleteFromCategoryMappingByCategoryId(categoryId: Int) = appDao.deleteFromCategoryMappingByCategoryId(categoryId)
    override suspend fun deleteCategoryKeywordByKeywordId(keywordId: Int) = appDao.deleteCategoryKeywordByKeywordId(keywordId)
    override suspend fun deleteTransactionFromSpecificCategory(categoryId: Int, transactionId: Int) =
        appDao.deleteTransactionFromSpecificCategory(categoryId, transactionId)
    override suspend fun insertDeletedCrossRef(categoryId: Int, transactionId: Int) =
        appDao.insertDeletedCrossRef(DeletedCrossRef(transactionId = transactionId, categoryId = categoryId))
    override suspend fun isDeletedCrossRef(transactionId: Int, categoryId: Int): Boolean =
        appDao.isDeletedCrossRef(transactionId, categoryId)

    // Time Period Selector implementations
    override fun getDistinctYearsWithTransactions(): Flow<List<Int>> = transactionsDao.getDistinctYearsWithTransactions()
    override fun getTransactionsBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>> = transactionsDao.getTransactionsBetweenDates(startDate, endDate)
    override suspend fun getTotalInForPeriod(startDate: LocalDate, endDate: LocalDate): Double = transactionsDao.getTotalInForPeriod(startDate, endDate)
    override suspend fun getTotalOutForPeriod(startDate: LocalDate, endDate: LocalDate): Double = transactionsDao.getTotalOutForPeriod(startDate, endDate)
    override suspend fun getTransactionTypeBreakdown(startDate: LocalDate, endDate: LocalDate): List<TransactionTypeData> = transactionsDao.getTransactionTypeBreakdown(startDate, endDate)
    override suspend fun getTotalTransactionCount(): Int = transactionsDao.getTotalTransactionCount()
    override suspend fun getUncategorizedTransactionCount(): Int = transactionsDao.getUncategorizedTransactionCount()
    override suspend fun getTopUncategorizedEntities(): List<AggregatedTransaction> = transactionsDao.getTopUncategorizedEntities()

    // Budget operations
    override suspend fun insertBudget(budget: Budget): Long = budgetDao.insertBudget(budget)
    override suspend fun updateBudget(budget: Budget) = budgetDao.updateBudget(budget)
    override suspend fun updateBudgetExpenditure(id: Int, expenditure: Double, limitReached: Boolean, exceededBy: Double) =
        budgetDao.updateBudgetExpenditure(id, expenditure, limitReached, exceededBy)
    override suspend fun deleteBudget(budget: Budget) = budgetDao.softDeleteBudget(budget.id, LocalDateTime.now())
    override suspend fun deleteBudgetById(id: Int, deletedAt: LocalDateTime) = budgetDao.deleteBudgetById(id, deletedAt)
    override fun getBudgetById(id: Int): Flow<Budget?> = budgetDao.getBudgetById(id)
    override suspend fun getBudgetByIdOnce(id: Int): Budget? = budgetDao.getBudgetByIdOnce(id)
    override fun getBudgetsByCategoryId(categoryId: Int): Flow<List<Budget>> = budgetDao.getBudgetsByCategoryId(categoryId)
    override fun getActiveBudgets(): Flow<List<Budget>> = budgetDao.getActiveBudgets()
    override fun getAllBudgets(): Flow<List<Budget>> = budgetDao.getAllBudgets()

    // Local spending computation
    override fun getOutflowForCategory(categoryId: Int, startDate: LocalDate, endDate: LocalDate): Flow<Double> =
        transactionsDao.getOutflowForCategory(categoryId, startDate, endDate)
    override fun getInflowForCategory(categoryId: Int, startDate: LocalDate, endDate: LocalDate): Flow<Double> =
        transactionsDao.getInflowForCategory(categoryId, startDate, endDate)

    override suspend fun insertBudgetRecalcLog(log: BudgetRecalcLog) = budgetRecalcLogDao.insertLog(log)
    override fun getLogsForBudget(budgetId: Int) = budgetRecalcLogDao.getLogsForBudget(budgetId)
    override suspend fun getAllBudgetRecalcLogsOnce(): List<BudgetRecalcLog> = budgetRecalcLogDao.getAllLogsOnce()

    override suspend fun insertBudgetCycleLog(log: BudgetCycleLog) = budgetCycleLogDao.insertLog(log)
    override fun getBudgetCycleLogs(budgetId: Int) = budgetCycleLogDao.getLogsForBudget(budgetId)
    override suspend fun getBudgetCycleLogsOnce(budgetId: Int) = budgetCycleLogDao.getLogsForBudgetOnce(budgetId)
    override suspend fun deleteBudgetCycleLogs(budgetId: Int) = budgetCycleLogDao.deleteLogsForBudget(budgetId)
    override suspend fun getAllBudgetCycleLogsOnce() = budgetCycleLogDao.getAllOnce()

    override suspend fun insertManualTransaction(tx: ManualBudgetTransaction) = manualBudgetTransactionDao.insert(tx)
    override fun getManualTransactionsForBudget(budgetId: Int) = manualBudgetTransactionDao.getForBudget(budgetId)
    override suspend fun sumManualTransactionsForBudget(budgetId: Int) = manualBudgetTransactionDao.sumForBudget(budgetId)
    override suspend fun deleteManualTransaction(id: Int) = manualBudgetTransactionDao.deleteById(id)
    override suspend fun getAllManualBudgetTransactionsOnce(): List<ManualBudgetTransaction> = manualBudgetTransactionDao.getAllOnce()

    override fun getAllManualTransactionTypes(): Flow<List<ManualTransactionType>> = manualTransactionTypeDao.getAll()
    override suspend fun getAllManualTransactionTypesOnce(): List<ManualTransactionType> = manualTransactionTypeDao.getAllOnce()
    override suspend fun insertManualTransactionType(type: ManualTransactionType): Long = manualTransactionTypeDao.insert(type)
    override suspend fun deleteCustomManualTransactionType(id: Int) = manualTransactionTypeDao.deleteCustomById(id)

    override fun getManualMembersForCategory(categoryId: Int): Flow<List<ManualCategoryMember>> = manualCategoryMemberDao.getForCategory(categoryId)
    override suspend fun getAllManualCategoryMembersOnce(): List<ManualCategoryMember> = manualCategoryMemberDao.getAllOnce()
    override suspend fun insertManualCategoryMember(member: ManualCategoryMember): Long = manualCategoryMemberDao.insert(member)
    override suspend fun deleteManualCategoryMember(id: Int, deletedAt: LocalDateTime) = manualCategoryMemberDao.deleteById(id, deletedAt)
    override suspend fun updateManualCategoryMemberName(id: Int, newName: String) = manualCategoryMemberDao.updateName(id, newName)

    override fun getManualTransactionsForCategory(categoryId: Int): Flow<List<ManualTransaction>> = manualTransactionDao.getForCategory(categoryId)
    override suspend fun getManualTransactionsForCategoryOnce(categoryId: Int): List<ManualTransaction> = manualTransactionDao.getForCategoryOnce(categoryId)
    override fun getManualTransactionById(id: Int): Flow<ManualTransaction> = manualTransactionDao.getById(id)
    override suspend fun insertManualCategoryTransaction(tx: ManualTransaction): Long = manualTransactionDao.insert(tx)
    override suspend fun deleteManualCategoryTransaction(id: Int, deletedAt: LocalDateTime) = manualTransactionDao.deleteById(id, deletedAt)
    override suspend fun updateManualCategoryTransaction(tx: ManualTransaction) = manualTransactionDao.update(tx)
    override suspend fun deleteManualTransactionsByMember(categoryId: Int, memberName: String) = manualTransactionDao.deleteByMemberAndCategory(categoryId, memberName)
    override suspend fun updateManualTransactionMemberName(categoryId: Int, oldName: String, newName: String) = manualTransactionDao.updateMemberName(categoryId, oldName, newName)
    override suspend fun sumManualOutflowForCategory(categoryId: Int): Double = manualTransactionDao.sumOutflowForCategory(categoryId)
    override suspend fun sumManualOutflowForCategoryInPeriod(categoryId: Int, startDate: java.time.LocalDate, endDate: java.time.LocalDate): Double =
        manualTransactionDao.sumOutflowForCategoryInPeriod(categoryId, startDate, endDate)
    override suspend fun getAllManualTransactionsOnce(): List<ManualTransaction> = manualTransactionDao.getAllOnce()

    override fun getTransactionsForCategory(categoryId: Int): Flow<List<Transaction>> = transactionsDao.getTransactionsForCategory(categoryId)

    override fun getBudgetMembers(budgetId: Int): Flow<List<BudgetMember>> = budgetMemberDao.getForBudget(budgetId)
    override suspend fun getBudgetMembersOnce(budgetId: Int): List<BudgetMember> = budgetMemberDao.getForBudgetOnce(budgetId)
    override suspend fun insertBudgetMembers(members: List<BudgetMember>) = budgetMemberDao.insertAll(members)
    override suspend fun deleteBudgetMembers(budgetId: Int) = budgetMemberDao.deleteByBudget(budgetId)
    override suspend fun getAllBudgetMembersOnce(): List<BudgetMember> = budgetMemberDao.getAllOnce()
    override suspend fun getOutflowForCategoryAndMembers(categoryId: Int, startDate: LocalDate, endDate: LocalDate, memberNames: List<String>): Double =
        transactionsDao.getOutflowForCategoryAndMembers(categoryId, startDate, endDate, memberNames)
    override suspend fun sumManualOutflowForCategoryAndMembers(categoryId: Int, startDate: LocalDate, endDate: LocalDate, memberNames: List<String>): Double =
        manualTransactionDao.sumOutflowForCategoryAndMembers(categoryId, startDate, endDate, memberNames)

    // Chat message operations
    override suspend fun insertChatMessage(message: ChatMessage): Long = appDao.insertChatMessage(message)
    override fun getMessagesForUser(userId: Int): Flow<List<ChatMessage>> = appDao.getMessagesForUser(userId)
    override suspend fun getMessagesForUserOnce(userId: Int): List<ChatMessage> = appDao.getMessagesForUserOnce(userId)
    override suspend fun clearChatForUser(userId: Int) = appDao.clearChatForUser(userId)
    override suspend fun deleteChatMessage(id: Int) = appDao.deleteChatMessage(id)
    override suspend fun updateChatConsentGiven(value: Boolean) = appDao.updateChatConsentGiven(value)

    // AI context data
    override suspend fun getAllTransactionsOnce(): List<Transaction> = transactionsDao.getAllTransactionsOnce()
    override suspend fun getAllCategoriesOnce(): List<TransactionCategory> = appDao.getAllCategoriesOnce()
    override suspend fun getKeywordsForCategoryOnce(categoryId: Int): List<CategoryKeyword> = appDao.getKeywordsForCategoryOnce(categoryId)
    override suspend fun getAllBudgetsOnce(): List<Budget> = budgetDao.getAllBudgetsOnce()

}