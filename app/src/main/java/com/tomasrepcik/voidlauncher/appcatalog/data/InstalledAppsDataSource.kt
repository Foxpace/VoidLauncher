package com.tomasrepcik.voidlauncher.appcatalog.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import com.tomasrepcik.voidlauncher.launcher.AppKey
import com.tomasrepcik.voidlauncher.launcher.InstalledApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class InstalledAppsDataSource(
    private val packageManager: PackageManager,
    private val launcherPackageName: String,
    private val packageChanges: Flow<String>,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val nextPackageRevision = AtomicLong()
    private val packageRevisions = ConcurrentHashMap<String, Long>()

    fun observeInstalledApps(): Flow<List<InstalledApp>> = packageChanges
        .map<String, String?> { packageName -> packageName }
        .onStart { emit(null) }
        .map { changedPackageName ->
            changedPackageName?.let { packageName ->
                packageRevisions[packageName] = nextPackageRevision.incrementAndGet()
            }
            withContext(ioDispatcher) { loadInstalledApps() }
        }

    suspend fun getInstalledApp(appKey: AppKey): InstalledApp? = withContext(ioDispatcher) {
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
            packageRevision = packageRevisions[activityInfo.packageName] ?: 0,
        )
    }
}

fun Context.observeInstalledAppChanges(): Flow<String> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val packageName = intent?.data?.schemeSpecificPart ?: return
            trySend(packageName)
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
        registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
    } else {
        @Suppress("DEPRECATION")
        registerReceiver(receiver, filter)
    }

    awaitClose { unregisterReceiver(receiver) }
}
