package com.ff.headshot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility service that injects headshot gestures into Free Fire.
 *
 * Mechanism: Uses dispatchGesture() to inject tap events at head-level Y
 * coordinates. Free Fire renders enemies on screen and maps screen Y to
 * vertical aim. By offsetting touch Y upward by HEAD_OFFSET_PX pixels
 * relative to where the user fires, the aim snaps to head level.
 *
 * HEAD_OFFSET_PX is calibrated for 1920x1080 resolution.
 * Adjust for higher-res devices (2400x1080 etc.) proportionally.
 */
class HeadshotService : AccessibilityService() {

    companion object {
        const val ACTION_TOGGLE = "com.ff.headshot.TOGGLE_HEADSHOT"
        const val EXTRA_ENABLED = "enabled"

        @Volatile var isHeadshotEnabled = false

        // Pixels above the user's tap to inject the headshot tap
        // 1080p baseline — adjust for your screen height
        private const val HEAD_OFFSET_PX = -145

        // Slight X drift correction (center bias)
        private const val X_CORRECTION = 0
    }

    private val toggleReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == ACTION_TOGGLE) {
                isHeadshotEnabled = intent.getBooleanExtra(EXTRA_ENABLED, false)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(toggleReceiver, IntentFilter(ACTION_TOGGLE), RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(toggleReceiver, IntentFilter(ACTION_TOGGLE))
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Monitor when Free Fire is in foreground
        val pkg = event?.packageName?.toString() ?: return
        if (pkg != "com.dts.freefireth" && pkg != "com.dts.freefiremax") return
        // Game is active — gesture injection is live when toggle is ON
    }

    /**
     * Inject a headshot tap at the head-corrected position.
     * Called from FloatingMenuService when headshot is active and
     * the user fires (detected via screen region monitoring).
     */
    fun injectHeadshotGesture(touchX: Float, touchY: Float) {
        if (!isHeadshotEnabled) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        val correctedX = touchX + X_CORRECTION
        val correctedY = touchY + HEAD_OFFSET_PX

        // Clamp Y so it doesn't go off-screen top
        val finalY = correctedY.coerceAtLeast(0f)

        val path = Path().apply { moveTo(correctedX, finalY) }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 60L))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) { /* done */ }
            override fun onCancelled(gestureDescription: GestureDescription?) { /* retry optional */ }
        }, null)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(toggleReceiver) } catch (_: Exception) {}
        isHeadshotEnabled = false
    }
}
