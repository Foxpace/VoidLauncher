package com.tomasrepcik.voidlauncher.di

import com.tomasrepcik.voidlauncher.data.local.openLauncherDatabase
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepository
import com.tomasrepcik.voidlauncher.data.repository.HomeAppsRepository
import com.tomasrepcik.voidlauncher.data.repository.InstalledAppsRepository
import com.tomasrepcik.voidlauncher.data.repository.LauncherStatusRepository
import com.tomasrepcik.voidlauncher.data.repository.PreferencesRepository
import com.tomasrepcik.voidlauncher.data.repository.ScheduleRepository
import com.tomasrepcik.voidlauncher.data.repository.ShortcutRepository
import com.tomasrepcik.voidlauncher.data.source.InstalledAppsDataSource
import com.tomasrepcik.voidlauncher.data.source.PackageManagerInstalledAppsDataSource
import com.tomasrepcik.voidlauncher.data.source.observeInstalledAppChanges
import com.tomasrepcik.voidlauncher.domain.action.AndroidAppLauncher
import com.tomasrepcik.voidlauncher.domain.action.AppLauncher
import com.tomasrepcik.voidlauncher.domain.action.LauncherActionExecutor
import com.tomasrepcik.voidlauncher.domain.error.AppErrorMessageMapper
import com.tomasrepcik.voidlauncher.domain.search.InstalledAppSearch
import com.tomasrepcik.voidlauncher.ui.customization.CustomizationViewModel
import com.tomasrepcik.voidlauncher.ui.customization.shortcutpicker.ShortcutPickerViewModel
import com.tomasrepcik.voidlauncher.ui.drawer.DrawerViewModel
import com.tomasrepcik.voidlauncher.ui.home.HomeViewModel
import com.tomasrepcik.voidlauncher.ui.home.appearance.HomeAppearanceViewModel
import com.tomasrepcik.voidlauncher.ui.home.appearance.AndroidBackgroundImageReader
import com.tomasrepcik.voidlauncher.ui.home.appearance.AndroidContentPermissionManager
import com.tomasrepcik.voidlauncher.ui.home.appearance.BackgroundImageReader
import com.tomasrepcik.voidlauncher.ui.home.appearance.ContentPermissionManager
import com.tomasrepcik.voidlauncher.ui.navigation.AndroidLogUnexpectedErrorReporter
import com.tomasrepcik.voidlauncher.ui.navigation.AndroidLauncherRootActionMessages
import com.tomasrepcik.voidlauncher.ui.navigation.LauncherAppViewModel
import com.tomasrepcik.voidlauncher.ui.navigation.LauncherRootActionHandler
import com.tomasrepcik.voidlauncher.ui.navigation.LauncherRootActionMessages
import com.tomasrepcik.voidlauncher.ui.navigation.UnexpectedErrorReporter
import com.tomasrepcik.voidlauncher.ui.schedule.editor.ScheduleEditorArgs
import com.tomasrepcik.voidlauncher.ui.schedule.editor.ScheduleEditorViewModel
import com.tomasrepcik.voidlauncher.ui.schedule.list.ScheduleListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val launcherModule = module {
    single { openLauncherDatabase(androidContext()) }
    single<InstalledAppsDataSource> {
        val context = androidContext()
        PackageManagerInstalledAppsDataSource(
            packageManager = context.packageManager,
            launcherPackageName = context.packageName,
            packageChanges = context.observeInstalledAppChanges(),
        )
    }
    single { LauncherRepository(database = get(), installedAppsDataSource = get()) }
    single { LauncherStatusRepository(get()) }
    single { InstalledAppsRepository(get()) }
    single { HomeAppsRepository(get()) }
    single { ShortcutRepository(get()) }
    single { PreferencesRepository(get()) }
    single { ScheduleRepository(get()) }
    single { InstalledAppSearch() }
    single<ContentPermissionManager> { AndroidContentPermissionManager(androidContext()) }
    single<BackgroundImageReader> { AndroidBackgroundImageReader(androidContext()) }

    single<AppLauncher> {
        val context = androidContext()
        AndroidAppLauncher(
            packageManager = context.packageManager,
            startActivity = context::startActivity,
        )
    }
    single { LauncherActionExecutor(appLauncher = get()) }
    single { AppErrorMessageMapper() }
    single<UnexpectedErrorReporter> { AndroidLogUnexpectedErrorReporter() }
    single<LauncherRootActionMessages> {
        AndroidLauncherRootActionMessages(context = androidContext(), messageMapper = get())
    }
    single {
        LauncherRootActionHandler(
            actionExecutor = get(),
            unexpectedErrorReporter = get(),
            messages = get(),
        )
    }

    viewModel { LauncherAppViewModel(status = get(), preferences = get()) }
    viewModel {
        HomeViewModel.createForProduction(
            installedApps = get(),
            homeApps = get(),
            shortcuts = get(),
            schedules = get(),
            installedAppSearch = get(),
        )
    }
    viewModel {
        DrawerViewModel(
            installedApps = get(),
            homeApps = get(),
            installedAppSearch = get(),
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
        )
    }
    viewModel {
        HomeAppearanceViewModel(
            preferences = get(),
            contentPermissions = get(),
            backgroundImageReader = get(),
        )
    }
}
