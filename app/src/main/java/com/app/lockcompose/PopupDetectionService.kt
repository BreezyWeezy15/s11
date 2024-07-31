package com.app.lockcompose

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class PopupDetectionService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        serviceInfo = info
        Log.d("PopupDetectionService", "Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event != null) {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

                val packageName = event.packageName?.toString()
                Log.d("PopupDetectionService", "Window changed: $packageName")

                // Check if the current window belongs to an app you want to lock
                val appsToLock = listOf("com.android.settings", "com.example.somepopupapp")
                if (appsToLock.contains(packageName)) {
                    showLockScreen()
                }
            }
        }
    }

    override fun onInterrupt() {
        // Handle any interruptions
    }

    private fun showLockScreen() {
        try {
            val lockIntent = Intent(this, LockScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(lockIntent)
            Log.d("PopupDetectionService", "Lock screen shown.")
        } catch (e: Exception) {
            Log.e("PopupDetectionService", "Error showing lock screen", e)
        }
    }
}
