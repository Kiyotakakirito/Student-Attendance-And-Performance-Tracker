package com.example.studenttracker.data.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    // Note: Apps Script URLs redirect, but Retrofit handles redirects automatically.
    // The base URL must end in a trailing slash. We will put the ID in the base URL
    // and just use "exec" in the endpoint.
    private const val BASE_URL = "https://script.google.com/macros/s/AKfycbz8vjArV5jZrzwOC2S1v9DkPqXmfcWzA57FsS9bNHfoj6dKm3TUkoBKhSPV36g9EhtKnQ/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    val api: GoogleSheetsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleSheetsApi::class.java)
    }
}
