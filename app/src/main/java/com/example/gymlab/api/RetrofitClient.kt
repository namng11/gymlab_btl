package com.example.gymlab.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // 10.0.2.2 trỏ đến localhost của máy tính từ Emulator
    // Cổng 3000 là mặc định của Node.js

//    private const val BASE_URL = "http://192.168.100.234:3000/"

//    private const val BASE_URL = "http://192.168.100.234:3000/"
    private const val BASE_URL = "http://192.168.100.233:3000/"

    val instance: AuthApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(AuthApi::class.java)
    }
}