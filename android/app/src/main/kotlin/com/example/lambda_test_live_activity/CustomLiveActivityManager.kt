package com.example.lambda_test_live_activity

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import com.example.live_activities.LiveActivityManager

class CustomLiveActivityManager(context: Context) : LiveActivityManager(context) {
    private val appContext: Context = context.applicationContext

    private val pendingIntent = PendingIntent.getActivity(
        appContext,
        200,
        Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private val remoteViews = RemoteViews(
        appContext.packageName,
        R.layout.live_activity
    )

    // Optional: load image from URL and resize to ~64dp
    private suspend fun loadImageBitmap(imageUrl: String?): Bitmap? {
        val dp = appContext.resources.displayMetrics.density
        val targetPx = (64 * dp).toInt()
        return withContext(Dispatchers.IO) {
            if (imageUrl.isNullOrEmpty()) return@withContext null
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.connect()
                connection.inputStream.use { inputStream ->
                    val original = BitmapFactory.decodeStream(inputStream)
                    original?.let {
                        Bitmap.createScaledBitmap(it, targetPx, targetPx, true)
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    // Update RemoteViews with job searching data
    private suspend fun updateRemoteViewsForJob(
        company: String,
        jobTitle: String,
        description: String,
        postedAtMillis: Long,
        imageUrl: String?,
    ) {
        // Map existing views: company, title, description
        remoteViews.setTextViewText(R.id.team1_name, company)
        remoteViews.setTextViewText(R.id.team2_name, jobTitle)
        remoteViews.setTextViewText(R.id.score, description)

        val elapsedRealtime = android.os.SystemClock.elapsedRealtime()
        val currentTimeMillis = System.currentTimeMillis()
        val base = elapsedRealtime - (currentTimeMillis - postedAtMillis)
        remoteViews.setChronometer(R.id.match_time, base, null, true)

        val logo = if (!imageUrl.isNullOrEmpty()) loadImageBitmap(imageUrl) else null
        logo?.let { remoteViews.setImageViewBitmap(R.id.team1_image_placeholder, it) }
    }

    override suspend fun buildNotification(
        notification: Notification.Builder,
        event: String,
        data: Map<String, Any>
    ): Notification {
        // Prefer job searching fields
        val company = data["company"] as? String ?: (data["matchName"] as? String ?: "Company")
        val jobTitle = data["jobTitle"] as? String ?: (data["teamBName"] as? String ?: "Job Title")
        val description = data["description"] as? String ?: "Албан тушаал зарлагдлаа"
        val postedAt = (data["postedAt"] as? Number)?.toLong()
            ?: (data["matchStartDate"] as? Number)?.toLong()
            ?: System.currentTimeMillis()
        val imageUrl = data["imageUrl"] as? String

        // Update views for job
        updateRemoteViewsForJob(company, jobTitle, description, postedAt, imageUrl)

        return notification
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setOngoing(true)
            .setContentTitle(company)
            .setContentIntent(pendingIntent)
            .setContentText(jobTitle)
            .setStyle(Notification.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setPriority(Notification.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_EVENT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()
    }
}
