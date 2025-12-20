package com.example.lambda_test_live_activity

import android.app.Application
import android.util.Log
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationLifecycleListener
import com.onesignal.notifications.INotificationWillDisplayEvent

class MyApplication : Application() {

    companion object {
        // Must match the OneSignal App ID used in Flutter (lib/main.dart)
        private const val ONE_SIGNAL_APP_ID = "be13a59a-95c4-43c5-b104-43d3b3f1921d"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize OneSignal
        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        OneSignal.initWithContext(this, ONE_SIGNAL_APP_ID)

        // Foreground: only intercept job notifications and render our custom RemoteViews.
        OneSignal.Notifications.addForegroundLifecycleListener(object : INotificationLifecycleListener {
            override fun onWillDisplay(event: INotificationWillDisplayEvent) {
                val notification = event.notification
                val additionalData = notification.additionalData

                val type = additionalData?.optString("type")
                val isJob = type == "job" || additionalData?.has("jobTitle") == true || additionalData?.has("company") == true
                if (!isJob) return

                Log.d("MyApplication", "Foreground job notification intercepted")
                event.preventDefault()

                val company = additionalData?.optString("company")?.takeIf { it.isNotBlank() }
                    ?: notification.title
                    ?: "Company"
                val jobTitle = additionalData?.optString("jobTitle")?.takeIf { it.isNotBlank() }
                    ?: notification.body
                    ?: "Job Opening"
                val description = additionalData?.optString("description")?.takeIf { it.isNotBlank() }
                    ?: "Албан тушаал зарлагдлаа"
                val jobUrl = additionalData?.optString("jobUrl")?.takeIf { it.isNotBlank() }
                val companyImageUrl = additionalData?.optString("companyImageUrl")?.takeIf { it.isNotBlank() }
                    ?: additionalData?.optString("imageUrl")?.takeIf { it.isNotBlank() }
                val postedAtMillis = additionalData?.optLong("postedAt")?.takeIf { it > 0 }

                LiveActivityNotificationHelper.show(
                    context = applicationContext,
                    company = company,
                    jobTitle = jobTitle,
                    description = description,
                    jobUrl = jobUrl,
                    companyImageUrl = companyImageUrl,
                    postedAtMillis = postedAtMillis,
                )
            }
        })

        // Background/terminated notifications are customized via the OneSignal
        // NotificationServiceExtension registered in AndroidManifest.xml.
        Log.d("MyApplication", "OneSignal initialized")
    }
}
