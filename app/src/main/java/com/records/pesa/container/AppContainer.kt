package com.records.pesa.container

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.records.pesa.network.ApiRepository
import com.records.pesa.network.ApiRepositoryImpl
import com.records.pesa.network.ApiService
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

interface AppContainer {
    val apiRepository: ApiRepository
}

class AppContainerImpl(context: Context): AppContainer {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val baseUrl = "http://192.168.210.6:8080/api/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val retrofitService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    override val apiRepository: ApiRepository by lazy {
        ApiRepositoryImpl(retrofitService)
    }
}