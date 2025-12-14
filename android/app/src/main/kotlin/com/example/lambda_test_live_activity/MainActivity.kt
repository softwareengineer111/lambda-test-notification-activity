package com.example.lambda_test_live_activity

import android.content.Intent
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import com.example.live_activities.LiveActivityManagerHolder

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.example.foreground/service"
    private var methodChannel: MethodChannel? = null
    private var pendingNotifAction: String? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
        methodChannel?.setMethodCallHandler { call, result ->
            when (call.method) {
                "startService" -> {
                    // Deprecated: ride foreground service removed
                    result.success("ignored")
                }
                "updateService" -> {
                    // Deprecated: ride foreground service removed
                    result.success("ignored")
                }
                "stopService" -> {
                    // Deprecated: ride foreground service removed
                    result.success("ignored")
                }
                "showCustomNotification" -> {
                    val args = call.arguments as? Map<String, String>
                    val title = args?.get("title") ?: "Ride"
                    val status = args?.get("status") ?: args?.get("text") ?: "Driver arriving..."
                    val eta = args?.get("eta") ?: "5 min"
                    // Deprecated: ride notification removed
                    // RideNotificationHelper.show(applicationContext, title, status, eta)
                    result.success(true)
                }
                "showJobNotification" -> {
                    val args = call.arguments as? Map<String, String>
                    val company = args?.get("company") ?: "Company"
                    val jobTitle = args?.get("jobTitle") ?: "Job Opening"
                    val description = args?.get("description") ?: "New position available"
                    val jobUrl = args?.get("jobUrl")
                    val imageUrl = args?.get("imageUrl")
                    JobNotificationHelper.show(applicationContext, company, jobTitle, description, jobUrl, imageUrl)
                    result.success(true)
                }
                "openUrl" -> {
                    val args = call.arguments as? Map<String, String>
                    val url = args?.get("url")
                    android.util.Log.d("MainActivity", "🧪 Testing deep link: $url")
                    if (url != null) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            result.success("Deep link opened: $url")
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Failed to open deep link", e)
                            result.error("ERROR", "Failed to open deep link: ${e.message}", null)
                        }
                    } else {
                        result.error("ERROR", "URL is null", null)
                    }
                }
                else -> result.notImplemented()
            }
        }

        // Wire custom Live Activity manager for Android RemoteViews integration
        LiveActivityManagerHolder.instance = CustomLiveActivityManager(this)

        // If there was a pending notification action (activity launched from notification before channel ready), forward it now
        pendingNotifAction?.let { action ->
            methodChannel?.invokeMethod("onNotificationAction", mapOf("action" to action))
            pendingNotifAction = null
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        android.util.Log.d("MainActivity", "========== handleNotificationIntent START ==========")
        if (intent == null) {
            android.util.Log.w("MainActivity", "Intent is null!")
            return
        }
        
        android.util.Log.d("MainActivity", "Processing intent:")
        android.util.Log.d("MainActivity", "  action: ${intent.action}")
        android.util.Log.d("MainActivity", "  from_live_activity: ${intent.getBooleanExtra("from_live_activity", false)}")
        android.util.Log.d("MainActivity", "  company: ${intent.getStringExtra("company")}")
        android.util.Log.d("MainActivity", "  jobTitle: ${intent.getStringExtra("jobTitle")}")
        
        intent?.getStringExtra("notif_action")?.let { action ->
            // If Dart side is ready, invoke method. Otherwise store it to send later.
            if (methodChannel != null) {
                methodChannel?.invokeMethod("onNotificationAction", mapOf("action" to action))
            } else {
                pendingNotifAction = action
            }
        }
        
        // PRIORITY 1: Handle Live Activity click with full job data (custom action)
        if (intent?.getBooleanExtra("from_live_activity", false) == true ||
            intent?.action == "com.example.lambda_test_live_activity.OPEN_JOB_DETAIL") {
            val company = intent.getStringExtra("company")
            val jobTitle = intent.getStringExtra("jobTitle")
            val description = intent.getStringExtra("description")
            val companyImageUrl = intent.getStringExtra("companyImageUrl")
            val jobUrl = intent.getStringExtra("job_url")
            
            android.util.Log.d("MainActivity", "🔗 Live Activity clicked with FULL DATA")
            android.util.Log.d("MainActivity", "  company: $company")
            android.util.Log.d("MainActivity", "  jobTitle: $jobTitle")
            android.util.Log.d("MainActivity", "  description: $description")
            android.util.Log.d("MainActivity", "  jobUrl: $jobUrl")
            
            // Pass job data to Flutter
            if (methodChannel != null && company != null && jobTitle != null) {
                methodChannel?.invokeMethod("openJobDetail", mapOf(
                    "company" to company,
                    "jobTitle" to jobTitle,
                    "description" to (description ?: "Ажлын байрны тайлбар"),
                    "companyImageUrl" to companyImageUrl,
                    "jobUrl" to jobUrl
                ))
            } else {
                android.util.Log.w("MainActivity", "MethodChannel not ready or job data missing")
            }
            return // Exit early to prevent deep link from being processed
        }
        
        // PRIORITY 2: Handle deep link scheme (lambda://job/123) from external sources
        if (intent?.action == android.content.Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null && uri.scheme == "lambda" && uri.host == "job") {
                val jobId = uri.pathSegments.firstOrNull()
                android.util.Log.d("MainActivity", "🔗 Deep link opened from EXTERNAL: lambda://job/$jobId")
                
                // For deep link, we don't have full job data, just navigate with job ID
                if (methodChannel != null && jobId != null) {
                    methodChannel?.invokeMethod("openJobDetailById", mapOf(
                        "jobId" to jobId
                    ))
                } else {
                    android.util.Log.w("MainActivity", "MethodChannel not ready or jobId missing")
                }
                return
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        android.util.Log.d("MainActivity", "========== onNewIntent called ==========")
        android.util.Log.d("MainActivity", "Intent action: ${intent.action}")
        android.util.Log.d("MainActivity", "Intent data: ${intent.data}")
        android.util.Log.d("MainActivity", "Intent extras: ${intent.extras?.keySet()?.joinToString()}")
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("MainActivity", "========== onResume called ==========")
        android.util.Log.d("MainActivity", "Intent action: ${intent?.action}")
        android.util.Log.d("MainActivity", "Intent data: ${intent?.data}")
        android.util.Log.d("MainActivity", "Intent extras: ${intent?.extras?.keySet()?.joinToString()}")
        // Handle initial intent if activity started from notification
        handleNotificationIntent(intent)
    }
}

