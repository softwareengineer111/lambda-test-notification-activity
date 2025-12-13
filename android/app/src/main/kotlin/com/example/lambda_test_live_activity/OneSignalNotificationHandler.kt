package com.example.lambda_test_live_activity

import android.content.Context
import org.json.JSONObject

object OneSignalNotificationHandler {
    
    fun handleNotification(context: Context, title: String?, body: String?, additionalData: JSONObject?) {
        // Extract data from notification
        val notifTitle = additionalData?.optString("title") ?: title ?: "Ride"
        val status = additionalData?.optString("status") ?: body ?: "Driver arriving..."
        val eta = additionalData?.optString("eta") ?: "5 min"
        
        // Show custom RemoteViews notification
        RideNotificationHelper.show(context, notifTitle, status, eta)
    }
}
