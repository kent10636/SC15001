package com.walkietalkie.app.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // Change this to your machine's LAN IP when using real device
    // For emulator use 10.0.2.2
    private const val BASE_URL = "http://10.0.2.2:8080/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}