package com.tomasrepcik.voidlauncher.domain.action

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.net.toUri
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal interface LauncherActionPlatform {
    fun launchInstalledApp(app: InstalledApp): Boolean
    fun openShortcut(shortcut: ResolvedShortcut): Boolean
    fun openWebSearch(query: String): Boolean
    fun openBrowserSearch(query: String): Boolean
    fun openPlayStore(query: String): Boolean
    fun openPlayStoreWebsite(query: String): Boolean
    fun openMaps(query: String): Boolean
    fun openMapsWebsite(query: String): Boolean
    fun applicationFlags(packageName: String): Int?
    fun openUninstaller(packageName: String): Boolean
    fun openAppInfo(packageName: String): Boolean
}

internal class AndroidLauncherActionPlatform(
    private val context: Context,
) : LauncherActionPlatform {
    private val packageManager = context.packageManager

    override fun launchInstalledApp(app: InstalledApp): Boolean = context.startResolved(
        Intent().apply {
            component = ComponentName(app.key.packageName, app.key.activityName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )

    override fun openShortcut(shortcut: ResolvedShortcut): Boolean {
        val intent = when (shortcut.selection) {
            is ShortcutSelection.AppShortcut -> {
                val app = shortcut.installedApp ?: return false
                Intent().apply {
                    component = ComponentName(app.key.packageName, app.key.activityName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            ShortcutSelection.SystemCamera -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ShortcutSelection.SystemContacts -> Intent(
                Intent.ACTION_VIEW,
                ContactsContract.Contacts.CONTENT_URI,
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return context.startResolved(intent)
    }

    override fun openWebSearch(query: String): Boolean = context.startResolved(
        Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )

    override fun openBrowserSearch(query: String): Boolean {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        return context.startResolved(webIntent("https://www.google.com/search?q=$encodedQuery"))
    }

    override fun openPlayStore(query: String): Boolean = context.startResolved(
        webIntent("market://search?q=${Uri.encode(query)}&c=apps"),
    )

    override fun openPlayStoreWebsite(query: String): Boolean = context.startResolved(
        webIntent("https://play.google.com/store/search?q=${Uri.encode(query)}&c=apps"),
    )

    override fun openMaps(query: String): Boolean = context.startResolved(
        webIntent("geo:0,0?q=${Uri.encode(query)}").apply {
            `package` = "com.google.android.apps.maps"
        },
    )

    override fun openMapsWebsite(query: String): Boolean = context.startResolved(
        webIntent("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"),
    )

    override fun applicationFlags(packageName: String): Int? = try {
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

    override fun openUninstaller(packageName: String): Boolean = context.startResolved(
        Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )

    override fun openAppInfo(packageName: String): Boolean = context.startResolved(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )

}

private fun webIntent(uri: String) = Intent(Intent.ACTION_VIEW, uri.toUri())
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun Context.startResolved(intent: Intent): Boolean {
    if (intent.resolveActivity(packageManager) == null) return false
    startActivity(intent)
    return true
}
