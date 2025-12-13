package com.example.lambda_test_live_activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

object RideNotificationHelper {
    private const val CHANNEL_ID = "ride_channel"
    private const val CHANNEL_NAME = "Ride Notifications"
    private const val NOTIF_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    fun show(context: Context, title: String, status: String, eta: String) {
        ensureChannel(context)

        val collapsed = RemoteViews(context.packageName, R.layout.notification_ride_collapsed)
        val expanded = RemoteViews(context.packageName, R.layout.notification_ride_expanded)

        collapsed.setTextViewText(R.id.tv_title, title)
        collapsed.setTextViewText(R.id.tv_eta, "ETA: $eta")

        expanded.setTextViewText(R.id.tv_title, title)
        expanded.setTextViewText(R.id.tv_status, status)

        val largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)

        val callPI = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, NotificationActionReceiver::class.java).apply { action = "ACTION_CALL" },
            PendingIntent.FLAG_IMMUTABLE
        )
        val navPI = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, NotificationActionReceiver::class.java).apply { action = "ACTION_NAVIGATE" },
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopPI = PendingIntent.getBroadcast(
            context, 2,
            Intent(context, NotificationActionReceiver::class.java).apply { action = "ACTION_STOP" },
            PendingIntent.FLAG_IMMUTABLE
        )

        expanded.setOnClickPendingIntent(R.id.btn_call, callPI)
        expanded.setOnClickPendingIntent(R.id.btn_navigate, navPI)
        expanded.setOnClickPendingIntent(R.id.btn_stop, stopPI)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
            .setLargeIcon(largeIcon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, builder.build())
    }

    fun update(context: Context, status: String, eta: String) {
        show(context, "Ride", status, eta)
    }

    fun cancel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ID)
    }
}