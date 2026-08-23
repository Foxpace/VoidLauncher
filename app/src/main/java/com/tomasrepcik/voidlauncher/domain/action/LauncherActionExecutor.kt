package com.tomasrepcik.voidlauncher.domain.action

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.ApplicationInfo
import com.tomasrepcik.voidlauncher.domain.error.AppError
import com.tomasrepcik.voidlauncher.domain.error.AppErrorKind
import com.tomasrepcik.voidlauncher.domain.error.AppOperation
import com.tomasrepcik.voidlauncher.domain.error.ErrorRecovery
import kotlinx.coroutines.CancellationException

class LauncherActionExecutor {
    fun execute(context: Context, action: LauncherAction): LauncherActionOutcome = execute(
        action = action,
        platform = AndroidLauncherActionPlatform(
            packageManager = context.packageManager,
            startActivity = context::startActivity,
        ),
    )

    internal fun execute(
        action: LauncherAction,
        platform: LauncherActionPlatform,
    ): LauncherActionOutcome = runCatching { executeAction(action, platform) }
        .fold(
            onSuccess = { it },
            onFailure = { cause ->
                cause.rethrowIfCancellation()
                failure(
                    kind = AppErrorKind.UNEXPECTED,
                    operation = action.operation,
                    cause = cause,
                )
            },
        )

    private fun executeAction(
        action: LauncherAction,
        platform: LauncherActionPlatform,
    ): LauncherActionOutcome = when (action) {
        is LauncherAction.LaunchInstalledApp -> attempt(
            operation = AppOperation.LAUNCH_APP,
            unavailableKind = AppErrorKind.APP_UNAVAILABLE,
        ) {
            platform.launchInstalledApp(action.app)
        }
        is LauncherAction.OpenShortcut -> openShortcut(action, platform)
        is LauncherAction.OpenWebSearch -> openWebSearch(action, platform)
        is LauncherAction.OpenPlayStoreSearch -> openPlayStore(action, platform)
        is LauncherAction.OpenMapsSearch -> openMaps(action, platform)
        is LauncherAction.UninstallApp -> uninstall(action, platform)
    }

    private fun openShortcut(
        action: LauncherAction.OpenShortcut,
        platform: LauncherActionPlatform,
    ): LauncherActionOutcome {
        val shortcut = action.shortcut
        if (!shortcut.isAvailable) {
            return failure(AppErrorKind.APP_UNAVAILABLE, AppOperation.OPEN_SHORTCUT)
        }
        return attempt(AppOperation.OPEN_SHORTCUT) { platform.openShortcut(shortcut) }
    }

    private fun openWebSearch(
        action: LauncherAction.OpenWebSearch,
        platform: LauncherActionPlatform,
    ): LauncherActionOutcome = attemptWithFallback(
        operation = AppOperation.SEARCH_WEB,
        recovery = ErrorRecovery.BROWSER_FALLBACK,
        primary = { platform.openWebSearch(action.query) },
        fallback = { platform.openBrowserSearch(action.query) },
    )

    private fun openPlayStore(
        action: LauncherAction.OpenPlayStoreSearch,
        platform: LauncherActionPlatform,
    ): LauncherActionOutcome = attemptWithFallback(
        operation = AppOperation.SEARCH_STORE,
        recovery = ErrorRecovery.STORE_WEBSITE,
        primary = { platform.openPlayStore(action.query) },
        fallback = { platform.openPlayStoreWebsite(action.query) },
    )

    private fun openMaps(
        action: LauncherAction.OpenMapsSearch,
        platform: LauncherActionPlatform,
    ): LauncherActionOutcome = attemptWithFallback(
        operation = AppOperation.SEARCH_MAPS,
        recovery = ErrorRecovery.MAPS_WEBSITE,
        primary = { platform.openMaps(action.query) },
        fallback = { platform.openMapsWebsite(action.query) },
    )

    private fun uninstall(
        action: LauncherAction.UninstallApp,
        platform: LauncherActionPlatform,
    ): LauncherActionOutcome {
        val flags = platform.applicationFlags(action.app.key.packageName)
            ?: return failure(AppErrorKind.APP_UNAVAILABLE, AppOperation.UNINSTALL_APP)
        if (!canUninstallFromLauncher(flags)) {
            return recoverToAppInfo(action, platform, ErrorRecovery.SYSTEM_APP_INFO)
        }
        return try {
            if (platform.openUninstaller(action.app.key.packageName)) {
                LauncherActionOutcome.Completed
            } else {
                recoverToAppInfo(
                    action,
                    platform,
                    ErrorRecovery.UNINSTALL_UNAVAILABLE_APP_INFO,
                )
            }
        } catch (cause: SecurityException) {
            recoverToAppInfo(
                action,
                platform,
                ErrorRecovery.UNINSTALL_BLOCKED_APP_INFO,
                cause,
            )
        } catch (cause: ActivityNotFoundException) {
            recoverToAppInfo(
                action,
                platform,
                ErrorRecovery.UNINSTALL_UNAVAILABLE_APP_INFO,
                cause,
            )
        }
    }

    private fun recoverToAppInfo(
        action: LauncherAction.UninstallApp,
        platform: LauncherActionPlatform,
        recovery: ErrorRecovery,
        originalCause: Throwable? = null,
    ): LauncherActionOutcome = try {
        if (platform.openAppInfo(action.app.key.packageName)) {
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
        failure(
            AppErrorKind.DESTINATION_UNAVAILABLE,
            AppOperation.UNINSTALL_APP,
            recovery,
            cause,
        )
    }

    private fun attempt(
        operation: AppOperation,
        unavailableKind: AppErrorKind = AppErrorKind.DESTINATION_UNAVAILABLE,
        launch: () -> Boolean,
    ): LauncherActionOutcome = try {
        if (launch()) LauncherActionOutcome.Completed else failure(unavailableKind, operation)
    } catch (cause: SecurityException) {
        failure(AppErrorKind.ACTION_BLOCKED, operation, cause = cause)
    } catch (cause: ActivityNotFoundException) {
        failure(unavailableKind, operation, cause = cause)
    }

    private fun attemptWithFallback(
        operation: AppOperation,
        recovery: ErrorRecovery,
        primary: () -> Boolean,
        fallback: () -> Boolean,
    ): LauncherActionOutcome {
        var primaryCause: Throwable? = null
        try {
            if (primary()) return LauncherActionOutcome.Completed
        } catch (cause: SecurityException) {
            primaryCause = cause
        } catch (cause: ActivityNotFoundException) {
            primaryCause = cause
        }

        return try {
            if (fallback()) {
                LauncherActionOutcome.Recovered(recovery)
            } else {
                failure(
                    AppErrorKind.DESTINATION_UNAVAILABLE,
                    operation,
                    recovery,
                    primaryCause,
                )
            }
        } catch (cause: SecurityException) {
            failure(AppErrorKind.ACTION_BLOCKED, operation, recovery, cause)
        } catch (cause: ActivityNotFoundException) {
            failure(AppErrorKind.DESTINATION_UNAVAILABLE, operation, recovery, cause)
        }
    }

}

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
