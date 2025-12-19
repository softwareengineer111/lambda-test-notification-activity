package com.example.lambda_test_live_activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import io.flutter.embedding.android.FlutterActivity

class MainActivity: FlutterActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleLiveActivityIntent(intent)
    }

    private fun handleLiveActivityIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("from_live_activity", false) == true) {
            val jobUrl = intent.getStringExtra("job_url")
            if (jobUrl != null) {
                Log.d("MainActivity", "Live Activity clicked with jobUrl: $jobUrl")
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(jobUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(browserIntent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to open URL: $jobUrl", e)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLiveActivityIntent(intent)
    }
}

