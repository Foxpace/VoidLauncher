package com.tomasrepcik.voidlauncher.domain.action

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

internal class AndroidAppLauncher(
    private val packageManager: PackageManager,
    private val startActivity: (Intent) -> Unit,
) {
    fun open(intent: Intent): Boolean {
        if (intent.resolveActivity(packageManager) == null) return false
        startActivity(intent)
        return true
    }

    fun installedApplicationFlags(packageName: String): Int? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0),
            ).flags
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0).flags
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}
