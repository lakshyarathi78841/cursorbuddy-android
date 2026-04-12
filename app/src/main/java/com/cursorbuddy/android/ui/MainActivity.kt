package com.cursorbuddy.android.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cursorbuddy.android.R
import com.cursorbuddy.android.service.CursorBuddyService
import com.cursorbuddy.android.service.ScreenCapturer

class MainActivity : AppCompatActivity() {

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST = 1000
        private const val SCREEN_CAPTURE_REQUEST = 1001
        private const val MIC_PERMISSION_REQUEST = 1002
        private const val PREFS_NAME = "cursorbuddy_prefs"
        private const val KEY_API_KEY = "claude_api_key"
    }

    private lateinit var overlayStatus: TextView
    private lateinit var accessibilityStatus: TextView
    private lateinit var screenCaptureStatus: TextView
    private lateinit var overlayButton: Button
    private lateinit var accessibilityButton: Button
    private lateinit var screenCaptureButton: Button
    private lateinit var startButton: Button
    private lateinit var statusText: TextView
    private lateinit var apiKeyInput: EditText
    private lateinit var saveApiKeyButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        overlayStatus = findViewById(R.id.overlayStatus)
        accessibilityStatus = findViewById(R.id.accessibilityStatus)
        screenCaptureStatus = findViewById(R.id.screenCaptureStatus)
        overlayButton = findViewById(R.id.overlayButton)
        accessibilityButton = findViewById(R.id.accessibilityButton)
        screenCaptureButton = findViewById(R.id.screenCaptureButton)
        startButton = findViewById(R.id.startButton)
        statusText = findViewById(R.id.statusText)
        apiKeyInput = findViewById(R.id.apiKeyInput)
        saveApiKeyButton = findViewById(R.id.saveApiKeyButton)

        overlayButton.setOnClickListener { requestOverlayPermission() }
        accessibilityButton.setOnClickListener { openAccessibilitySettings() }
        screenCaptureButton.setOnClickListener { requestScreenCapture() }
        startButton.setOnClickListener { startService() }
        saveApiKeyButton.setOnClickListener { saveApiKey() }
        
        // Load saved API key
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedKey = prefs.getString(KEY_API_KEY, "") ?: ""
        if (savedKey.isNotEmpty()) {
            apiKeyInput.setText(savedKey)
        }
        
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val hasOverlay = Settings.canDrawOverlays(this)
        val hasAccessibility = isAccessibilityServiceEnabled()
        val hasScreenCapture = ScreenCapturer.hasPermission()
        val hasApiKey = apiKeyInput.text.toString().trim().startsWith("sk-")
        
        overlayStatus.text = if (hasOverlay) "✅ Granted" else "❌ Required"
        overlayButton.isEnabled = !hasOverlay
        overlayButton.text = if (hasOverlay) "Overlay Permission Granted" else "Grant Overlay Permission"
        
        accessibilityStatus.text = if (hasAccessibility) "✅ Enabled" else "❌ Required"
        accessibilityButton.isEnabled = !hasAccessibility
        accessibilityButton.text = if (hasAccessibility) "Accessibility Service Enabled" else "Enable Accessibility Service"
        
        screenCaptureStatus.text = if (hasScreenCapture) "✅ Granted" else "⚡ Optional (for AI vision)"
        screenCaptureButton.isEnabled = !hasScreenCapture
        screenCaptureButton.text = if (hasScreenCapture) "Screen Capture Granted" else "Grant Screen Capture"
        
        val canStart = hasOverlay && hasAccessibility
        startButton.isEnabled = canStart
        startButton.alpha = if (canStart) 1.0f else 0.5f
        
        if (CursorBuddyService.isRunning) {
            startButton.text = "Stop CursorBuddy"
            statusText.text = if (hasApiKey) "CursorBuddy is running with AI! Look for the cursor."
                              else "CursorBuddy is running (no AI key — basic mode). Look for the cursor."
        } else {
            startButton.text = "Start CursorBuddy"
            statusText.text = if (canStart) "Ready to go! Tap Start to begin."
                              else "Grant the permissions above to get started."
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST)
    }

    private fun openAccessibilitySettings() {
        Toast.makeText(this, "Find CursorBuddy and enable it", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun requestScreenCapture() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), SCREEN_CAPTURE_REQUEST)
    }

    private fun saveApiKey() {
        val key = apiKeyInput.text.toString().trim()
        if (key.isEmpty()) {
            Toast.makeText(this, "Enter your Claude API key", Toast.LENGTH_SHORT).show()
            return
        }
        
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_API_KEY, key).apply()
        
        // Update running service if active
        CursorBuddyService.instance?.overlayManager?.setApiKey(key)
        
        Toast.makeText(this, "API key saved! AI mode enabled.", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    private fun requestMicPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), MIC_PERMISSION_REQUEST)
        }
    }

    private fun startService() {
        // Request mic permission if not granted
        requestMicPermission()
        
        if (CursorBuddyService.isRunning) {
            CursorBuddyService.stop(this)
        } else {
            CursorBuddyService.start(this)
            
            // Pass API key to service after a brief delay
            window.decorView.postDelayed({
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val key = prefs.getString(KEY_API_KEY, "") ?: ""
                if (key.isNotEmpty()) {
                    CursorBuddyService.instance?.overlayManager?.setApiKey(key)
                }
            }, 1000)
        }
        window.decorView.postDelayed({ updateUI() }, 500)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == packageName
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MIC_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone enabled for voice input", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            OVERLAY_PERMISSION_REQUEST -> updateUI()
            SCREEN_CAPTURE_REQUEST -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    ScreenCapturer.resultCode = resultCode
                    ScreenCapturer.resultData = data
                    Toast.makeText(this, "Screen capture enabled!", Toast.LENGTH_SHORT).show()
                }
                updateUI()
            }
        }
    }
}
