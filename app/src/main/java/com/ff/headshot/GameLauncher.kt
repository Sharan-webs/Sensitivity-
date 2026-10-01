package com.ff.headshot

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object GameLauncher {

    private val FF_PACKAGES = listOf(
        "com.dts.freefireth",
        "com.dts.freefiremax"
    )

    fun launch(context: Context): Boolean {
        for (pkg in FF_PACKAGES) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                context.startActivity(intent)
                return true
            }
        }
        return false
    }

    fun getInstalledPackage(context: Context): String? {
        return FF_PACKAGES.firstOrNull { pkg ->
            try {
                context.packageManager.getPackageInfo(pkg, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    fun getGameName(pkg: String) = when (pkg) {
        "com.dts.freefireth" -> "Free Fire"
        "com.dts.freefiremax" -> "Free Fire MAX"
        else -> "Free Fire"
    }
}
