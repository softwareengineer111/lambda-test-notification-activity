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
        // Ensure the notification channel exists on Android O+
        // createChannel() is called in onCreate so channel exists before messages arrive
        try {
            Log.d(TAG, "onMessageReceived: ${message.data}")

            val title = message.notification?.title ?: message.data["title"] ?: "Ride"
            val text = message.notification?.body ?: message.data["text"] ?: message.data["eta"] ?: "Updating..."

            // Build RemoteViews similar to ForegroundNotificationService
            val collapsedView = RemoteViews(packageName, R.layout.notification_small)
            collapsedView.setTextViewText(R.id.notif_title, title)
            collapsedView.setTextViewText(R.id.notif_text, text)

            val expandedView = RemoteViews(packageName, R.layout.notification_big)
            expandedView.setTextViewText(R.id.notif_big_title, title)
            expandedView.setTextViewText(R.id.notif_big_text, text)
            expandedView.setTextViewText(R.id.notif_eta, text)

            // Base content intent to open the app when tapping the notification
            val contentIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val immFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val updateFlag = PendingIntent.FLAG_UPDATE_CURRENT

            val contentPending = PendingIntent.getActivity(this, 0, contentIntent, immFlag)

            // Button intents (unique requestCodes so extras don't get mixed). Use UPDATE_CURRENT so extras are delivered.
            val callIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("notif_action", "call")
            }
            val callPending = PendingIntent.getActivity(this, 1, callIntent, immFlag or updateFlag)

            val navIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("notif_action", "navigate")
            }
            val navPending = PendingIntent.getActivity(this, 2, navIntent, immFlag or updateFlag)

            val stopIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("notif_action", "stop")
            }
            val stopPending = PendingIntent.getActivity(this, 3, stopIntent, immFlag or updateFlag)

            val largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentIntent(contentPending)
                .setLargeIcon(largeIcon)
                .setOngoing(false)
                .setOnlyAlertOnce(true)
                .setColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(collapsedView)
                .setCustomBigContentView(expandedView)

            builder.setProgress(0, 0, true)

            // Wire the buttons to pending intents on the expanded view (RemoteViews)
            expandedView.setOnClickPendingIntent(R.id.action_call, callPending)
            expandedView.setOnClickPendingIntent(R.id.action_navigate, navPending)
            expandedView.setOnClickPendingIntent(R.id.action_stop, stopPending)

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIF_ID, builder.build())

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
