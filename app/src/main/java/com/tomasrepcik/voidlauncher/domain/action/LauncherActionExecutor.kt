package com.tomasrepcik.voidlauncher.domain.action

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.ApplicationInfo
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.domain.error.AppError
import com.tomasrepcik.voidlauncher.domain.error.AppErrorKind
import com.tomasrepcik.voidlauncher.domain.error.AppErrorMessageMapper
import com.tomasrepcik.voidlauncher.domain.error.AppOperation
import com.tomasrepcik.voidlauncher.domain.error.ErrorRecovery

class LauncherActionExecutor internal constructor(
    private val platform: LauncherActionPlatform,
    private val messages: LauncherActionMessages,
) {
    constructor(context: Context) : this(
        platform = AndroidLauncherActionPlatform(context),
        messages = AndroidLauncherActionMessages(context, AppErrorMessageMapper(context)),
    )

    @Suppress("RedundantSuspendModifier")
    suspend fun execute(action: LauncherAction): LauncherActionOutcome = when (action) {
        is LauncherAction.LaunchInstalledApp -> attempt(
            operation = AppOperation.LAUNCH_APP,
            unavailableKind = AppErrorKind.APP_UNAVAILABLE,
        ) {
            platform.launchInstalledApp(action.app)
        }
        is LauncherAction.OpenShortcut -> openShortcut(action)
        is LauncherAction.OpenWebSearch -> openWebSearch(action)
        is LauncherAction.OpenPlayStoreSearch -> openPlayStore(action)
        is LauncherAction.OpenMapsSearch -> openMaps(action)
        is LauncherAction.UninstallApp -> uninstall(action)
    }

    private fun openShortcut(action: LauncherAction.OpenShortcut): LauncherActionOutcome {
        val shortcut = action.shortcut
        if (!shortcut.isAvailable) {
            return failure(AppErrorKind.APP_UNAVAILABLE, AppOperation.OPEN_SHORTCUT)
        }
        return attempt(AppOperation.OPEN_SHORTCUT) { platform.openShortcut(shortcut) }
    }

    private fun openWebSearch(action: LauncherAction.OpenWebSearch): LauncherActionOutcome =
        attemptWithFallback(
            operation = AppOperation.SEARCH_WEB,
            recovery = ErrorRecovery.BROWSER_FALLBACK,
            primary = { platform.openWebSearch(action.query) },
            fallback = { platform.openBrowserSearch(action.query) },
        )

    private fun openPlayStore(action: LauncherAction.OpenPlayStoreSearch): LauncherActionOutcome =
        attemptWithFallback(
            operation = AppOperation.SEARCH_STORE,
            recovery = ErrorRecovery.STORE_WEBSITE,
            primary = { platform.openPlayStore(action.query) },
            fallback = { platform.openPlayStoreWebsite(action.query) },
        )

    private fun openMaps(action: LauncherAction.OpenMapsSearch): LauncherActionOutcome =
        attemptWithFallback(
            operation = AppOperation.SEARCH_MAPS,
            recovery = ErrorRecovery.MAPS_WEBSITE,
            primary = { platform.openMaps(action.query) },
            fallback = { platform.openMapsWebsite(action.query) },
        )

    private fun uninstall(action: LauncherAction.UninstallApp): LauncherActionOutcome {
        val flags = platform.applicationFlags(action.app.key.packageName)
            ?: return failure(AppErrorKind.APP_UNAVAILABLE, AppOperation.UNINSTALL_APP)
        if (!canUninstallFromLauncher(flags)) {
            return recoverToAppInfo(action, messages.systemAppInfoOpened)
        }
        return try {
            if (platform.openUninstaller(action.app.key.packageName)) {
                LauncherActionOutcome.Completed
            } else {
                recoverToAppInfo(action, messages.uninstallUnavailableInfoOpened)
            }
        } catch (cause: SecurityException) {
            recoverToAppInfo(action, messages.uninstallBlockedInfoOpened, cause)
        } catch (cause: ActivityNotFoundException) {
            recoverToAppInfo(action, messages.uninstallUnavailableInfoOpened, cause)
        }
    }

    private fun recoverToAppInfo(
        action: LauncherAction.UninstallApp,
        message: String,
        originalCause: Throwable? = null,
    ): LauncherActionOutcome = try {
        if (platform.openAppInfo(action.app.key.packageName)) {
            LauncherActionOutcome.Recovered(ErrorRecovery.APP_INFO, message)
        } else {
            failure(
                kind = AppErrorKind.DESTINATION_UNAVAILABLE,
                operation = AppOperation.UNINSTALL_APP,
                recovery = ErrorRecovery.APP_INFO,
                cause = originalCause,
            )
        }
    } catch (cause: SecurityException) {
        failure(AppErrorKind.ACTION_BLOCKED, AppOperation.UNINSTALL_APP, ErrorRecovery.APP_INFO, cause)
    } catch (cause: ActivityNotFoundException) {
        failure(
            AppErrorKind.DESTINATION_UNAVAILABLE,
            AppOperation.UNINSTALL_APP,
            ErrorRecovery.APP_INFO,
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
        try {
            if (primary()) return LauncherActionOutcome.Completed
        } catch (_: SecurityException) {
            // The known recovery is attempted below.
        } catch (_: ActivityNotFoundException) {
            // The known recovery is attempted below.
        }

        return try {
            if (fallback()) {
                LauncherActionOutcome.Recovered(recovery)
            } else {
                failure(AppErrorKind.DESTINATION_UNAVAILABLE, operation, recovery)
            }
        } catch (cause: SecurityException) {
            failure(AppErrorKind.ACTION_BLOCKED, operation, recovery, cause)
        } catch (cause: ActivityNotFoundException) {
            failure(AppErrorKind.DESTINATION_UNAVAILABLE, operation, recovery, cause)
        }
    }

    private fun failure(
        kind: AppErrorKind,
        operation: AppOperation,
        recovery: ErrorRecovery = ErrorRecovery.NONE,
        cause: Throwable? = null,
    ): LauncherActionOutcome.Failed {
        val error = AppError(kind, operation, recovery, cause)
        return LauncherActionOutcome.Failed(error, messages.error(error))
    }

    private fun canUninstallFromLauncher(flags: Int): Boolean {
        val isSystemApp = flags and ApplicationInfo.FLAG_SYSTEM != 0
        val isUpdatedSystemApp = flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
        return !isSystemApp || isUpdatedSystemApp
    }
}

internal interface LauncherActionMessages {
    val systemAppInfoOpened: String
    val uninstallBlockedInfoOpened: String
    val uninstallUnavailableInfoOpened: String
    fun error(error: AppError): String
}

private class AndroidLauncherActionMessages(
    private val context: Context,
    private val errorMessageMapper: AppErrorMessageMapper,
) : LauncherActionMessages {
    override val systemAppInfoOpened: String
        get() = context.getString(R.string.system_app_info_opened)
    override val uninstallBlockedInfoOpened: String
        get() = context.getString(R.string.uninstall_blocked_info_opened)
    override val uninstallUnavailableInfoOpened: String
        get() = context.getString(R.string.uninstall_unavailable_info_opened)

    override fun error(error: AppError): String = errorMessageMapper.message(error)
}
