package com.tomasrepcik.voidlauncher.ui.navigation

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.tomasrepcik.voidlauncher.LauncherApplication
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.ui.customization.CustomizationScreen
import com.tomasrepcik.voidlauncher.ui.customization.CustomizationViewModel
import com.tomasrepcik.voidlauncher.ui.customization.ShortcutPickerScreen
import com.tomasrepcik.voidlauncher.ui.customization.ShortcutPickerViewModel
import com.tomasrepcik.voidlauncher.ui.drawer.AppDrawerScreen
import com.tomasrepcik.voidlauncher.ui.drawer.DrawerViewModel
import com.tomasrepcik.voidlauncher.ui.home.HomeScreen
import com.tomasrepcik.voidlauncher.ui.home.HomeViewModel
import kotlinx.coroutines.flow.Flow
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.core.net.toUri
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

@Serializable
data object AppListRoute : NavKey

@Serializable
data object CustomizationRoute : NavKey

@Serializable
data class ShortcutPickerRoute(val slot: ShortcutSlot) : NavKey

sealed interface LauncherCommand {
    data class LaunchInstalledApp(val app: InstalledApp) : LauncherCommand
    data class OpenWebSearch(val query: String) : LauncherCommand
    data class OpenPlayStoreSearch(val query: String) : LauncherCommand
    data class OpenMapsSearch(val query: String) : LauncherCommand
    data class OpenShortcut(val shortcut: ResolvedShortcut) : LauncherCommand
    data class UninstallApp(val app: InstalledApp) : LauncherCommand
    data class ShowMessage(val message: String) : LauncherCommand
}

@Composable
fun VoidLauncherApp() {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as LauncherApplication).appContainer
    val backStack = rememberNavBackStack(HomeRoute)
    val snackbarHostState = remember { SnackbarHostState() }
    val activityResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize(),
            onBack = backStack::popIfNotRoot,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<HomeRoute> {
                    val viewModel: HomeViewModel = viewModel(
                        factory = HomeViewModel.provideFactory(
                            repository = appContainer.launcherRepository,
                            searchResolver = appContainer.searchResolver,
                        )
                    )
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    CollectCommands(
                        commands = viewModel.commands,
                        snackbarHostState = snackbarHostState,
                        context = context,
                        launchActivityForResult = activityResultLauncher::launch,
                    )
                    HomeScreen(
                        state = state,
                        onQueryChange = viewModel::onQueryChange,
                        onPrimarySearch = viewModel::onPrimarySearch,
                        onBrowserSearch = viewModel::onBrowserSearch,
                        onPlayStoreSearch = viewModel::onPlayStoreSearch,
                        onMapsSearch = viewModel::onMapsSearch,
                        onAppHint = viewModel::onAppHint,
                        onAppClicked = viewModel::onAppClicked,
                        onShortcutClicked = viewModel::onShortcutClicked,
                        onOpenDrawer = { backStack.pushSingleTop(AppListRoute) },
                        onRemoveHomeApp = viewModel::removeHomeApp,
                        onRenameHomeApp = viewModel::renameHomeApp,
                        onUninstallApp = viewModel::uninstallApp,
                        onReorderHomeApps = viewModel::reorderHomeApps,
                    )
                }

                entry<AppListRoute> {
                    val viewModel: DrawerViewModel = viewModel(
                        factory = DrawerViewModel.provideFactory(appContainer.launcherRepository)
                    )
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    CollectCommands(
                        commands = viewModel.commands,
                        snackbarHostState = snackbarHostState,
                        context = context,
                        launchActivityForResult = activityResultLauncher::launch,
                    )
                    AppDrawerScreen(
                        state = state,
                        onBack = backStack::popIfNotRoot,
                        onOpenSettings = { backStack.pushSingleTop(CustomizationRoute) },
                        onQueryChange = viewModel::onQueryChange,
                        onAppClicked = viewModel::onAppClicked,
                        onAddHomeApp = viewModel::addHomeApp,
                        onRemoveHomeApp = viewModel::removeHomeApp,
                        onUninstallApp = viewModel::uninstallApp,
                    )
                }

                entry<CustomizationRoute> {
                    val viewModel: CustomizationViewModel = viewModel(
                        factory = CustomizationViewModel.provideFactory(appContainer.launcherRepository)
                    )
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    CustomizationScreen(
                        state = state,
                        onBack = backStack::popIfNotRoot,
                        onEditShortcut = { slot -> backStack.pushSingleTop(ShortcutPickerRoute(slot)) },
                    )
                }

                entry<ShortcutPickerRoute> { route ->
                    val viewModel: ShortcutPickerViewModel = viewModel(
                        factory = ShortcutPickerViewModel.provideFactory(
                            repository = appContainer.launcherRepository,
                            slot = route.slot,
                        )
                    )
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    ShortcutPickerScreen(
                        slot = route.slot,
                        state = state,
                        onBack = backStack::popIfNotRoot,
                        onQueryChange = viewModel::onQueryChange,
                        onContactsSelected = {
                            viewModel.onContactsSelected()
                            backStack.popIfNotRoot()
                        },
                        onCameraSelected = {
                            viewModel.onCameraSelected()
                            backStack.popIfNotRoot()
                        },
                        onAppSelected = { app ->
                            viewModel.onAppSelected(app)
                            backStack.popIfNotRoot()
                        },
                    )
                }
            }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Composable
