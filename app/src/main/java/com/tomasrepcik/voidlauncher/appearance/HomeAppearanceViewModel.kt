package com.tomasrepcik.voidlauncher.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.voidlauncher.launcher.LauncherPreferences
import com.tomasrepcik.voidlauncher.customization.data.PreferencesRepository
import com.tomasrepcik.voidlauncher.storage.launcher.RepositoryWriteResult
import com.tomasrepcik.voidlauncher.launcher.error.AppError
import com.tomasrepcik.voidlauncher.launcher.error.AppErrorKind
import com.tomasrepcik.voidlauncher.launcher.error.AppOperation
import com.tomasrepcik.voidlauncher.launcher.error.ErrorRecovery
import com.tomasrepcik.voidlauncher.launcher.LauncherRootAction
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class HomeAppearanceViewModel(
    private val preferences: PreferencesRepository,
    private val keepBackgroundReadAccess: (String) -> Result<Unit>,
    private val releaseBackgroundReadAccess: (String) -> Unit,
    private val readBackground: suspend (String) -> HomeBackgroundImage?,
) : ViewModel() {
    private val backgroundChangeLock = Mutex()
    private val mutableState = MutableStateFlow(HomeAppearanceState())
    val state: StateFlow<HomeAppearanceState> = mutableState.asStateFlow()

    private val rootActionChannel = Channel<LauncherRootAction>(Channel.BUFFERED)
    val rootActions = rootActionChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            preferences.preferences
                .filterNotNull()
                .collectLatest(::loadAppearance)
        }
    }

    fun onAction(action: HomeAppearanceAction) {
        when (action) {
            is HomeAppearanceAction.SelectBackground -> saveBackground(action.uri)
            HomeAppearanceAction.RestoreDefaultBackground -> saveBackground(null)
            is HomeAppearanceAction.SetUseBackgroundColors ->
                saveUseBackgroundColors(action.enabled)
        }
    }

    private fun saveUseBackgroundColors(enabled: Boolean) {
        viewModelScope.launch {
            rootActionChannel.sendErrorIfFailed(preferences.setUseBackgroundColors(enabled))
        }
    }

    private fun saveBackground(uri: String?) {
        viewModelScope.launch {
            backgroundChangeLock.withLock { persistBackground(uri) }
        }
    }

    private suspend fun persistBackground(uri: String?) {
        val previousUri = mutableState.value.backgroundUri
        val accessResult = uri?.let(keepBackgroundReadAccess)
        if (accessResult?.isFailure == true) {
            rootActionChannel.send(
                LauncherRootAction.ShowError(backgroundAccessError(accessResult.exceptionOrNull())),
            )
            return
        }
        when (val result = preferences.setHomeBackground(uri)) {
            RepositoryWriteResult.Completed -> {
                if (previousUri != uri) previousUri?.let(releaseBackgroundReadAccess)
            }
            is RepositoryWriteResult.Failed -> {
                if (uri != null && previousUri != uri) releaseBackgroundReadAccess(uri)
                rootActionChannel.send(LauncherRootAction.ShowError(result.error))
            }
        }
    }

    private fun backgroundAccessError(cause: Throwable?) = AppError(
        kind = AppErrorKind.BACKGROUND_ACCESS_FAILED,
        operation = AppOperation.SAVE_HOME_BACKGROUND,
        recovery = ErrorRecovery.NONE,
        cause = cause,
    )

    private suspend fun loadAppearance(preferences: LauncherPreferences) {
        val current = mutableState.value
        val uriChanged = current.backgroundUri != preferences.homeBackgroundUri
        mutableState.value = current.copy(
            backgroundUri = preferences.homeBackgroundUri,
            useBackgroundColors = preferences.useBackgroundColors,
            background = if (uriChanged) null else current.background,
            isLoadingBackground = uriChanged && preferences.homeBackgroundUri != null,
        )
        if (!uriChanged) return

        val loaded = preferences.homeBackgroundUri?.let { readBackground(it) }
        mutableState.update { state ->
            if (state.backgroundUri == preferences.homeBackgroundUri) {
                state.copy(background = loaded, isLoadingBackground = false)
            } else {
                state
            }
        }
    }
}

private suspend fun Channel<LauncherRootAction>.sendErrorIfFailed(
    result: RepositoryWriteResult,
) {
    if (result is RepositoryWriteResult.Failed) {
        send(LauncherRootAction.ShowError(result.error))
    }
}
