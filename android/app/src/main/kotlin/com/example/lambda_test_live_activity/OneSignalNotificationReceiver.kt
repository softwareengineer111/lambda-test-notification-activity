package com.example.lambda_test_live_activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class OneSignalNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("OneSignalReceiver", "Notification received in background")
        
        // Extract notification data from intent extras
        val title = intent.getStringExtra("title")
        val body = intent.getStringExtra("body")
        val customData = intent.getStringExtra("customData")
        
        // Parse additional data if available
        try {
            val additionalData = if (customData != null) {
                org.json.JSONObject(customData)
            } else {
                null
            }
            
            // Show custom notification
            OneSignalNotificationHandler.handleNotification(context, title, body, additionalData)
        } catch (e: Exception) {
            Log.e("OneSignalReceiver", "Error handling notification", e)
        }
    }
}
