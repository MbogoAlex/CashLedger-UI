package com.records.pesa.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.records.pesa.db.models.UserAccount
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.models.dbModel.AppLaunchStatus
import com.records.pesa.models.dbModel.UserDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserDetails)

    @Update
    suspend fun updateUser(user: UserDetails)

    @Query("delete from user where userId = :userId")
    suspend fun deleteUser(userId: Int)

    @Query("delete from user")
    suspend fun deleteAllFromUser()

    @Query("delete from transactionCategoryCrossRef")
    suspend fun deleteCategoryMappings()

    @Query("delete from categoryKeyword")
    suspend fun deleteCategoryKeywords()

    @Query("delete from transactionCategory")
    suspend fun deleteCategories()

    @Query("delete from `transaction`")
    suspend fun deleteTransactions()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'transaction'")
    suspend fun resetTransactionPK()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'transactionCategory'")
    suspend fun resetCategoryPK()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'categoryKeyword'")
    suspend fun resetCategoryKeywordPK()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'transactionCategoryCrossRef'")
    suspend fun resetCategoryMappingsPK()

    @Query("delete from `transaction` where id = :id")
    suspend fun deleteTransaction(id: Int)

    @Query("delete from transactionCategory where id = :id")
    suspend fun deleteCategory(id: Int)

    @Query("delete from categoryKeyword where id = :id")
    suspend fun deleteCategoryKeyword(id: Int)

    @Query("delete from categoryKeyword where categoryId = :categoryId")
    suspend fun deleteCategoryKeywordByCategoryId(categoryId: Int)

    @Query("delete from transactionCategoryCrossRef where transactionId = :transactionId")
    suspend fun deleteTransactionFromCategoryMapping(transactionId: Int)

    @Query("delete from transactionCategoryCrossRef where categoryId = :categoryId")
    suspend fun deleteFromCategoryMappingByCategoryId(categoryId: Int)

    @Query("delete from categoryKeyword where id = :keywordId")
    suspend fun deleteCategoryKeywordByKeywordId(keywordId: Int)


    @Query("select * from user where userId = :userId")
    fun getUser(userId: Int): Flow<UserDetails>

    @Query("select * from user")
    fun getUsers(): Flow<List<UserDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppLaunchStatus(appLaunchStatus: AppLaunchStatus)

    @Update
    suspend fun updateAppLaunchStatus(appLaunchStatus: AppLaunchStatus)

    @Query("select * from app_launch_state where id = :id")
    fun getAppLaunchStatus(id: Int): Flow<AppLaunchStatus>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccount(userAccount: UserAccount): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUserPreferences(userReferences: UserPreferences)

    @Update
    suspend fun updateUserPreferences(userReferences: UserPreferences)

    @Query("SELECT * FROM userPreferences LIMIT 1")
    fun getUserPreferences(): Flow<UserPreferences?>

    @Query("DELETE FROM userPreferences")
    suspend fun deleteUserPreferences();

    @Query("select * from userAccount where id = :id")
    fun getUserAccountById(id: Long): Flow<UserAccount>
}