package com.records.pesa.db

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.db.models.UserSession
import com.records.pesa.models.dbModel.AppLaunchStatus
import com.records.pesa.models.dbModel.UserDetails
import kotlinx.coroutines.flow.Flow

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

    suspend fun deleteCategory(id: Int)

    suspend fun deleteCategoryKeyword(id: Int)

    suspend fun deleteTransactionFromCategoryMapping(transactionId: Int)
    suspend fun deleteCategoryKeywordByCategoryId(categoryId: Int)
    suspend fun deleteFromCategoryKeywordByCategoryId(categoryId: Int)

    suspend fun deleteFromCategoryMappingByCategoryId(categoryId: Int)
    suspend fun deleteCategoryKeywordByKeywordId(keywordId: Int)
}

class DBRepositoryImpl(private val appDao: AppDao): DBRepository {
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

    override suspend fun deleteCategory(id: Int) = appDao.deleteCategory(id)

    override suspend fun deleteCategoryKeyword(id: Int) = appDao.deleteCategoryKeyword(id)

    override suspend fun deleteTransactionFromCategoryMapping(transactionId: Int) = appDao.deleteTransactionFromCategoryMapping(transactionId)
    override suspend fun deleteCategoryKeywordByCategoryId(categoryId: Int) = appDao.deleteCategoryKeywordByCategoryId(categoryId)
    override suspend fun deleteFromCategoryKeywordByCategoryId(categoryId: Int) = appDao.deleteCategoryKeywordByCategoryId(categoryId)
    override suspend fun deleteFromCategoryMappingByCategoryId(categoryId: Int) = appDao.deleteFromCategoryMappingByCategoryId(categoryId)
    override suspend fun deleteCategoryKeywordByKeywordId(keywordId: Int) = appDao.deleteCategoryKeywordByKeywordId(keywordId)

}