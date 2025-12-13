package com.example.lambda_test_live_activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.plugin.common.MethodChannel

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val engine: FlutterEngine? = FlutterEngineCache.getInstance().get("my_engine")
        if (engine != null) {
            val channel = MethodChannel(engine.dartExecutor.binaryMessenger, "com.example.foreground/service")
            when (action) {
                "ACTION_STOP" -> channel.invokeMethod("onNotificationAction", mapOf("action" to "stop"))
                "ACTION_CALL" -> channel.invokeMethod("onNotificationAction", mapOf("action" to "call"))
                "ACTION_NAVIGATE" -> channel.invokeMethod("onNotificationAction", mapOf("action" to "navigate"))
            }
        } else {
            if (action == "ACTION_STOP") {
                RideNotificationHelper.cancel(context)
            }
        }
    }
}