package com.example.lambda_test_live_activity

import android.app.Service
import android.content.Intent
import android.os.IBinder

// Deprecated service: ride-related foreground notifications removed
class ForegroundNotificationService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf()
        return START_NOT_STICKY
    }
}
