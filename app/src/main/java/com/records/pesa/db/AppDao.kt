package com.records.pesa.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
}