package com.records.pesa.db

import com.records.pesa.models.dbModel.AppLaunchStatus
import com.records.pesa.models.dbModel.UserDetails
import kotlinx.coroutines.flow.Flow

interface DBRepository {
    suspend fun insertUser(user: UserDetails)
    suspend fun updateUser(user: UserDetails)
    fun getUser(userId: Int): Flow<UserDetails>
    suspend fun deleteUser(userId: Int)
    fun getUsers(): Flow<List<UserDetails>>
    suspend fun insertAppLaunchStatus(appLaunchStatus: AppLaunchStatus)
    suspend fun updateAppLaunchStatus(appLaunchStatus: AppLaunchStatus)
    fun getAppLaunchStatus(id: Int): Flow<AppLaunchStatus>
    suspend fun deleteAllFromUser()
}

class DBRepositoryImpl(private val appDao: AppDao): DBRepository {
    override suspend fun insertUser(user: UserDetails) = appDao.insertUser(user)

    override suspend fun updateUser(user: UserDetails) = appDao.updateUser(user)

    override fun getUser(userId: Int): Flow<UserDetails> = appDao.getUser(userId)
    override suspend fun deleteUser(userId: Int) = appDao.deleteUser(userId)

    override fun getUsers(): Flow<List<UserDetails>> = appDao.getUsers()

    override suspend fun insertAppLaunchStatus(appLaunchStatus: AppLaunchStatus) = appDao.insertAppLaunchStatus(appLaunchStatus)

    override suspend fun updateAppLaunchStatus(appLaunchStatus: AppLaunchStatus) = appDao.updateAppLaunchStatus(appLaunchStatus)

    override fun getAppLaunchStatus(id: Int): Flow<AppLaunchStatus> = appDao.getAppLaunchStatus(id)
    override suspend fun deleteAllFromUser() = appDao.deleteAllFromUser()

}