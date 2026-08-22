package com.tomasrepcik.voidlauncher.domain.error

data class AppError(
    val kind: AppErrorKind,
    val operation: AppOperation,
    val recovery: ErrorRecovery,
    val cause: Throwable? = null,
)

enum class AppErrorKind {
    APP_UNAVAILABLE,
    DESTINATION_UNAVAILABLE,
    ACTION_BLOCKED,
    STORAGE_INITIALIZATION_FAILED,
    STORAGE_WRITE_FAILED,
}

enum class AppOperation {
    LAUNCH_APP,
    OPEN_SHORTCUT,
    SEARCH_WEB,
    SEARCH_STORE,
    SEARCH_MAPS,
    UNINSTALL_APP,
    INITIALIZE_STORAGE,
    SAVE_HOME_APPS,
    ADD_HOME_APP,
    REMOVE_HOME_APP,
    REORDER_HOME_APPS,
    RENAME_HOME_APP,
    SAVE_SHORTCUT,
    UPDATE_PREFERENCES,
}

enum class ErrorRecovery {
    NONE,
    BROWSER_FALLBACK,
    STORE_WEBSITE,
    MAPS_WEBSITE,
    APP_INFO,
}
