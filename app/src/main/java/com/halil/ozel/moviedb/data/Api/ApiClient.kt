package com.halil.ozel.moviedb.data.Api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// ApiClient sebagai Singleton object (setara dengan class statis di Java)
object ApiClient {
    private const val BASE_URL = "https://api.themoviedb.org/3/"

    // Inisialisasi Retrofit hanya dilakukan sekali (Lazy Initialization)
    // dan hasilnya disimpan. Ini setara dengan blok 'if (retrofit == null)' di Java.
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Membuat instance ApiService menggunakan Retrofit yang sudah diinisialisasi secara lazy.
    // Ini setara dengan 'return retrofit.create(ApiService.class)' di Java.
    val service: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}