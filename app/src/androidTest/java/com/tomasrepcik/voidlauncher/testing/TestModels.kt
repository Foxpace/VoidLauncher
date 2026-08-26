package com.tomasrepcik.voidlauncher.testing

import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule

fun installedApp(
    label: String,
    packageName: String = "pkg.${label.lowercase()}",
    activityName: String = "Activity${label.lowercase()}",
): InstalledApp = InstalledApp(
    key = AppKey(packageName, activityName),
    label = label,
    sortLabel = label.lowercase(),
)

fun resolvedShortcut(slot: ShortcutSlot): ResolvedShortcut = ResolvedShortcut(
    slot = slot,
    label = when (slot) {
        ShortcutSlot.LEFT -> "Contacts"
        ShortcutSlot.RIGHT -> "Camera"
    },
    selection = when (slot) {
        ShortcutSlot.LEFT -> ShortcutSelection.SystemContacts
        ShortcutSlot.RIGHT -> ShortcutSelection.SystemCamera
    },
)

fun appSchedule(
    id: String = "work",
    name: String = "Work",
    apps: Iterable<InstalledApp> = emptyList(),
    enabled: Boolean = true,
): AppSchedule = AppSchedule(
    id = id,
    name = name,
    days = setOf(java.time.DayOfWeek.MONDAY),
    startMinute = 9 * 60,
    endMinute = 17 * 60,
    appKeys = apps.mapTo(mutableSetOf()) { it.key },
    enabled = enabled,
)
