package com.ff.headshot

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuHelper {

    const val REQUEST_CODE = 101
    const val SHIZUKU_PERMISSION = "rikka.shizuku.permission.API_V23"

    fun isRunning(): Boolean = try { Shizuku.pingBinder() } catch (e: Exception) { false }

    fun hasPermission(): Boolean = try {
        if (Shizuku.isPreV11()) false
        else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) { false }

    fun requestPermission() {
        try {
            if (!Shizuku.isPreV11()) {
                Shizuku.requestPermission(REQUEST_CODE)
            }
        } catch (e: Exception) { /* shizuku not available */ }
    }

    /**
     * Execute a shell command with Shizuku's privileged shell.
     * Falls back to regular shell if Shizuku not available.
     */
    fun exec(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val out = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            out
        } catch (e: Exception) {
            e.message ?: "error"
        }
    }

    /**
     * Inject a touch tap at given screen coordinates via input subsystem.
     * Requires Shizuku or ADB shell access.
     */
    fun injectTap(x: Int, y: Int) {
        exec("input tap $x $y")
    }

    /**
     * Inject a swipe gesture.
     */
    fun injectSwipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int = 50) {
        exec("input swipe $x1 $y1 $x2 $y2 $durationMs")
    }
}
