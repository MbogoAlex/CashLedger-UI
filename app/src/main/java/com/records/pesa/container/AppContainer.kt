package com.records.pesa.container

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.records.pesa.db.AppDatabase
import com.records.pesa.db.DBRepository
import com.records.pesa.db.DBRepositoryImpl
import com.records.pesa.network.ApiRepository
import com.records.pesa.network.ApiRepositoryImpl
import com.records.pesa.network.ApiService
import com.records.pesa.service.category.CategoryService
import com.records.pesa.service.category.CategoryServiceImpl
import com.records.pesa.service.transaction.TransactionService
import com.records.pesa.service.transaction.TransactionsServiceImpl
import com.records.pesa.service.userAccount.UserAccountService
import com.records.pesa.service.userAccount.UserAccountServiceImpl
import com.records.pesa.workers.WorkersRepository
import com.records.pesa.workers.WorkersRepositoryImpl
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

interface AppContainer {
    val apiRepository: ApiRepository
    val workersRepository: WorkersRepository
    val dbRepository: DBRepository
    val transactionService: TransactionService
    val userAccountService: UserAccountService
    val categoryService: CategoryService
}

class AppContainerImpl(context: Context): AppContainer {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
//    private val baseUrl = "https://cashledger-backend-java.onrender.com/api/"
    private val baseUrl = "http://192.168.100.5:8080/api/"
//      private val baseUrl = "https://example.com/"

//    private val baseUrl = "https://8d1b-102-211-145-169.ngrok-free.app/api/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    // First Retrofit instance for the first URL
    private val retrofitLipa = Retrofit.Builder()
        .baseUrl("https://4cv4a4f2fc.execute-api.eu-central-1.amazonaws.com/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    // Second Retrofit instance for the second URL
    private val retrofitLipaStatus = Retrofit.Builder()
        .baseUrl("https://6x0twkya55.execute-api.eu-central-1.amazonaws.com/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val retrofitService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    private val retrofitServiceLipa: ApiService by lazy {
        retrofitLipa.create(ApiService::class.java)
    }

    private val retrofitServiceLipaStatus: ApiService by lazy {
        retrofitLipaStatus.create(ApiService::class.java)
    }

    override val dbRepository: DBRepository by lazy {
        DBRepositoryImpl(AppDatabase.getDatabase(context).appDao())
    }

    override val transactionService: TransactionService by lazy {
        TransactionsServiceImpl(
            AppDatabase.getDatabase(context).transactionDao(),
            AppDatabase.getDatabase(context).categoryDao()
        )
    }
    override val userAccountService: UserAccountService by lazy {
        UserAccountServiceImpl(AppDatabase.getDatabase(context).userDao())
    }
    override val categoryService: CategoryService by lazy {
        CategoryServiceImpl(AppDatabase.getDatabase(context).categoryDao())
    }

    override val apiRepository: ApiRepository by lazy {
        ApiRepositoryImpl(retrofitService, retrofitServiceLipa, retrofitServiceLipaStatus,  dbRepository)
    }
    override val workersRepository: WorkersRepository = WorkersRepositoryImpl(context)


}