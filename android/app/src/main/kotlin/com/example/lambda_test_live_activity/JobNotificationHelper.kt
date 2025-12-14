package com.example.lambda_test_live_activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object JobNotificationHelper {
    private const val CHANNEL_ID = "job_alerts"
    private const val NOTIF_ID = 200

    fun show(context: Context, company: String, jobTitle: String, description: String, jobUrl: String? = null, imageUrl: String? = null) {
        createChannel(context)

        val collapsedView = RemoteViews(context.packageName, R.layout.notification_small)
        collapsedView.setTextViewText(R.id.job_company, company)
        collapsedView.setTextViewText(R.id.job_title, jobTitle)

        val expandedView = RemoteViews(context.packageName, R.layout.notification_big)
        expandedView.setTextViewText(R.id.job_big_company, company)
        expandedView.setTextViewText(R.id.job_big_title, jobTitle)
        expandedView.setTextViewText(R.id.job_description, description)
        
        // Load company logo if URL provided
        if (!imageUrl.isNullOrEmpty()) {
            try {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Thread {
                        try {
                            val url = java.net.URL(imageUrl)
                            val bitmap = android.graphics.BitmapFactory.decodeStream(url.openConnection().getInputStream())
                            collapsedView.setImageViewBitmap(R.id.job_icon, bitmap)
                            expandedView.setImageViewBitmap(R.id.job_big_icon, bitmap)
                            
                            // Re-notify to update images
                            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            val builder = buildNotification(context, collapsedView, expandedView, jobUrl)
                            manager.notify(NOTIF_ID, builder.build())
                        } catch (e: Exception) {
                            android.util.Log.e("JobNotificationHelper", "Failed to load image", e)
                        }
                    }.start()
                }
            } catch (e: Exception) {
                android.util.Log.e("JobNotificationHelper", "Error setting up image loading", e)
            }
        }

        val immFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) 
            PendingIntent.FLAG_IMMUTABLE else 0

        // ҮЗЭХ button - open job detail
        val viewIntent = if (jobUrl != null && jobUrl.isNotEmpty()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(jobUrl))
        } else {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("action", "view_job")
            }
        }
        val viewPending = PendingIntent.getActivity(
            context, 1, viewIntent, immFlag or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // CV Илгээх button - open apply URL or form
        val applyIntent = if (jobUrl != null && jobUrl.isNotEmpty()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(jobUrl + "/apply"))
        } else {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("action", "apply_job")
            }
        }
        val applyPending = PendingIntent.getActivity(
            context, 2, applyIntent, immFlag or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Set button click handlers
        expandedView.setOnClickPendingIntent(R.id.action_view, viewPending)
        expandedView.setOnClickPendingIntent(R.id.action_apply, applyPending)

        val builder = buildNotification(context, collapsedView, expandedView, jobUrl)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, builder.build())
    }

    private fun buildNotification(context: Context, collapsedView: RemoteViews, expandedView: RemoteViews, jobUrl: String?): NotificationCompat.Builder {
        val contentIntent = if (jobUrl != null && jobUrl.isNotEmpty()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(jobUrl))
        } else {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
        
        val immFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) 
            PendingIntent.FLAG_IMMUTABLE else 0
        val contentPending = PendingIntent.getActivity(
            context, 0, contentIntent, immFlag or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentIntent(contentPending)
            .setLargeIcon(largeIcon)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
    }

    fun update(context: Context, company: String, jobTitle: String, description: String) {
        show(context, company, jobTitle, description)
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIF_ID)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Job Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new job postings"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
