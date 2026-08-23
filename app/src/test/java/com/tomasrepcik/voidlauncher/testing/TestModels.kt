package com.tomasrepcik.voidlauncher.testing

import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.data.repository.InMemoryLauncherStorage
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepository
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepositoryState
import com.tomasrepcik.voidlauncher.data.repository.LauncherStorageSnapshot
import com.tomasrepcik.voidlauncher.data.repository.StoredPinnedApp
import com.tomasrepcik.voidlauncher.data.repository.StoredShortcut
import com.tomasrepcik.voidlauncher.data.source.InstalledAppsDataSource
import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

fun installedApp(label: String): InstalledApp = InstalledApp(
    key = AppKey(
        packageName = "pkg.${label.lowercase()}",
        activityName = "Activity${label.lowercase()}",
    ),
    label = label,
    sortLabel = label.lowercase(),
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
fun TestScope.launcherRepository(
    installedApps: List<InstalledApp> = emptyList(),
    pinnedApps: List<InstalledApp> = emptyList(),
    shortcuts: List<ResolvedShortcut> = emptyList(),
    schedules: List<AppSchedule> = emptyList(),
    failures: PlannedRepositoryFailures = PlannedRepositoryFailures(),
): LauncherRepository {
    val source = TestInstalledAppsDataSource(installedApps)
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
    return LauncherRepository(storage, source, repositoryScope)
}

fun LauncherRepository.readyState(): LauncherRepositoryState.Ready =
    state.value as LauncherRepositoryState.Ready

private class TestInstalledAppsDataSource(
    apps: List<InstalledApp>,
) : InstalledAppsDataSource {
    private val installedApps = MutableStateFlow(apps)

    override fun observeInstalledApps(): Flow<List<InstalledApp>> = installedApps

    override suspend fun getInstalledApp(appKey: AppKey): InstalledApp? =
        installedApps.value.firstOrNull { it.key == appKey }
}
