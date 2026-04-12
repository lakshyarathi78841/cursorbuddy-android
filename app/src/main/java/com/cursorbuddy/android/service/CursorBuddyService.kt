package com.cursorbuddy.android.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cursorbuddy.android.overlay.OverlayManager
import com.cursorbuddy.android.ui.MainActivity

class CursorBuddyService : Service() {

    companion object {
        const val CHANNEL_ID = "cursorbuddy_service"
        const val NOTIFICATION_ID = 1001
        
        var instance: CursorBuddyService? = null
            private set
        
        var isRunning: Boolean = false
            private set
        
        fun start(context: Context) {
            val intent = Intent(context, CursorBuddyService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, CursorBuddyService::class.java))
        }
    }

    var overlayManager: OverlayManager? = null
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        isRunning = true
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // Initialize overlay
        overlayManager = OverlayManager(this).also { om ->
            // Load API key from prefs
            val prefs = getSharedPreferences("cursorbuddy_prefs", MODE_PRIVATE)
            val apiKey = prefs.getString("claude_api_key", "") ?: ""
            if (apiKey.isNotEmpty()) {
                om.setApiKey(apiKey)
            }
            om.show()
            
            // Poll for accessibility service connection (it may start after us)
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            fun checkAccessibility(attempts: Int) {
                if (CursorBuddyAccessibilityService.isRunning) {
                    android.util.Log.d("CursorBuddy", "Accessibility service confirmed connected in same PID")
                } else if (attempts < 20) {
                    handler.postDelayed({ checkAccessibility(attempts + 1) }, 500)
                } else {
                    android.util.Log.w("CursorBuddy", "Accessibility service not connected after 10s — user may need to re-enable it")
                }
            }
            checkAccessibility(0)
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        overlayManager?.hide()
        overlayManager = null
        instance = null
        isRunning = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CursorBuddy",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "CursorBuddy is ready to help"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CursorBuddy")
            .setContentText("Tap the floating bubble to get help with any app")
            .setSmallIcon(android.R.drawable.ic_menu_help)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
