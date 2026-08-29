package com.tomasrepcik.voidlauncher.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.voidlauncher.home.data.HomeAppsRepository
import com.tomasrepcik.voidlauncher.appcatalog.data.InstalledAppsRepository
import com.tomasrepcik.voidlauncher.storage.launcher.RepositoryWriteResult
import com.tomasrepcik.voidlauncher.launcher.action.LauncherAction
import com.tomasrepcik.voidlauncher.appcatalog.search.InstalledAppSearch
import com.tomasrepcik.voidlauncher.launcher.LauncherRootAction
import com.tomasrepcik.voidlauncher.launcher.sendWriteResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DrawerViewModel(
    installedApps: InstalledAppsRepository,
    private val homeApps: HomeAppsRepository,
    installedAppSearch: InstalledAppSearch,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val rootActionChannel = Channel<LauncherRootAction>(capacity = Channel.BUFFERED)
    private val navigationChannel = Channel<DrawerNavigationEvent>(capacity = Channel.BUFFERED)

    internal val rootActions = rootActionChannel.receiveAsFlow()
    internal val navigation = navigationChannel.receiveAsFlow()

    val uiState: StateFlow<DrawerUiState> = combine(
        installedApps.apps,
        homeApps.data,
        query,
    ) { currentApps, currentHomeApps, currentQuery ->
        if (currentApps == null || currentHomeApps == null) {
            return@combine DrawerUiState(query = currentQuery, isLoading = true)
        }
        val filteredApps = installedAppSearch.filter(currentQuery, currentApps)
        val sectionLetters = filteredApps.associate { app ->
            app.key to installedAppSearch.sectionLetter(app.label)
        }
        val alphabetIndex = linkedMapOf<Char, Int>()
        filteredApps.forEachIndexed { index, app ->
            alphabetIndex.putIfAbsent(sectionLetters.getValue(app.key), index)
        }
        DrawerUiState(
            query = currentQuery,
            apps = filteredApps,
            pinnedAppKeys = currentHomeApps.keys,
            sectionLetters = sectionLetters,
            alphabetIndex = alphabetIndex,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DrawerUiState(isLoading = true),
    )

    fun onAction(action: DrawerAction) {
        when (action) {
            DrawerAction.Back -> navigationChannel.trySend(DrawerNavigationEvent.Back)
            DrawerAction.OpenCustomization ->
                navigationChannel.trySend(DrawerNavigationEvent.OpenCustomization)
            is DrawerAction.QueryChanged -> query.value = action.value
            is DrawerAction.OpenApp -> rootActionChannel.trySend(
                LauncherRootAction.Open(LauncherAction.LaunchInstalledApp(action.app)),
            )
            is DrawerAction.AddHomeApp -> runHomeAppWrite(
                addedAppLabel = action.app.label,
                write = { homeApps.add(action.app.key) },
            )
            is DrawerAction.RemoveHomeApp -> runHomeAppWrite { homeApps.remove(action.app.key) }
            is DrawerAction.UninstallApp -> rootActionChannel.trySend(
                LauncherRootAction.Open(LauncherAction.UninstallApp(action.app)),
            )
        }
    }

    private fun runHomeAppWrite(
        addedAppLabel: String? = null,
        write: suspend () -> RepositoryWriteResult,
    ) {
        viewModelScope.launch {
            val result = write()
            if (result == RepositoryWriteResult.Completed && addedAppLabel != null) {
                rootActionChannel.send(
                    LauncherRootAction.ShowAppAddedConfirmation(addedAppLabel),
                )
            } else {
                rootActionChannel.sendWriteResult(result)
            }
        }
    }
}
