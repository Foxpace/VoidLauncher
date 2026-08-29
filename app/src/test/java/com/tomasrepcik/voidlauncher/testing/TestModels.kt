package com.tomasrepcik.voidlauncher.testing

import com.tomasrepcik.voidlauncher.launcher.AppKey
import com.tomasrepcik.voidlauncher.launcher.InstalledApp
import com.tomasrepcik.voidlauncher.launcher.ResolvedShortcut
import com.tomasrepcik.voidlauncher.launcher.ShortcutSelection
import com.tomasrepcik.voidlauncher.launcher.ShortcutSlot
import com.tomasrepcik.voidlauncher.storage.launcher.LauncherRepository
import com.tomasrepcik.voidlauncher.storage.launcher.LauncherRepositoryState
import com.tomasrepcik.voidlauncher.home.data.HomeAppsRepository
import com.tomasrepcik.voidlauncher.appcatalog.data.InstalledAppsRepository
import com.tomasrepcik.voidlauncher.customization.data.PreferencesRepository
import com.tomasrepcik.voidlauncher.schedule.data.ScheduleRepository
import com.tomasrepcik.voidlauncher.shortcuts.data.ShortcutRepository
import com.tomasrepcik.voidlauncher.storage.database.LauncherStorageSnapshot
import com.tomasrepcik.voidlauncher.storage.database.StoredPinnedApp
import com.tomasrepcik.voidlauncher.storage.database.StoredShortcut
import com.tomasrepcik.voidlauncher.schedule.data.AppSchedule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

fun installedApp(
    label: String,
    packageName: String = "pkg.${label.lowercase()}",
    activityName: String = "Activity${label.lowercase()}",
): InstalledApp = InstalledApp(
    key = AppKey(
        packageName = packageName,
        activityName = activityName,
    ),
    label = label,
    sortLabel = label.lowercase(),
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

fun resolvedShortcut(slot: ShortcutSlot): ResolvedShortcut = ResolvedShortcut(
    slot = slot,
    label = slot.name,
    selection = when (slot) {
        ShortcutSlot.LEFT -> ShortcutSelection.SystemContacts
        ShortcutSlot.RIGHT -> ShortcutSelection.SystemCamera
    },
)

@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.startCollecting(state: StateFlow<*>) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        state.collect { }
    }
}

data class PlannedRepositoryFailures(
    val initializationCount: Int = 0,
    val writeCount: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LongParameterList") // Fixture options keep each test setup explicit at the call site.
fun TestScope.launcherRepository(
    installedApps: List<InstalledApp> = emptyList(),
    pinnedApps: List<InstalledApp> = emptyList(),
    shortcuts: List<ResolvedShortcut> = emptyList(),
    schedules: List<AppSchedule> = emptyList(),
    failures: PlannedRepositoryFailures = PlannedRepositoryFailures(),
    installedAppUpdates: MutableStateFlow<List<InstalledApp>> = MutableStateFlow(installedApps),
): LauncherRepository {
    val storage = InMemoryLauncherStorage(
        initialSnapshot = LauncherStorageSnapshot(
            installedApps = installedApps,
            pinnedApps = pinnedApps.map { StoredPinnedApp(it.key) },
            shortcuts = shortcuts.map { StoredShortcut(it.slot, it.selection) },
            schedules = schedules,
        ),
        initializationFailuresRemaining = failures.initializationCount,
        writeFailuresRemaining = failures.writeCount,
    )
    val repositoryScope = CoroutineScope(
        backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler),
    )
    return LauncherRepository(
        storage = storage,
        installedAppUpdates = installedAppUpdates,
        findInstalledApp = { appKey ->
            installedAppUpdates.value.firstOrNull { it.key == appKey }
        },
        scope = repositoryScope,
    )
}

fun LauncherRepository.readyState(): LauncherRepositoryState.Ready =
    state.value as LauncherRepositoryState.Ready

fun LauncherRepository.installedAppsRepository() = InstalledAppsRepository(this)
fun LauncherRepository.homeAppsRepository() = HomeAppsRepository(this, storage)
fun LauncherRepository.shortcutRepository() = ShortcutRepository(this, storage)
fun LauncherRepository.preferencesRepository() = PreferencesRepository(this, storage)
fun LauncherRepository.scheduleRepository() = ScheduleRepository(this, storage)
