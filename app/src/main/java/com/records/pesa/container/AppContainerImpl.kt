package com.records.pesa.container

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.records.pesa.db.AppDatabase
import com.records.pesa.db.DBRepository
import com.records.pesa.db.DBRepositoryImpl
import com.records.pesa.network.ApiRepository
import com.records.pesa.network.ApiRepositoryImpl
import com.records.pesa.network.ApiService
import com.records.pesa.service.auth.AuthenticationManager
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
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class AppContainerImpl(context: Context): AppContainer {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    //    private val baseUrl = "https://cashledger-backend-java.onrender.com/api/"
//    private val baseUrl = "https://mledger-a110f0487fa8.herokuapp.com/api/"
    private val baseUrl = "https://prod.kiwitechhub.com/api/v1/"
//    private val baseUrl = "https://prod.kiwitechhub.com/api/v1/"
//      private val baseUrl = "https://example.com/"

//    private val baseUrl = "https://8d1b-102-211-145-169.ngrok-free.app/api/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(createOkHttpClient())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()


    private val retrofitService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    // Create OkHttpClient with custom timeouts
    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        return OkHttpClient.Builder()
            .connectTimeout(180, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
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
        ApiRepositoryImpl(retrofitService)
    }
    override val workersRepository: WorkersRepository = WorkersRepositoryImpl(context)

    override val authenticationManager: AuthenticationManager by lazy {
        AuthenticationManager(
            apiRepository = apiRepository,
            dbRepository = dbRepository
        )
    }


}