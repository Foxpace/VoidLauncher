package com.tomasrepcik.voidlauncher.launcher

import com.tomasrepcik.voidlauncher.storage.database.openLauncherDatabase
import com.tomasrepcik.voidlauncher.storage.launcher.LauncherRepository
import com.tomasrepcik.voidlauncher.home.data.HomeAppsRepository
import com.tomasrepcik.voidlauncher.appcatalog.data.InstalledAppsRepository
import com.tomasrepcik.voidlauncher.storage.launcher.LauncherStorage
import com.tomasrepcik.voidlauncher.storage.launcher.LauncherStatusRepository
import com.tomasrepcik.voidlauncher.customization.data.PreferencesRepository
import com.tomasrepcik.voidlauncher.storage.database.RoomLauncherStorage
import com.tomasrepcik.voidlauncher.schedule.data.ScheduleRepository
import com.tomasrepcik.voidlauncher.shortcuts.data.ShortcutRepository
import com.tomasrepcik.voidlauncher.appcatalog.data.InstalledAppsDataSource
import com.tomasrepcik.voidlauncher.appcatalog.data.observeInstalledAppChanges
import com.tomasrepcik.voidlauncher.appcatalog.action.HandleAppSelection
import com.tomasrepcik.voidlauncher.launcher.action.AndroidAppLauncher
import com.tomasrepcik.voidlauncher.launcher.action.LauncherActionExecutor
import com.tomasrepcik.voidlauncher.launcher.error.AppErrorMessageMapper
import com.tomasrepcik.voidlauncher.schedule.data.AppScheduleResolver
import com.tomasrepcik.voidlauncher.appcatalog.search.InstalledAppSearch
import com.tomasrepcik.voidlauncher.design.components.AppIconLoader
import com.tomasrepcik.voidlauncher.customization.CustomizationViewModel
import com.tomasrepcik.voidlauncher.shortcuts.picker.ShortcutPickerViewModel
import com.tomasrepcik.voidlauncher.drawer.DrawerViewModel
import com.tomasrepcik.voidlauncher.home.HomeViewModel
import com.tomasrepcik.voidlauncher.home.minuteTicks
import com.tomasrepcik.voidlauncher.appearance.HomeAppearanceViewModel
import com.tomasrepcik.voidlauncher.appearance.AndroidBackgroundImageReader
import com.tomasrepcik.voidlauncher.appearance.AndroidContentPermissionManager
import com.tomasrepcik.voidlauncher.launcher.root.AndroidLogUnexpectedErrorReporter
import com.tomasrepcik.voidlauncher.launcher.root.AndroidLauncherRootActionMessages
import com.tomasrepcik.voidlauncher.launcher.root.LauncherAppViewModel
import com.tomasrepcik.voidlauncher.launcher.root.LauncherRootActionHandler
import com.tomasrepcik.voidlauncher.schedule.editor.ScheduleEditorArgs
import com.tomasrepcik.voidlauncher.schedule.editor.ScheduleEditorViewModel
import com.tomasrepcik.voidlauncher.schedule.editor.ScheduleIdFactory
import com.tomasrepcik.voidlauncher.schedule.list.ScheduleListViewModel
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private val IoDispatcherQualifier = named("ioDispatcher")
private val LauncherScopeQualifier = named("launcherScope")
private val HomeTimeQualifier = named("homeTime")

