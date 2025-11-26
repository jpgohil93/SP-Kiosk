package com.screenpulse.kiosk.core.remote

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class RemoteControlService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("RemoteControlService", "Service Connected")
        
        RemoteClient.onRemoteInput = { keyCode ->
            performRemoteAction(keyCode)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op
    }

    override fun onInterrupt() {
        Log.d("RemoteControlService", "Service Interrupted")
    }

    private fun performRemoteAction(keyCode: Int) {
        Log.d("RemoteControlService", "Performing action: $keyCode")
        when (keyCode) {
            KeyEvent.KEYCODE_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            KeyEvent.KEYCODE_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            KeyEvent.KEYCODE_DPAD_UP -> simulateDPAD(0f, -1f)
            KeyEvent.KEYCODE_DPAD_DOWN -> simulateDPAD(0f, 1f)
            KeyEvent.KEYCODE_DPAD_LEFT -> simulateDPAD(-1f, 0f)
            KeyEvent.KEYCODE_DPAD_RIGHT -> simulateDPAD(1f, 0f)
            KeyEvent.KEYCODE_DPAD_CENTER -> simulateTap()
            // Add more mappings as needed
        }
    }

    private fun simulateTap() {
        // Simulate tap at center of screen (approximate)
        val path = Path()
        path.moveTo(960f, 540f) // 1080p center
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun simulateDPAD(dx: Float, dy: Float) {
        // AccessibilityService cannot easily inject raw key events without root/signature.
        // We can simulate swipes for navigation if the UI supports it, 
        // or just rely on global actions.
        // For standard Android TV, DPAD is hard to simulate via Accessibility without INJECT_EVENTS.
        // However, we can try to find focus and traverse? No, that's complex.
        
        // Fallback: If we are the kiosk app, we might handle these in the Activity directly.
        // But for global control, we are limited.
        // For this prototype, we will just log it or try a small swipe which might move focus in some apps.
    }
}
