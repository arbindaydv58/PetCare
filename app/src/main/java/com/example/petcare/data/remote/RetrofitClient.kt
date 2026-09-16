package com.example.petcare.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://6aaa713dff4dd5698b4e8da7.mockapi.io/"

    val vetApiService: VetApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VetApiService::class.java)
    }
}