val launcherModule = module {
    single<CoroutineDispatcher>(IoDispatcherQualifier) { productionIoDispatcher() }
    single<CoroutineScope>(LauncherScopeQualifier) {
        CoroutineScope(SupervisorJob() + get<CoroutineDispatcher>(IoDispatcherQualifier))
    }
    single<Clock> { Clock.systemDefaultZone() }
    single<Flow<LocalDateTime>>(HomeTimeQualifier) { minuteTicks(clock = get()) }

    single { openLauncherDatabase(androidContext()) }
    single<LauncherStorage> { RoomLauncherStorage(get()) }
    single {
        val context = androidContext()
        InstalledAppsDataSource(
            packageManager = context.packageManager,
            launcherPackageName = context.packageName,
            packageChanges = context.observeInstalledAppChanges(),
            ioDispatcher = get(IoDispatcherQualifier),
        )
    }
    single {
        val installedApps = get<InstalledAppsDataSource>()
        LauncherRepository(
            storage = get(),
            installedAppUpdates = installedApps.observeInstalledApps(),
            findInstalledApp = installedApps::getInstalledApp,
            scope = get(LauncherScopeQualifier),
        )
    }
    single { LauncherStatusRepository(get()) }
    single { InstalledAppsRepository(get()) }
    single { HomeAppsRepository(launcher = get(), storage = get()) }
    single { ShortcutRepository(launcher = get(), storage = get()) }
    single { PreferencesRepository(launcher = get(), storage = get()) }
    single { ScheduleRepository(launcher = get(), storage = get()) }
    single { InstalledAppSearch() }
    single { HandleAppSelection(homeApps = get()) }
    single { AppScheduleResolver() }
    single<ScheduleIdFactory> { { UUID.randomUUID().toString() } }
    single { AppIconLoader(ioDispatcher = get(IoDispatcherQualifier)) }
    single { AndroidContentPermissionManager(androidContext()) }
    single {
        AndroidBackgroundImageReader(
            context = androidContext(),
            ioDispatcher = get(IoDispatcherQualifier),
        )
    }

    single {
        val context = androidContext()
        AndroidAppLauncher(
            packageManager = context.packageManager,
            startActivity = context::startActivity,
        )
    }
    single {
        val appLauncher = get<AndroidAppLauncher>()
        LauncherActionExecutor(
            openApp = appLauncher::open,
            installedApplicationFlags = appLauncher::installedApplicationFlags,
        )
    }
    single { AppErrorMessageMapper() }
    single { AndroidLogUnexpectedErrorReporter() }
    single {
        AndroidLauncherRootActionMessages(context = androidContext(), messageMapper = get())
    }
    single {
        val unexpectedErrorReporter = get<AndroidLogUnexpectedErrorReporter>()
        val messages = get<AndroidLauncherRootActionMessages>()
        LauncherRootActionHandler(
            actionExecutor = get(),
            reportUnexpectedError = unexpectedErrorReporter::report,
            errorMessage = messages::errorMessage,
            recoveryMessage = messages::recoveryMessage,
            appAddedToHomeMessage = messages::appAddedToHomeMessage,
        )
    }

    viewModel { LauncherAppViewModel(status = get(), preferences = get()) }
    viewModel {
        HomeViewModel(
            installedApps = get(),
            homeApps = get(),
            shortcuts = get(),
            schedules = get(),
            installedAppSearch = get(),
            handleAppSelection = get(),
            scheduleResolver = get(),
            currentTime = get(HomeTimeQualifier),
        )
    }
    viewModel {
        DrawerViewModel(
            installedApps = get(),
            homeApps = get(),
            installedAppSearch = get(),
            handleAppSelection = get(),
        )
    }
    viewModel { CustomizationViewModel(shortcuts = get()) }
    viewModel { (slot: ShortcutSlot) ->
        ShortcutPickerViewModel(
            slot = slot,
            installedApps = get(),
            shortcuts = get(),
            installedAppSearch = get(),
        )
    }
    viewModel { ScheduleListViewModel(schedules = get()) }
    viewModel { (args: ScheduleEditorArgs) ->
        ScheduleEditorViewModel(
            schedules = get(),
            installedApps = get(),
            homeApps = get(),
            scheduleId = args.scheduleId,
            installedAppSearch = get(),
            scheduleIdFactory = get(),
        )
    }
    viewModel {
        val contentPermissions = get<AndroidContentPermissionManager>()
        val backgroundImageReader = get<AndroidBackgroundImageReader>()
        HomeAppearanceViewModel(
            preferences = get(),
            keepBackgroundReadAccess = contentPermissions::keepReadAccess,
            releaseBackgroundReadAccess = contentPermissions::releaseReadAccess,
            readBackground = backgroundImageReader::read,
        )
    }
}

@Suppress("InjectDispatcher") // The composition root selects the production dispatcher.
private fun productionIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
