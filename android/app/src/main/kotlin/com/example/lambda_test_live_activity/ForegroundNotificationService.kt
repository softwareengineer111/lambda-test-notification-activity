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
import android.widget.RemoteViews
import com.example.lambda_test_live_activity.R

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

        // Custom RemoteViews for collapsed and expanded notification
        val collapsedView = RemoteViews(packageName, R.layout.notification_small)
        collapsedView.setTextViewText(R.id.notif_title, title)
        collapsedView.setTextViewText(R.id.notif_text, text)

        val expandedView = RemoteViews(packageName, R.layout.notification_big)
    expandedView.setTextViewText(R.id.notif_big_title, title)
    expandedView.setTextViewText(R.id.notif_big_text, text)
    // Use the text field as ETA display when available (caller can pass ETA via text)
    expandedView.setTextViewText(R.id.notif_eta, text)

        // Action intents for buttons (call, navigate, stop)
        val callIntent = Intent(this, ForegroundNotificationService::class.java).apply {
            action = ACTION_UPDATE
            putExtra(EXTRA_TEXT, "call")
        }
        val callPending = PendingIntent.getService(this, 1, callIntent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        val navIntent = Intent(this, ForegroundNotificationService::class.java).apply {
            action = ACTION_UPDATE
            putExtra(EXTRA_TEXT, "navigate")
        }
        val navPending = PendingIntent.getService(this, 2, navIntent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        val stopIntent = Intent(this, ForegroundNotificationService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(this, 3, stopIntent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        // Wire button clicks in RemoteViews to the pending intents
        expandedView.setOnClickPendingIntent(R.id.action_call, callPending)
        expandedView.setOnClickPendingIntent(R.id.action_navigate, navPending)
        expandedView.setOnClickPendingIntent(R.id.action_stop, stopPending)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentIntent(pendingIntent)
            .setLargeIcon(largeIcon)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)

        // Indeterminate spinner feel
        builder.setProgress(0, 0, true)

        return builder.build()
    }
}
