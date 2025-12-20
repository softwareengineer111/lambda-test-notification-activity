package com.example.lambda_test_live_activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object LiveActivityNotificationHelper {
    private const val CHANNEL_ID = "job_alerts"
    private const val NOTIF_ID = 200

    fun show(
        context: Context,
        company: String,
        jobTitle: String,
        description: String,
        jobUrl: String? = null,
        companyImageUrl: String? = null,
        postedAtMillis: Long? = null,
    ) {
        createChannel(context)

        val remoteViews = RemoteViews(context.packageName, R.layout.live_activity)
        remoteViews.setTextViewText(R.id.team1_name, company)
        remoteViews.setTextViewText(R.id.team2_name, jobTitle)
        remoteViews.setTextViewText(R.id.score, description)

        // Always show a placeholder first (also used for error state).
        remoteViews.setImageViewResource(R.id.team1_image_placeholder, R.drawable.ic_company_placeholder)

        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("job_url", jobUrl)
            putExtra("from_live_activity", true)
        }

        val immFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val clickPendingIntent = PendingIntent.getActivity(
            context,
            200,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or immFlag
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(company)
            .setContentText(jobTitle)
            .setContentIntent(clickPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, builder.build())

        if (!companyImageUrl.isNullOrEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                val logo = withContext(Dispatchers.IO) {
                    loadCircleLogoBitmap(context, companyImageUrl)
                }
                if (logo != null) {
                    remoteViews.setImageViewBitmap(R.id.team1_image_placeholder, logo)
                    manager.notify(NOTIF_ID, builder.build())
                }
            }
        }
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

    private fun loadCircleLogoBitmap(context: Context, imageUrl: String): Bitmap? {
        return try {
            val sizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                56f,
                context.resources.displayMetrics
            ).toInt()

            Glide.with(context.applicationContext)
                .asBitmap()
                .load(imageUrl)
                .circleCrop()
                .submit(sizePx, sizePx)
                .get()
        } catch (_: Exception) {
            null
        }
    }
}
