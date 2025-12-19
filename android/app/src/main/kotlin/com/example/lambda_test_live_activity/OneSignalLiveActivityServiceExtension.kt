package com.example.lambda_test_live_activity

import android.util.Log
import com.onesignal.notifications.INotificationReceivedEvent
import com.onesignal.notifications.INotificationServiceExtension
import org.json.JSONObject

class OneSignalLiveActivityServiceExtension : INotificationServiceExtension {

    override fun onNotificationReceived(event: INotificationReceivedEvent) {
        try {
            val context = event.context
            val notificationObj: Any = event.notification as Any

            val title = notificationObj.callStringGetter("getTitle")
            val body = notificationObj.callStringGetter("getBody")

            val additionalData = notificationObj.callJsonObjectGetter("getAdditionalData")

            val type = additionalData?.optString("type")
            val isJob = type == "job" || additionalData?.has("jobTitle") == true || additionalData?.has("company") == true

            if (!isJob) {
                return
            }

            // Stop OneSignal from displaying its default notification in background/terminated.
            event.preventDefault()

            val company = additionalData?.optString("company")?.takeIf { it.isNotBlank() }
                ?: title
                ?: "Company"

            val jobTitle = additionalData?.optString("jobTitle")?.takeIf { it.isNotBlank() }
                ?: body
                ?: "Job Opening"

            val description = additionalData?.optString("description")?.takeIf { it.isNotBlank() }
                ?: "Албан тушаал зарлагдлаа"

            val jobUrl = additionalData?.optString("jobUrl")?.takeIf { it.isNotBlank() }
            val companyImageUrl = additionalData?.optString("companyImageUrl")
                ?.takeIf { it.isNotBlank() }
                ?: additionalData?.optString("imageUrl")?.takeIf { it.isNotBlank() }

            val postedAtMillis = additionalData?.optLong("postedAt")?.takeIf { it > 0 }

            Log.d("OneSignalExt", "Showing live_activity notification for job: $company / $jobTitle")
            LiveActivityNotificationHelper.show(
                context = context,
                company = company,
                jobTitle = jobTitle,
                description = description,
                jobUrl = jobUrl,
                companyImageUrl = companyImageUrl,
                postedAtMillis = postedAtMillis,
            )
        } catch (e: Exception) {
            Log.e("OneSignalExt", "Failed to render custom live_activity notification", e)
        }
    }

    private fun Any.callStringGetter(methodName: String): String? {
        return runCatching { this.javaClass.getMethod(methodName).invoke(this) as? String }.getOrNull()
    }

    private fun Any.callJsonObjectGetter(methodName: String): JSONObject? {
        return runCatching { this.javaClass.getMethod(methodName).invoke(this) as? JSONObject }.getOrNull()
    }
}
