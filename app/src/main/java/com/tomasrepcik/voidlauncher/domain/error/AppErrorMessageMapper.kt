package com.tomasrepcik.voidlauncher.domain.error

import android.content.Context
import com.tomasrepcik.voidlauncher.R

class AppErrorMessageMapper {
    fun message(context: Context, error: AppError): String = context.getString(
        when (error.kind) {
            AppErrorKind.APP_UNAVAILABLE -> R.string.error_app_unavailable
            AppErrorKind.DESTINATION_UNAVAILABLE -> R.string.error_destination_unavailable
            AppErrorKind.ACTION_BLOCKED -> R.string.error_action_blocked
            AppErrorKind.UNEXPECTED -> R.string.error_unexpected
            AppErrorKind.STORAGE_INITIALIZATION_FAILED -> R.string.error_storage_initialization_failed
            AppErrorKind.STORAGE_READ_FAILED -> R.string.error_storage_read_failed
            AppErrorKind.STORAGE_WRITE_FAILED -> R.string.error_storage_write_failed
            AppErrorKind.INSTALLED_APPS_LOAD_FAILED -> R.string.error_installed_apps_load_failed
        },
    )

    fun recoveryMessage(context: Context, recovery: ErrorRecovery): String? = when (recovery) {
        ErrorRecovery.NONE,
        ErrorRecovery.BROWSER_FALLBACK,
        ErrorRecovery.STORE_WEBSITE,
        ErrorRecovery.MAPS_WEBSITE,
        -> null
        ErrorRecovery.SYSTEM_APP_INFO -> context.getString(R.string.system_app_info_opened)
        ErrorRecovery.UNINSTALL_BLOCKED_APP_INFO ->
            context.getString(R.string.uninstall_blocked_info_opened)
        ErrorRecovery.UNINSTALL_UNAVAILABLE_APP_INFO ->
            context.getString(R.string.uninstall_unavailable_info_opened)
    }
}
