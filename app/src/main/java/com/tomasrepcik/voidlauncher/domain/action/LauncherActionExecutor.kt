package com.tomasrepcik.voidlauncher.domain.action

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.net.toUri
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.domain.error.AppError
import com.tomasrepcik.voidlauncher.domain.error.AppErrorKind
import com.tomasrepcik.voidlauncher.domain.error.AppOperation
import com.tomasrepcik.voidlauncher.domain.error.ErrorRecovery
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CancellationException

/** Owns Android intent planning, fallback policy, recovery, and failure translation. */
class LauncherActionExecutor internal constructor(
    private val openApp: (Intent) -> Boolean,
    private val installedApplicationFlags: (String) -> Int?,
) {
    fun execute(action: LauncherAction): LauncherActionOutcome = runCatching {
        executeAction(action)
    }.fold(
        onSuccess = { it },
        onFailure = { cause ->
            cause.rethrowIfCancellation()
            failure(AppErrorKind.UNEXPECTED, action.operation, cause = cause)
        },
    )

    private fun executeAction(action: LauncherAction): LauncherActionOutcome = when (action) {
        is LauncherAction.LaunchInstalledApp -> openDestination(
            operation = AppOperation.LAUNCH_APP,
            unavailableKind = AppErrorKind.APP_UNAVAILABLE,
        ) { openApp(action.app.launchIntent()) }
        is LauncherAction.OpenShortcut -> openShortcut(action)
        is LauncherAction.OpenWebSearch -> openWebSearch(action)
        is LauncherAction.OpenPlayStoreSearch -> openPlayStore(action)
        is LauncherAction.OpenMapsSearch -> openMaps(action)
        is LauncherAction.UninstallApp -> uninstall(action)
    }

    private fun openShortcut(action: LauncherAction.OpenShortcut): LauncherActionOutcome {
        val intent = action.shortcut.launchIntent()
            ?: return failure(AppErrorKind.APP_UNAVAILABLE, AppOperation.OPEN_SHORTCUT)
        return openDestination(AppOperation.OPEN_SHORTCUT) { openApp(intent) }
    }

    private fun openWebSearch(action: LauncherAction.OpenWebSearch) =
        openPreferredOrAlternativeDestination(
            operation = AppOperation.SEARCH_WEB,
            alternativeRecovery = ErrorRecovery.WEB_SEARCH_PAGE,
            openPreferredDestination = {
                openApp(
                    Intent(Intent.ACTION_WEB_SEARCH).apply {
                        putExtra(SearchManager.QUERY, action.query)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            },
            openAlternativeDestination = {
                val encodedQuery = URLEncoder.encode(action.query, StandardCharsets.UTF_8.toString())
                openApp(webIntent("https://www.google.com/search?q=$encodedQuery"))
            },
        )

    private fun openPlayStore(action: LauncherAction.OpenPlayStoreSearch) =
        openPreferredOrAlternativeDestination(
        operation = AppOperation.SEARCH_STORE,
        alternativeRecovery = ErrorRecovery.STORE_WEBSITE,
        openPreferredDestination = {
            openApp(webIntent("market://search?q=${Uri.encode(action.query)}&c=apps"))
        },
        openAlternativeDestination = {
            openApp(
                webIntent("https://play.google.com/store/search?q=${Uri.encode(action.query)}&c=apps"),
            )
        },
    )

    private fun openMaps(action: LauncherAction.OpenMapsSearch) =
        openPreferredOrAlternativeDestination(
        operation = AppOperation.SEARCH_MAPS,
        alternativeRecovery = ErrorRecovery.MAPS_WEBSITE,
        openPreferredDestination = {
            openApp(
                webIntent("geo:0,0?q=${Uri.encode(action.query)}").apply {
                    `package` = "com.google.android.apps.maps"
                },
            )
        },
        openAlternativeDestination = {
            openApp(
                webIntent("https://www.google.com/maps/search/?api=1&query=${Uri.encode(action.query)}"),
            )
        },
    )

    private fun uninstall(action: LauncherAction.UninstallApp): LauncherActionOutcome {
        val packageName = action.app.key.packageName
        val flags = installedApplicationFlags(packageName)
            ?: return failure(AppErrorKind.APP_UNAVAILABLE, AppOperation.UNINSTALL_APP)
        if (!canUninstallFromLauncher(flags)) {
            return recoverToAppInfo(packageName, ErrorRecovery.SYSTEM_APP_INFO)
        }
        return try {
            if (openApp(uninstallIntent(packageName))) {
                LauncherActionOutcome.Completed
            } else {
                recoverToAppInfo(packageName, ErrorRecovery.UNINSTALL_UNAVAILABLE_APP_INFO)
            }
        } catch (cause: SecurityException) {
            recoverToAppInfo(packageName, ErrorRecovery.UNINSTALL_BLOCKED_APP_INFO, cause)
        } catch (cause: ActivityNotFoundException) {
            recoverToAppInfo(packageName, ErrorRecovery.UNINSTALL_UNAVAILABLE_APP_INFO, cause)
        }
    }

    private fun recoverToAppInfo(
        packageName: String,
        recovery: ErrorRecovery,
        originalCause: Throwable? = null,
    ): LauncherActionOutcome = try {
        if (openApp(appInfoIntent(packageName))) {
            LauncherActionOutcome.Recovered(recovery)
        } else {
            failure(
                kind = AppErrorKind.DESTINATION_UNAVAILABLE,
                operation = AppOperation.UNINSTALL_APP,
                recovery = recovery,
                cause = originalCause,
            )
        }
    } catch (cause: SecurityException) {
        failure(AppErrorKind.ACTION_BLOCKED, AppOperation.UNINSTALL_APP, recovery, cause)
    } catch (cause: ActivityNotFoundException) {
        failure(AppErrorKind.DESTINATION_UNAVAILABLE, AppOperation.UNINSTALL_APP, recovery, cause)
    }

    private fun openDestination(
        operation: AppOperation,
        unavailableKind: AppErrorKind = AppErrorKind.DESTINATION_UNAVAILABLE,
        openDestination: () -> Boolean,
    ): LauncherActionOutcome = try {
        if (openDestination()) {
            LauncherActionOutcome.Completed
        } else {
            failure(unavailableKind, operation)
        }
    } catch (cause: SecurityException) {
        failure(AppErrorKind.ACTION_BLOCKED, operation, cause = cause)
    } catch (cause: ActivityNotFoundException) {
        failure(unavailableKind, operation, cause = cause)
    }

    private fun openPreferredOrAlternativeDestination(
        operation: AppOperation,
        alternativeRecovery: ErrorRecovery,
        openPreferredDestination: () -> Boolean,
        openAlternativeDestination: () -> Boolean,
    ): LauncherActionOutcome {
        var preferredDestinationFailure: Throwable? = null
        try {
            if (openPreferredDestination()) return LauncherActionOutcome.Completed
        } catch (cause: SecurityException) {
            preferredDestinationFailure = cause
        } catch (cause: ActivityNotFoundException) {
            preferredDestinationFailure = cause
        }

        return try {
            if (openAlternativeDestination()) {
                LauncherActionOutcome.Recovered(alternativeRecovery)
            } else {
                failure(
                    AppErrorKind.DESTINATION_UNAVAILABLE,
                    operation,
                    alternativeRecovery,
                    preferredDestinationFailure,
                )
            }
        } catch (cause: SecurityException) {
            failure(AppErrorKind.ACTION_BLOCKED, operation, alternativeRecovery, cause)
        } catch (cause: ActivityNotFoundException) {
            failure(AppErrorKind.DESTINATION_UNAVAILABLE, operation, alternativeRecovery, cause)
        }
    }
}

private fun InstalledApp.launchIntent() = Intent().apply {
    component = ComponentName(key.packageName, key.activityName)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

private fun ResolvedShortcut.launchIntent(): Intent? = when (selection) {
    is ShortcutSelection.AppShortcut -> installedApp?.launchIntent()
    ShortcutSelection.SystemCamera -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ShortcutSelection.SystemContacts -> Intent(
        Intent.ACTION_VIEW,
        ContactsContract.Contacts.CONTENT_URI,
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

private fun webIntent(uri: String) = Intent(Intent.ACTION_VIEW, uri.toUri())
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun uninstallIntent(packageName: String) = Intent(
    Intent.ACTION_DELETE,
    Uri.fromParts("package", packageName, null),
).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun appInfoIntent(packageName: String) = Intent(
    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    Uri.fromParts("package", packageName, null),
).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun failure(
    kind: AppErrorKind,
    operation: AppOperation,
    recovery: ErrorRecovery = ErrorRecovery.NONE,
    cause: Throwable? = null,
): LauncherActionOutcome.Failed = LauncherActionOutcome.Failed(
    AppError(kind, operation, recovery, cause),
)

private fun canUninstallFromLauncher(flags: Int): Boolean {
    val isSystemApp = flags and ApplicationInfo.FLAG_SYSTEM != 0
    val isUpdatedSystemApp = flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
    return !isSystemApp || isUpdatedSystemApp
}

private val LauncherAction.operation: AppOperation
    get() = when (this) {
        is LauncherAction.LaunchInstalledApp -> AppOperation.LAUNCH_APP
        is LauncherAction.OpenShortcut -> AppOperation.OPEN_SHORTCUT
        is LauncherAction.OpenWebSearch -> AppOperation.SEARCH_WEB
        is LauncherAction.OpenPlayStoreSearch -> AppOperation.SEARCH_STORE
        is LauncherAction.OpenMapsSearch -> AppOperation.SEARCH_MAPS
        is LauncherAction.UninstallApp -> AppOperation.UNINSTALL_APP
    }

private fun Throwable.rethrowIfCancellation() {
    if (this is CancellationException) throw this
}
