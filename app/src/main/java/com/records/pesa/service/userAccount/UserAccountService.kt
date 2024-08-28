package com.records.pesa.service.userAccount

import com.records.pesa.db.models.UserAccount
import kotlinx.coroutines.flow.Flow

interface UserAccountService {
    suspend fun insertUserAccount(userAccount: UserAccount)
    suspend fun updateUserAccount(userAccount: UserAccount)
    suspend fun getUserAccount(userId: Int): Flow<UserAccount>
}