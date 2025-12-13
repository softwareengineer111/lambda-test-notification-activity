package com.example.lambda_test_live_activity

import android.app.Application
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationLifecycleListener
import com.onesignal.notifications.INotificationWillDisplayEvent

class MyApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize OneSignal
        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        OneSignal.initWithContext(this, "be13a59a-95c4-43c5-b104-43d3b3f1921d")
        
        // Set notification will show in foreground handler
        OneSignal.Notifications.addForegroundLifecycleListener(object : INotificationLifecycleListener {
            override fun onWillDisplay(event: INotificationWillDisplayEvent) {
                // Get notification data
                val notification = event.notification
                val additionalData = notification.additionalData
                
                // Show custom notification
                OneSignalNotificationHandler.handleNotification(
                    applicationContext,
                    notification.title,
                    notification.body,
                    additionalData
                )
                
                // Prevent default OneSignal notification from showing
                event.preventDefault()
            }
        })
    }
}
