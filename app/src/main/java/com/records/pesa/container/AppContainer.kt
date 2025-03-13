package com.records.pesa.container

import com.records.pesa.db.DBRepository
import com.records.pesa.network.ApiRepository
import com.records.pesa.service.category.CategoryService
import com.records.pesa.service.transaction.TransactionService
import com.records.pesa.service.userAccount.UserAccountService
import com.records.pesa.workers.WorkersRepository

interface AppContainer {
    val apiRepository: ApiRepository
    val workersRepository: WorkersRepository
    val dbRepository: DBRepository
    val transactionService: TransactionService
    val userAccountService: UserAccountService
    val categoryService: CategoryService
}
