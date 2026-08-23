package com.tomasrepcik.voidlauncher.data.source

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

interface InstalledAppsDataSource {
    fun observeInstalledApps(): Flow<List<InstalledApp>>
    suspend fun getInstalledApp(appKey: AppKey): InstalledApp?
}

class PackageManagerInstalledAppsDataSource(
    private val packageManager: PackageManager,
    private val launcherPackageName: String,
    private val packageChanges: Flow<Unit>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : InstalledAppsDataSource {
    override fun observeInstalledApps(): Flow<List<InstalledApp>> = packageChanges
        .onStart { emit(Unit) }
        .map {
            withContext(ioDispatcher) { loadInstalledApps() }
        }

    override suspend fun getInstalledApp(appKey: AppKey): InstalledApp? = withContext(ioDispatcher) {
        try {
            loadInstalledApp(appKey)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun loadInstalledApps(): List<InstalledApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        return resolveInfos
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                resolveInstalledApp(activityInfo)
            }
            .sortedBy(InstalledApp::sortLabel)
    }

    private fun loadInstalledApp(appKey: AppKey): InstalledApp? {
        val activityInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getActivityInfo(
                ComponentName(appKey.packageName, appKey.activityName),
                PackageManager.ComponentInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getActivityInfo(
                ComponentName(appKey.packageName, appKey.activityName),
                0,
            )
        }
        return resolveInstalledApp(activityInfo)
    }

    private fun resolveInstalledApp(activityInfo: ActivityInfo): InstalledApp? {
        if (activityInfo.packageName == launcherPackageName) {
            return null
        }
        val label = activityInfo.loadLabel(packageManager).toString().trim()
            .takeIf { it.isNotEmpty() }
            ?: activityInfo.name.substringAfterLast('.')
        return InstalledApp(
            key = AppKey(
                packageName = activityInfo.packageName,
                activityName = activityInfo.name,
            ),
            label = label,
            sortLabel = label.lowercase(),
        )
    }
}

fun Context.observeInstalledAppChanges(): Flow<Unit> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            trySend(Unit)
        }
    }

    val filter = IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_ADDED)
        addAction(Intent.ACTION_PACKAGE_CHANGED)
        addAction(Intent.ACTION_PACKAGE_REMOVED)
        addAction(Intent.ACTION_PACKAGE_REPLACED)
        addDataScheme("package")
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    } else {
        @Suppress("DEPRECATION")
        registerReceiver(receiver, filter)
    }

    awaitClose { unregisterReceiver(receiver) }
}
