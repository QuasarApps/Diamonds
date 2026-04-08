package com.example.diamonds

import android.app.Application
import com.example.diamonds.fcm.DiamondsFcmService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DiamondsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Create notification channels for Android 8+ (no-op on older versions)
        DiamondsFcmService.createNotificationChannels(this)
    }
}
