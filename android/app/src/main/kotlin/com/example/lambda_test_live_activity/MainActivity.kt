package com.example.lambda_test_live_activity

import android.content.Intent
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

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
                    val title = call.argument<String>("title") ?: "Ride"
                    val text = call.argument<String>("text") ?: "Starting..."
                    val intent = Intent(context, ForegroundNotificationService::class.java)
                    intent.action = ForegroundNotificationService.ACTION_START
                    intent.putExtra(ForegroundNotificationService.EXTRA_TITLE, title)
                    intent.putExtra(ForegroundNotificationService.EXTRA_TEXT, text)
                    context.startForegroundService(intent)
                    result.success("started")
                }
                "updateService" -> {
                    val title = call.argument<String>("title") ?: "Ride"
                    val text = call.argument<String>("text") ?: "Update"
                    val intent = Intent(context, ForegroundNotificationService::class.java)
                    intent.action = ForegroundNotificationService.ACTION_UPDATE
                    intent.putExtra(ForegroundNotificationService.EXTRA_TITLE, title)
                    intent.putExtra(ForegroundNotificationService.EXTRA_TEXT, text)
                    context.startForegroundService(intent)
                    result.success("updated")
                }
                "stopService" -> {
                    val intent = Intent(context, ForegroundNotificationService::class.java)
                    intent.action = ForegroundNotificationService.ACTION_STOP
                    context.startService(intent)
                    result.success("stopped")
                }
                else -> result.notImplemented()
            }
        }

        // If there was a pending notification action (activity launched from notification before channel ready), forward it now
        pendingNotifAction?.let { action ->
            methodChannel?.invokeMethod("onNotificationAction", mapOf("action" to action))
            pendingNotifAction = null
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        intent?.getStringExtra("notif_action")?.let { action ->
            // If Dart side is ready, invoke method. Otherwise store it to send later.
            if (methodChannel != null) {
                methodChannel?.invokeMethod("onNotificationAction", mapOf("action" to action))
            } else {
                pendingNotifAction = action
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Handle initial intent if activity started from notification
        handleNotificationIntent(intent)
    }
}

