package com.records.pesa.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.records.pesa.db.models.UserAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccount(userAccount: UserAccount)

    @Query("select * from userAccount where id = :id")
    fun getUserAccountById(id: Int): Flow<UserAccount>

    @Query("select * from userAccount where backupUserId = :id")
    fun getUserAccountByBackupId(id: Long): Flow<UserAccount>

    @Update
    suspend fun updateUser(userAccount: UserAccount)
}