private fun CollectCommands(
    commands: Flow<LauncherCommand>,
    snackbarHostState: SnackbarHostState,
    context: Context,
    launchActivityForResult: (Intent) -> Unit,
) {
    LaunchedEffect(commands) {
        commands.collect { command ->
            when (command) {
                is LauncherCommand.LaunchInstalledApp -> {
                    launchInstalledApp(context, command.app)
                }

                is LauncherCommand.OpenPlayStoreSearch -> {
                    openPlayStore(context, command.query)
                }

                is LauncherCommand.OpenMapsSearch -> {
                    openGoogleMaps(context, command.query)
                }

                is LauncherCommand.OpenShortcut -> {
                    openShortcut(context, command.shortcut, snackbarHostState)
                }

                is LauncherCommand.OpenWebSearch -> {
                    openWebSearch(context, command.query)
                }

                is LauncherCommand.UninstallApp -> {
                    uninstallApp(
                        context = context,
                        app = command.app,
                        snackbarHostState = snackbarHostState,
                        launchActivityForResult = launchActivityForResult,
                    )
                }

                is LauncherCommand.ShowMessage -> {
                    snackbarHostState.showSnackbar(command.message)
                }
            }
        }
    }
}

private fun launchInstalledApp(context: Context, app: InstalledApp) {
    val intent = Intent().apply {
        component = ComponentName(app.key.packageName, app.key.activityName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private suspend fun openShortcut(
    context: Context,
    shortcut: ResolvedShortcut,
    snackbarHostState: SnackbarHostState,
) {
    val intent = when (shortcut.selection) {
        is ShortcutSelection.AppShortcut -> {
            val app = shortcut.installedApp ?: run {
                return showSnackbarMessage(
                    snackbarHostState,
                    "Selected shortcut app is no longer installed."
                )
            }
            Intent().apply {
                component = ComponentName(app.key.packageName, app.key.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        ShortcutSelection.SystemCamera -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        ShortcutSelection.SystemContacts -> Intent(
            Intent.ACTION_VIEW,
            ContactsContract.Contacts.CONTENT_URI
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val safeIntent = intent.resolveActivity(context.packageManager)
    if (safeIntent == null) {
        showSnackbarMessage(snackbarHostState, "${shortcut.label} is unavailable on this device.")
    } else {
        context.startActivity(intent)
    }
}

private fun openWebSearch(context: Context, query: String) {
    val webSearchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
        putExtra(SearchManager.QUERY, query)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val fallbackIntent = Intent(
        Intent.ACTION_VIEW,
        "https://www.google.com/search?q=${
            URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        }".toUri()
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    try {
        context.startActivity(
            if (webSearchIntent.resolveActivity(context.packageManager) != null) {
                webSearchIntent
            } else {
                fallbackIntent
            }
        )
    } catch (_: ActivityNotFoundException) {
        context.startActivity(fallbackIntent)
    }
}

private fun openPlayStore(context: Context, query: String) {
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        "market://search?q=${Uri.encode(query)}&c=apps".toUri()
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val fallbackIntent = Intent(
        Intent.ACTION_VIEW,
        "https://play.google.com/store/search?q=${Uri.encode(query)}&c=apps".toUri()
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    try {
        context.startActivity(
            if (marketIntent.resolveActivity(context.packageManager) != null) {
                marketIntent
            } else {
                fallbackIntent
            }
        )
    } catch (_: ActivityNotFoundException) {
        context.startActivity(fallbackIntent)
    }
}

private fun openGoogleMaps(context: Context, query: String) {
    val appIntent = Intent(
        Intent.ACTION_VIEW,
        "geo:0,0?q=${Uri.encode(query)}".toUri()
    ).apply {
        `package` = "com.google.android.apps.maps"
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val fallbackIntent = Intent(
        Intent.ACTION_VIEW,
        "https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}".toUri()
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    try {
        context.startActivity(
            if (appIntent.resolveActivity(context.packageManager) != null) {
                appIntent
            } else {
                fallbackIntent
            }
        )
    } catch (_: ActivityNotFoundException) {
        context.startActivity(fallbackIntent)
    }
}

private suspend fun uninstallApp(
    context: Context,
    app: InstalledApp,
    snackbarHostState: SnackbarHostState,
    launchActivityForResult: (Intent) -> Unit,
) {
    val packageUri = Uri.fromParts("package", app.key.packageName, null)
    val uninstallIntent = Intent(Intent.ACTION_DELETE, packageUri)
    val appInfoIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)

    suspend fun openAppInfoWithMessage(message: String) {
        if (appInfoIntent.resolveActivity(context.packageManager) != null) {
            launchActivityForResult(appInfoIntent)
        }
        showSnackbarMessage(snackbarHostState, message)
    }

    val appFlags = context.packageManager.applicationFlagsFor(app.key.packageName)
        ?: return showSnackbarMessage(
            snackbarHostState,
            context.getString(R.string.app_missing),
        )
    if (!canUninstallFromLauncher(appFlags)) {
        openAppInfoWithMessage(context.getString(R.string.system_app_info_opened))
        return
    }
    if (uninstallIntent.resolveActivity(context.packageManager) == null) {
        openAppInfoWithMessage(context.getString(R.string.uninstall_unavailable_info_opened))
        return
    }

    try {
        launchActivityForResult(uninstallIntent)
    } catch (_: SecurityException) {
        openAppInfoWithMessage(context.getString(R.string.uninstall_blocked_info_opened))
    } catch (_: ActivityNotFoundException) {
        openAppInfoWithMessage(context.getString(R.string.uninstall_unavailable_info_opened))
    }
}

private suspend fun showSnackbarMessage(
    snackbarHostState: SnackbarHostState,
    message: String,
) {
    snackbarHostState.showSnackbar(message)
}

private fun PackageManager.applicationFlagsFor(packageName: String): Int? = try {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0)).flags
    } else {
        @Suppress("DEPRECATION")
        getApplicationInfo(packageName, 0).flags
    }
} catch (_: PackageManager.NameNotFoundException) {
    null
}

private fun canUninstallFromLauncher(flags: Int): Boolean {
    val isSystemApp = flags and ApplicationInfo.FLAG_SYSTEM != 0
    val isUpdatedSystemApp = flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
    return !isSystemApp || isUpdatedSystemApp
}
