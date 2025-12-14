package com.example.lambda_test_live_activity

import android.content.Context

// Deprecated helper: ride notifications removed; no-op implementations
object RideNotificationHelper {
    fun ensureChannel(context: Context) {}
    fun show(context: Context, title: String, status: String, eta: String) {}
    fun update(context: Context, status: String, eta: String) {}
    fun cancel(context: Context) {}
}