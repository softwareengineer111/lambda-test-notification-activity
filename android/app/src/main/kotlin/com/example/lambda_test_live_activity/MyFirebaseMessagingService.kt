package com.example.lambda_test_live_activity

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.lambda_test_live_activity.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        private const val CHANNEL_ID = "ride_channel"
        private const val NOTIF_ID = 101
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        try {
            Log.d(TAG, "=== FCM Message Received ===")
            Log.d(TAG, "From: ${message.from}")
            Log.d(TAG, "Data: ${message.data}")
            Log.d(TAG, "Notification: ${message.notification?.title} - ${message.notification?.body}")
            
            // Check if this is from OneSignal - they use sender ID format or include specific keys
            val from = message.from ?: ""
            val isOneSignal = from.contains("onesignal") || 
                              message.data.containsKey("custom") || 
                              message.data.containsKey("i") ||  // OneSignal notification ID
                              message.data.containsKey("google.delivered_priority")
            
            // For OneSignal or any push notification, always show custom notification
            Log.d(TAG, "Showing custom notification (OneSignal: $isOneSignal)")
            
            // Parse notification data - handle both OneSignal format and standard format
            var title = message.notification?.title ?: message.data["title"] ?: "Ride"
            var body = message.notification?.body ?: message.data["alert"] ?: message.data["body"] ?: "Driver arriving..."
            
            // Check notification type first (before parsing OneSignal custom data)
            var notificationType = message.data["type"] ?: "ride"
            
            // OneSignal stores additional data in "custom" JSON string
            val customJson = message.data["custom"]
            if (customJson != null) {
                try {
                    val customData = org.json.JSONObject(customJson)
                    val additionalData = customData.optJSONObject("a")
                    if (additionalData != null) {
                        // Check type from custom data
                        notificationType = additionalData.optString("type", notificationType)
                        Log.d(TAG, "OneSignal additionalData type: $notificationType")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing OneSignal custom data", e)
                }
            }
            
            Log.d(TAG, "Notification type: $notificationType")
            
            if (notificationType == "job") {
                // Parse job notification data from custom JSON if available
                var company = message.data["company"] ?: title
                var jobTitle = message.data["jobTitle"] ?: body
                var description = message.data["description"] ?: "Ажлын байр нээгдлээ"
                var jobUrl = message.data["jobUrl"]
                var companyImageUrl = message.data["companyImageUrl"] ?: message.data["imageUrl"]
                
                // Override with OneSignal custom data if available
                if (customJson != null) {
                    try {
                        val customData = org.json.JSONObject(customJson)
                        val additionalData = customData.optJSONObject("a")
                        if (additionalData != null) {
                            company = additionalData.optString("company", company)
                            jobTitle = additionalData.optString("jobTitle", jobTitle)
                            description = additionalData.optString("description", description)
                            jobUrl = additionalData.optString("jobUrl", jobUrl)
                            companyImageUrl = additionalData.optString("companyImageUrl", 
                                additionalData.optString("imageUrl", companyImageUrl))
                            
                            Log.d(TAG, "📦 Extracted from OneSignal additionalData:")
                            Log.d(TAG, "  company: $company")
                            Log.d(TAG, "  jobTitle: $jobTitle")
                            Log.d(TAG, "  description: $description")
                            Log.d(TAG, "  companyImageUrl: $companyImageUrl")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing job data from custom", e)
                    }
                }
                
                Log.d(TAG, "🔔 Showing JOB notification: company=$company, jobTitle=$jobTitle, companyImageUrl=$companyImageUrl")
                JobNotificationHelper.show(applicationContext, company, jobTitle, description, jobUrl, companyImageUrl)
            } else {
                // Ride notification (fallback)
                val status = message.data["status"] ?: body
                val eta = message.data["eta"] ?: "5 min"
                
                Log.d(TAG, "Showing custom notification: title=$title, status=$status, eta=$eta")
                RideNotificationHelper.show(applicationContext, title, status, eta)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notification from FCM: ", e)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(CHANNEL_ID, "Ride Status", NotificationManager.IMPORTANCE_HIGH)
            chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Ensure channel exists as early as possible
        createChannel()
        Log.d(TAG, "MyFirebaseMessagingService created and channel ensured")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM new token: $token")
        // Optionally: send token to server here
    }
}
