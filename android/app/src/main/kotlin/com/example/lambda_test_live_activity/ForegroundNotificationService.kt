package com.example.lambda_test_live_activity

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class ForegroundNotificationService : Service() {

    companion object {
        const val TAG = "FGService"
        const val CHANNEL_ID = "ride_channel"
        const val CHANNEL_NAME = "Ride Status"
        const val NOTIF_ID = 101

        const val ACTION_START = "ACTION_START"
        const val ACTION_UPDATE = "ACTION_UPDATE"
        const val ACTION_STOP = "ACTION_STOP"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TEXT = "extra_text"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Ride"
        val text = intent?.getStringExtra(EXTRA_TEXT) ?: "Updating..."

        when (action) {
            ACTION_START -> {
                Log.i(TAG, "Starting foreground service")
                val notification = buildNotification(title, text)
                startForeground(NOTIF_ID, notification)
            }
            ACTION_UPDATE -> {
                Log.i(TAG, "Updating notification: \$title - \$text")
                val notification = buildNotification(title, text)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIF_ID, notification)
            }
            ACTION_STOP -> {
                Log.i(TAG, "Stopping foreground service")
                stopForeground(true)
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun buildNotification(title: String, text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        val largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            // Use a proper monochrome small icon for notifications.
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setLargeIcon(largeIcon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
            .setProgress(0, 0, true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }
}
