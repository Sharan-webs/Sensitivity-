package com.ff.headshot

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.ff.headshot.databinding.ActivityPermissionsBinding
import rikka.shizuku.Shizuku

class PermissionsActivity : AppCompatActivity() {

    private lateinit var b: ActivityPermissionsBinding

    private val runtimeLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshAll() }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(b.root)
        wireButtons()
        refreshAll()
    }

    override fun onResume() {
        super.onResume()
        refreshAll()
    }

    // ── Button wiring ─────────────────────────────────────────────────────────

    private fun wireButtons() {
        b.btnGrantOverlay.setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
        }
        b.btnGrantInstall.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName")))
        }
        b.btnGrantFiles.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")))
        }
        b.btnGrantUsage.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        b.btnGrantAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        b.btnGrantShizuku.setOnClickListener {
            try {
                if (!Shizuku.isPreV11()) Shizuku.requestPermission(ShizukuHelper.REQUEST_CODE)
                else b.tvShizukuStatus.text = "Install Shizuku from Play Store first"
            } catch (e: Exception) {
                b.tvShizukuStatus.text = "Shizuku not installed — get it from Play Store"
            }
        }
        b.btnGrantRuntime.setOnClickListener {
            runtimeLauncher.launch(arrayOf(
                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
        b.btnProceed.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    // ── Refresh status ────────────────────────────────────────────────────────

    private fun refreshAll() {
        val overlay = overlayOk()
        val install = installOk()
        val files = filesOk()
        val usage = usageOk()
        val acc = accessibilityOk()
        val shizuku = ShizukuHelper.hasPermission()
        val runtime = runtimeOk()

        setStatus(b.tvOverlayStatus, b.btnGrantOverlay, overlay, "✓ Granted", "✗ Required — CRITICAL")
        setStatus(b.tvInstallStatus, b.btnGrantInstall, install)
        setStatus(b.tvFilesStatus, b.btnGrantFiles, files)
        setStatus(b.tvUsageStatus, b.btnGrantUsage, usage)
        setStatus(b.tvAccessibilityStatus, b.btnGrantAccessibility, acc, "✓ Service Enabled", "✗ Enable in Accessibility Settings")
        setStatus(b.tvShizukuStatus, b.btnGrantShizuku,
            shizuku,
            "✓ Shizuku Active",
            if (ShizukuHelper.isRunning()) "Running — tap GRANT" else "Install Shizuku from Play Store")
        setStatus(b.tvRuntimeStatus, b.btnGrantRuntime, runtime)

        b.btnProceed.isEnabled = overlay
        b.btnProceed.alpha = if (overlay) 1f else 0.4f
    }

    private fun setStatus(
        tv: android.widget.TextView,
        btn: android.widget.Button,
        granted: Boolean,
        grantedText: String = "✓ Granted",
        requiredText: String = "✗ Required"
    ) {
        tv.text = if (granted) grantedText else requiredText
        tv.setTextColor(
            if (granted) android.graphics.Color.parseColor("#00FF88")
            else android.graphics.Color.parseColor("#FF4444")
        )
        btn.isEnabled = !granted
        btn.alpha = if (granted) 0.5f else 1f
        btn.text = if (granted) "✓" else "GRANT"
    }

    // ── Permission checks ─────────────────────────────────────────────────────

    private fun overlayOk() = Settings.canDrawOverlays(this)

    private fun installOk() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        packageManager.canRequestPackageInstalls() else true

    private fun filesOk() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        Environment.isExternalStorageManager() else true

    private fun usageOk(): Boolean {
        val ops = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        else
            @Suppress("DEPRECATION") ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun accessibilityOk(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == packageName }
    }

    private fun runtimeOk() = checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED
}
