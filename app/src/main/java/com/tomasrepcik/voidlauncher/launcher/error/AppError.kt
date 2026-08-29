package com.tomasrepcik.voidlauncher.launcher.error

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
    UNEXPECTED,
    STORAGE_INITIALIZATION_FAILED,
    STORAGE_READ_FAILED,
    STORAGE_WRITE_FAILED,
    INSTALLED_APPS_LOAD_FAILED,
    BACKGROUND_ACCESS_FAILED,
}

enum class AppOperation {
    LAUNCH_APP,
    OPEN_SHORTCUT,
    SEARCH_WEB,
    SEARCH_STORE,
    SEARCH_MAPS,
    UNINSTALL_APP,
    INITIALIZE_STORAGE,
    READ_STORAGE,
    LOAD_INSTALLED_APPS,
    SAVE_HOME_APPS,
    ADD_HOME_APP,
    REMOVE_HOME_APP,
    REORDER_HOME_APPS,
    RENAME_HOME_APP,
    SAVE_SHORTCUT,
    UPDATE_PREFERENCES,
    SAVE_HOME_BACKGROUND,
    SAVE_SCHEDULE,
    DELETE_SCHEDULE,
}

enum class ErrorRecovery {
    NONE,
    WEB_SEARCH_PAGE,
    STORE_WEBSITE,
    MAPS_WEBSITE,
    SYSTEM_APP_INFO,
    UNINSTALL_BLOCKED_APP_INFO,
    UNINSTALL_UNAVAILABLE_APP_INFO,
}
