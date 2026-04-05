package com.tomasrepcik.voidlauncher.data.source

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface InstalledAppsDataSource {
    fun observeInstalledApps(): Flow<List<InstalledApp>>
}

class PackageManagerInstalledAppsDataSource(
    private val context: Context,
) : InstalledAppsDataSource {

    private val packageManager: PackageManager = context.packageManager

    override fun observeInstalledApps(): Flow<List<InstalledApp>> = callbackFlow {
        fun emitApps() {
            trySend(loadInstalledApps())
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                emitApps()
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        emitApps()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    private fun loadInstalledApps(): List<InstalledApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        return resolveInfos
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                if (activityInfo.packageName == context.packageName) {
                    return@mapNotNull null
                }
                val label = resolveInfo.loadLabel(packageManager).toString().trim()
                    .takeIf { it.isNotEmpty() }
                    ?: activityInfo.name.substringAfterLast('.')
                InstalledApp(
                    key = AppKey(
                        packageName = activityInfo.packageName,
                        activityName = activityInfo.name,
                    ),
                    label = label,
                    sortLabel = label.lowercase(),
                )
            }
            .sortedBy(InstalledApp::sortLabel)
    }
}
