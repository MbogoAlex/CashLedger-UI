package com.records.pesa.container

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.records.pesa.db.AppDatabase
import com.records.pesa.db.DBRepository
import com.records.pesa.db.DBRepositoryImpl
import com.records.pesa.network.ApiRepository
import com.records.pesa.network.ApiRepositoryImpl
import com.records.pesa.network.ApiService
import com.records.pesa.workers.WorkersRepository
import com.records.pesa.workers.WorkersRepositoryImpl
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

interface AppContainer {
    val apiRepository: ApiRepository
    val workersRepository: WorkersRepository
    val dbRepository: DBRepository
}

class AppContainerImpl(context: Context): AppContainer {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val baseUrl = "https://cashledger-backend-java.onrender.com/api/"
//    private val baseUrl = "http://192.168.0.106:8080/api/"
//    private val baseUrl = "https://8d1b-102-211-145-169.ngrok-free.app/api/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val retrofitService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    override val dbRepository: DBRepository by lazy {
        DBRepositoryImpl(AppDatabase.getDatabase(context).appDao())
    }

    override val apiRepository: ApiRepository by lazy {
        ApiRepositoryImpl(retrofitService, dbRepository)
    }
    override val workersRepository: WorkersRepository = WorkersRepositoryImpl(context)


}