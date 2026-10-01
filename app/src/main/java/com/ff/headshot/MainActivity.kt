package com.ff.headshot

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ff.headshot.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        wireUI()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun wireUI() {
        b.btnStart.setOnClickListener { startPanel() }
        b.btnStop.setOnClickListener { stopPanel() }
    }

    private fun startPanel() {
        // Start the floating overlay service
        val intent = Intent(this, FloatingMenuService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // Launch Free Fire
        val launched = GameLauncher.launch(this)
        if (!launched) {
            Toast.makeText(
                this,
                "Free Fire not found! Install Free Fire or Free Fire MAX first.",
                Toast.LENGTH_LONG
            ).show()
        }

        // Minimize — go behind game
        moveTaskToBack(true)
        refreshStatus()
    }

    private fun stopPanel() {
        stopService(Intent(this, FloatingMenuService::class.java))
        HeadshotService.isHeadshotEnabled = false
        refreshStatus()
    }

    private fun refreshStatus() {
        // Game detection
        val pkg = GameLauncher.getInstalledPackage(this)
        if (pkg != null) {
            b.tvGameStatus.text = "✓ ${GameLauncher.getGameName(pkg)} Detected"
            b.tvGameStatus.setTextColor(Color.parseColor("#00FF88"))
        } else {
            b.tvGameStatus.text = "✗ Free Fire Not Installed"
            b.tvGameStatus.setTextColor(Color.parseColor("#FF4444"))
        }

        // Shizuku
        b.tvShizukuMain.text = if (ShizukuHelper.hasPermission())
            "✓ Shizuku Active" else "✗ Shizuku Inactive (optional)"
        b.tvShizukuMain.setTextColor(
            if (ShizukuHelper.hasPermission()) Color.parseColor("#00FF88")
            else Color.parseColor("#AAAAAA")
        )

        // Panel status
        val running = FloatingMenuService.isRunning
        b.tvPanelStatus.text = if (running) "● Panel: Active" else "● Panel: Stopped"
        b.tvPanelStatus.setTextColor(
            if (running) Color.parseColor("#00FF88") else Color.parseColor("#FF4444")
        )
    }
}
