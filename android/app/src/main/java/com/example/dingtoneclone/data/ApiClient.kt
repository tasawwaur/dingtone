package com.example.dingtoneclone.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

// ---- Replace with your actual backend URL ----
// For local dev with emulator: "http://10.0.2.2:5000/api/"
// For production: "https://your-backend.railway.app/api/"
private const val BASE_URL = "http://10.0.2.2:5000/api/"

object ApiClient {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Interceptor that auto-attaches the Firebase ID token as Bearer */
    private val authInterceptor = Interceptor { chain ->
        val auth = FirebaseAuth.getInstance()
        // Blocking call — safe because OkHttp dispatches on its own thread pool
        val token = runCatching {
            kotlinx.coroutines.runBlocking {
                auth.currentUser?.getIdToken(false)?.await()?.token
            }
        }.getOrNull()

        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }
}
