package com.tomasrepcik.voidlauncher.data.repository

import com.tomasrepcik.voidlauncher.data.local.AppScheduleEntity
import com.tomasrepcik.voidlauncher.data.local.InstalledAppEntity
import com.tomasrepcik.voidlauncher.data.local.LauncherPreferencesEntity
import com.tomasrepcik.voidlauncher.data.local.PinnedAppEntity
import com.tomasrepcik.voidlauncher.data.local.ShortcutEntity
import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.LauncherPreferences
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule
import com.tomasrepcik.voidlauncher.domain.schedule.MINUTES_PER_DAY
import java.time.DayOfWeek

internal const val HOME_SECTION = "HOME"
internal const val TYPE_CONTACTS = "SYSTEM_CONTACTS"
internal const val TYPE_CAMERA = "SYSTEM_CAMERA"
private const val TYPE_APP = "APP"

internal val PinnedAppEntity.key: AppKey
    get() = AppKey(packageName, activityName)

internal fun PinnedAppEntity.toStored() = StoredPinnedApp(key, labelOverride)

internal fun ShortcutEntity.toStored(): StoredShortcut? {
    val shortcutSlot = ShortcutSlot.entries.firstOrNull { it.name == slot } ?: return null
    val selection = when (shortcutType) {
        TYPE_CONTACTS -> ShortcutSelection.SystemContacts
        TYPE_CAMERA -> ShortcutSelection.SystemCamera
        TYPE_APP -> packageName?.let { packageName ->
            activityName?.let { activityName ->
                ShortcutSelection.AppShortcut(AppKey(packageName, activityName))
            }
        }
        else -> null
    } ?: return null
    return StoredShortcut(shortcutSlot, selection, customLabel)
}

internal fun defaultShortcut(slot: ShortcutSlot, type: String) = ShortcutEntity(
    slot = slot.name,
    position = slot.ordinal,
    shortcutType = type,
)

internal fun pinnedAppEntity(position: Int, appKey: AppKey) = PinnedAppEntity(
    section = HOME_SECTION,
    position = position,
    packageName = appKey.packageName,
    activityName = appKey.activityName,
)

internal fun ShortcutSelection.toEntity(slot: ShortcutSlot): ShortcutEntity = when (this) {
    ShortcutSelection.SystemCamera -> defaultShortcut(slot, TYPE_CAMERA)
    ShortcutSelection.SystemContacts -> defaultShortcut(slot, TYPE_CONTACTS)
    is ShortcutSelection.AppShortcut -> ShortcutEntity(
        slot = slot.name,
        position = slot.ordinal,
        shortcutType = TYPE_APP,
        packageName = key.packageName,
        activityName = key.activityName,
    )
}

internal fun InstalledApp.toEntity() = InstalledAppEntity(
    packageName = key.packageName,
    activityName = key.activityName,
    label = label,
    sortLabel = sortLabel,
)

internal fun InstalledAppEntity.toModel() = InstalledApp(
    key = AppKey(packageName, activityName),
    label = label,
    sortLabel = sortLabel,
)

internal val LauncherPreferencesEntity.model: LauncherPreferences
    get() = LauncherPreferences(
        hasSeenNavigationTutorial = hasSeenNavigationTutorial,
        homeBackgroundUri = homeBackgroundUri,
        useBackgroundColors = useBackgroundColors,
    )

internal val LauncherPreferences.entity: LauncherPreferencesEntity
    get() = LauncherPreferencesEntity(
        hasSeenNavigationTutorial = hasSeenNavigationTutorial,
        homeBackgroundUri = homeBackgroundUri,
        useBackgroundColors = useBackgroundColors,
    )

internal fun AppSchedule.toEntity() = AppScheduleEntity(
    id = id,
    name = name,
    days = days.joinToString(",", transform = DayOfWeek::name),
    startMinute = startMinute.coerceIn(0, MINUTES_PER_DAY - 1),
    endMinute = endMinute.coerceIn(0, MINUTES_PER_DAY - 1),
    appKeys = appKeys.joinToString("\n") { key ->
        "${key.packageName}\t${key.activityName}"
    },
    enabled = enabled,
)

internal fun AppScheduleEntity.toModel(): AppSchedule {
    val storedDays = days.split(',')
        .mapNotNull { value -> DayOfWeek.entries.firstOrNull { it.name == value } }
        .toSet()
    val storedAppKeys = appKeys.lineSequence().mapNotNull { value ->
        val parts = value.split('\t', limit = 2)
        if (parts.size == 2) AppKey(parts[0], parts[1]) else null
    }.toSet()
    return AppSchedule(
        id = id,
        name = name,
        days = storedDays,
        startMinute = startMinute.coerceIn(0, MINUTES_PER_DAY - 1),
        endMinute = endMinute.coerceIn(0, MINUTES_PER_DAY - 1),
        appKeys = storedAppKeys,
        enabled = enabled,
    )
}
