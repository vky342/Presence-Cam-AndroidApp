package com.example.projectkas.Network


import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Request
import org.json.JSONObject


object RetrofitInstance {

    private const val RENDER_SERVICE_URL = "https://mongo-endpoint.onrender.com"
    private var baseUrl: String? = null
    private var retrofitInstance: Retrofit? = null
    private var isInitialized = false

    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Initialize the base URL from config asynchronously
     * This won't block the main thread
     * Call this from KASApplication.onCreate()
     */
    fun initialize(onComplete: ((Boolean) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            println("🔄 Fetching base URL from Render service...")
            baseUrl = "https://pcam.vky342.in"

            if (baseUrl != null) {
                println("✅ Base URL loaded: $baseUrl")
                isInitialized = true
                onComplete?.invoke(true)
            } else {
                println("❌ Failed to fetch base URL from Render service")
                isInitialized = false
                onComplete?.invoke(false)
            }
        }
    }

    suspend fun getApi(): ApiService {
        // Wait until initialized
        while (!isInitialized) {
            delay(50)
        }
        return getRetrofitInstance().create(ApiService::class.java)
    }

    private fun getRetrofitInstance(): Retrofit {
        if (retrofitInstance == null) {
            val url = baseUrl ?: throw IllegalStateException(
                "Base URL not initialized. Config fetch may still be in progress or failed."
            )

            println("🚀 Initializing Retrofit with base URL: $url")

            retrofitInstance = Retrofit.Builder()
                .baseUrl(url)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofitInstance!!
    }

    /**
     * Fetches the dynamic Ngrok URL from the Render backend
     */
    private fun fetchUrlFromRender(): String? {
        return try {
            val request = Request.Builder()
                .url(RENDER_SERVICE_URL)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (!responseBody.isNullOrEmpty()) {
                    // Start: Logging (Optional)
                    println("Render Response: $responseBody")
                    // End: Logging

                    val jsonObject = JSONObject(responseBody)
                    // Matches the {"url": "..."} response from your python backend
                    val dynamicUrl = jsonObject.getString("url")

                    // Ensure it ends with a slash if Retrofit requires it (usually good practice)
                    if (!dynamicUrl.endsWith("/")) "$dynamicUrl/" else dynamicUrl
                } else {
                    null
                }
            } else {
                println("Render service returned code: ${response.code}")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Check if the base URL has been loaded
     */
    fun isReady(): Boolean = isInitialized
}