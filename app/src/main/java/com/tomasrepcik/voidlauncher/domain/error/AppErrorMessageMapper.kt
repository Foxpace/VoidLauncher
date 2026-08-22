package com.tomasrepcik.voidlauncher.domain.error

import android.content.Context
import com.tomasrepcik.voidlauncher.R

class AppErrorMessageMapper(
    private val context: Context,
) {
    fun message(error: AppError): String = context.getString(
        when (error.kind) {
            AppErrorKind.APP_UNAVAILABLE -> R.string.error_app_unavailable
            AppErrorKind.DESTINATION_UNAVAILABLE -> R.string.error_destination_unavailable
            AppErrorKind.ACTION_BLOCKED -> R.string.error_action_blocked
            AppErrorKind.STORAGE_INITIALIZATION_FAILED -> R.string.error_storage_initialization_failed
            AppErrorKind.STORAGE_WRITE_FAILED -> R.string.error_storage_write_failed
        },
    )
}
