package com.example.projectkas

import android.app.Application
import com.example.projectkas.Network.RetrofitInstance
import dagger.hilt.android.HiltAndroidApp
import android.util.Log

@HiltAndroidApp
class KASApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Retrofit asynchronously (won't block app startup)
        RetrofitInstance.initialize { success ->
            if (success) {
                Log.d("KASApplication", "✅ Base URL loaded successfully")
            } else {
                Log.e("KASApplication", "❌ Failed to load base URL")
                // Optionally: show a toast or handle the error
            }
        }

        // App continues loading normally while config is being fetched
    }
}