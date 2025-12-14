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
        android.util.Log.d("CustomLiveActivity", "🎨 updateRemoteViewsForJob called")
        android.util.Log.d("CustomLiveActivity", "  Setting company: $company")
        android.util.Log.d("CustomLiveActivity", "  Setting jobTitle: $jobTitle")
        android.util.Log.d("CustomLiveActivity", "  Setting description: $description")
        android.util.Log.d("CustomLiveActivity", "  Loading imageUrl: $imageUrl")
        
        // Map existing views: company, title, description
        remoteViews.setTextViewText(R.id.team1_name, company)
        remoteViews.setTextViewText(R.id.team2_name, jobTitle)
        remoteViews.setTextViewText(R.id.score, description)

        val elapsedRealtime = android.os.SystemClock.elapsedRealtime()
        val currentTimeMillis = System.currentTimeMillis()
        val base = elapsedRealtime - (currentTimeMillis - postedAtMillis)
        remoteViews.setChronometer(R.id.match_time, base, null, true)

        val logo = if (!imageUrl.isNullOrEmpty()) loadImageBitmap(imageUrl) else null
        if (logo != null) {
            android.util.Log.d("CustomLiveActivity", "✅ Logo loaded successfully: ${logo.width}x${logo.height}")
            remoteViews.setImageViewBitmap(R.id.team1_image_placeholder, logo)
        } else {
            android.util.Log.w("CustomLiveActivity", "❌ Failed to load logo from URL")
        }
    }

    override suspend fun buildNotification(
        notification: Notification.Builder,
        event: String,
        data: Map<String, Any>
    ): Notification {
        android.util.Log.d("CustomLiveActivity", "=== buildNotification called ===")
        android.util.Log.d("CustomLiveActivity", "Event: $event")
        android.util.Log.d("CustomLiveActivity", "Data keys: ${data.keys}")
        android.util.Log.d("CustomLiveActivity", "Full data: $data")
        
        // Prefer job searching fields
        val company = data["company"] as? String ?: (data["matchName"] as? String ?: "Company")
        val jobTitle = data["jobTitle"] as? String ?: (data["teamBName"] as? String ?: "Job Title")
        val description = data["description"] as? String ?: "Албан тушаал зарлагдлаа"
        val postedAt = (data["postedAt"] as? Number)?.toLong()
            ?: (data["matchStartDate"] as? Number)?.toLong()
            ?: System.currentTimeMillis()
        val companyImageUrl = data["companyImageUrl"] as? String ?: data["imageUrl"] as? String
        val jobUrl = data["jobUrl"] as? String

        android.util.Log.d("CustomLiveActivity", "📋 Extracted values:")
        android.util.Log.d("CustomLiveActivity", "  company: $company")
        android.util.Log.d("CustomLiveActivity", "  jobTitle: $jobTitle")
        android.util.Log.d("CustomLiveActivity", "  description: $description")
        android.util.Log.d("CustomLiveActivity", "  companyImageUrl: $companyImageUrl")
        android.util.Log.d("CustomLiveActivity", "  jobUrl: $jobUrl")
        android.util.Log.d("CustomLiveActivity", "  postedAt: $postedAt")

        // Update views for job
        updateRemoteViewsForJob(company, jobTitle, description, postedAt, companyImageUrl)

        // Create clickable PendingIntent with all job data
        // Important: Set action to prevent deep link handler from intercepting
        val clickIntent = Intent(appContext, MainActivity::class.java).apply {
            action = "com.example.lambda_test_live_activity.OPEN_JOB_DETAIL"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("from_live_activity", true)
            putExtra("company", company)
            putExtra("jobTitle", jobTitle)
            putExtra("description", description)
            putExtra("companyImageUrl", companyImageUrl)
            putExtra("job_url", jobUrl)
        }
        
        android.util.Log.d("CustomLiveActivity", "🔨 Creating PendingIntent with:")
        android.util.Log.d("CustomLiveActivity", "  company: $company")
        android.util.Log.d("CustomLiveActivity", "  jobTitle: $jobTitle")
        android.util.Log.d("CustomLiveActivity", "  action: ${clickIntent.action}")
        
        val clickPendingIntent = PendingIntent.getActivity(
            appContext,
            System.currentTimeMillis().toInt(), // Unique request code to ensure update
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        android.util.Log.d("CustomLiveActivity", "✅ PendingIntent created")
        
        // Make the entire RemoteViews clickable
        remoteViews.setOnClickPendingIntent(R.id.live_activity_root, clickPendingIntent)
        android.util.Log.d("CustomLiveActivity", "✅ Set click listener on RemoteViews")

        return notification
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setOngoing(true)
            .setContentTitle(company)
            .setContentIntent(clickPendingIntent) // Also set as content intent
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
