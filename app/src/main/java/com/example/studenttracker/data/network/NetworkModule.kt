package com.example.studenttracker.data.network

import com.example.studenttracker.BuildConfig
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class TrackerApiException(val code: String, message: String) : IOException(message)

class TrackerInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = AuthSession.idToken ?: throw TrackerApiException("UNAUTHENTICATED", "Sign in to continue.")
        val payload = if (original.body != null) {
            val buffer = Buffer()
            original.body!!.writeTo(buffer)
            JsonParser.parseString(buffer.readUtf8()).asJsonObject
        } else JsonObject()
        original.url.queryParameterNames.forEach { name -> payload.addProperty(name, original.url.queryParameter(name)) }
        payload.addProperty("idToken", token)
        val request = original.newBuilder().url(original.url.newBuilder().query(null).build())
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        val response = chain.proceed(request)
        try {
            if (!response.isSuccessful) throw TrackerApiException("HTTP", "The service is unavailable (${response.code}). Try again.")
            val envelope = JsonParser.parseString(response.peekBody(2_000_000).string()).asJsonObject
            if (envelope.get("apiVersion")?.asInt != 2) throw TrackerApiException("CONFIGURATION", "Deploy the version 2 Apps Script backend before using this app.")
            if (envelope.get("status")?.asString != "success") {
                val code = envelope.get("code")?.asString ?: "API_ERROR"
                if (code == "UNAUTHENTICATED") AuthSession.clear()
                throw TrackerApiException(code, envelope.get("message")?.asString ?: "The request could not be completed.")
            }
            return response
        } catch (error: Exception) {
            response.close()
            if (error is IOException) throw error
            throw TrackerApiException("INVALID_RESPONSE", "The backend returned an invalid response. Check its deployment.")
        }
    }
}

object NetworkModule {
    val isConfigured: Boolean get() = BuildConfig.TRACKER_BASE_URL.startsWith("https://script.google.com/macros/s/") &&
        BuildConfig.TRACKER_BASE_URL.endsWith("/") && BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(75, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .addInterceptor(TrackerInterceptor())
        .build()

    val api: GoogleSheetsApi by lazy {
        check(isConfigured) { "Configure TRACKER_BASE_URL and GOOGLE_WEB_CLIENT_ID before signing in." }
        Retrofit.Builder()
            .baseUrl(BuildConfig.TRACKER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleSheetsApi::class.java)
    }
}
