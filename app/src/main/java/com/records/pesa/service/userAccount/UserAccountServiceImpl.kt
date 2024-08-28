package com.records.pesa.service.userAccount

import com.records.pesa.db.dao.UserDao
import com.records.pesa.db.models.UserAccount
import kotlinx.coroutines.flow.Flow

class UserAccountServiceImpl(private val userDao: UserDao): UserAccountService {
    override suspend fun insertUserAccount(userAccount: UserAccount) = userDao.insertUserAccount(userAccount)

    override suspend fun updateUserAccount(userAccount: UserAccount) = userDao.updateUser(userAccount)

    override suspend fun getUserAccount(userId: Int): Flow<UserAccount> = userDao.getUserAccountById(userId)
}