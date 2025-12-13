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

    // Update RemoteViews with provided data
    private suspend fun updateRemoteViews(
        team1Name: String,
        team1Score: Int,
        team2Name: String,
        team2Score: Int,
        timestamp: Long,
        team1ImageUrl: String?,
        team2ImageUrl: String?,
    ) {
        remoteViews.setTextViewText(R.id.team1_name, team1Name)
        remoteViews.setTextViewText(R.id.team2_name, team2Name)
        remoteViews.setTextViewText(R.id.score, "$team1Score : $team2Score")

        val elapsedRealtime = android.os.SystemClock.elapsedRealtime()
        val currentTimeMillis = System.currentTimeMillis()
        val base = elapsedRealtime - (currentTimeMillis - timestamp)
        remoteViews.setChronometer(R.id.match_time, base, null, true)

        val team1Image = if (!team1ImageUrl.isNullOrEmpty()) loadImageBitmap(team1ImageUrl) else null
        val team2Image = if (!team2ImageUrl.isNullOrEmpty()) loadImageBitmap(team2ImageUrl) else null
        team1Image?.let { remoteViews.setImageViewBitmap(R.id.team1_image_placeholder, it) }
        team2Image?.let { remoteViews.setImageViewBitmap(R.id.team2_image_placeholder, it) }
    }

    override suspend fun buildNotification(
        notification: Notification.Builder,
        event: String,
        data: Map<String, Any>
    ): Notification {
        val matchName = data["matchName"] as? String ?: "Match"
        val timestamp = (data["matchStartDate"] as? Number)?.toLong() ?: System.currentTimeMillis()
        val team1Name = data["teamAName"] as? String ?: "Team A"
        val team1Score = (data["teamAScore"] as? Number)?.toInt() ?: 0
        val team2Name = data["teamBName"] as? String ?: "Team B"
        val team2Score = (data["teamBScore"] as? Number)?.toInt() ?: 0

        val team1ImageUrl = if (event == "update") null else data["teamAImageUrl"] as? String
        val team2ImageUrl = if (event == "update") null else data["teamBImageUrl"] as? String

        // Update views
        updateRemoteViews(
            team1Name,
            team1Score,
            team2Name,
            team2Score,
            timestamp,
            team1ImageUrl,
            team2ImageUrl,
        )

        return notification
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setOngoing(true)
            .setContentTitle("$team1Name vs $team2Name")
            .setContentIntent(pendingIntent)
            .setContentText("$team1Score : $team2Score")
            .setStyle(Notification.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setPriority(Notification.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_EVENT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()
    }
}
