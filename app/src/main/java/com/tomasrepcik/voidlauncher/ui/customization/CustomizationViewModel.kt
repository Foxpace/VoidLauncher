package com.tomasrepcik.voidlauncher.ui.customization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepository
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepositoryState
import com.tomasrepcik.voidlauncher.domain.search.InstalledAppSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CustomizationUiState(
    val shortcuts: List<ResolvedShortcut> = emptyList(),
)

class CustomizationViewModel(
    repository: LauncherRepository,
) : ViewModel() {

    val uiState: StateFlow<CustomizationUiState> =
        repository.state.map { repositoryState ->
            val shortcuts = (repositoryState as? LauncherRepositoryState.Ready)
                ?.launcher?.bottomShortcuts.orEmpty()
            CustomizationUiState(
                shortcuts = shortcuts.sortedBy { it.slot.ordinal },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CustomizationUiState(),
        )

    companion object {
        fun provideFactory(repository: LauncherRepository) = viewModelFactory {
            initializer {
                CustomizationViewModel(repository)
            }
        }
    }
}

data class ShortcutPickerUiState(
    val query: String = "",
    val apps: List<InstalledApp> = emptyList(),
    val isLoading: Boolean = true,
)

class ShortcutPickerViewModel(
    private val slot: ShortcutSlot,
    private val repository: LauncherRepository,
    installedAppSearch: InstalledAppSearch,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<ShortcutPickerUiState> = combine(
        repository.state,
        query,
    ) { repositoryState, currentQuery ->
        val installedApps = (repositoryState as? LauncherRepositoryState.Ready)
            ?.launcher?.installedApps
            ?: return@combine ShortcutPickerUiState(query = currentQuery, isLoading = true)
        val filtered = installedAppSearch.filter(currentQuery, installedApps)
        ShortcutPickerUiState(
            query = currentQuery,
            apps = filtered,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ShortcutPickerUiState(isLoading = true),
    )

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onAppSelected(app: InstalledApp) {
        viewModelScope.launch {
            repository.saveShortcut(slot, ShortcutSelection.AppShortcut(app.key))
        }
    }

    fun onContactsSelected() {
        viewModelScope.launch {
            repository.saveShortcut(slot, ShortcutSelection.SystemContacts)
        }
    }

    fun onCameraSelected() {
        viewModelScope.launch {
            repository.saveShortcut(slot, ShortcutSelection.SystemCamera)
        }
    }

    companion object {
        fun provideFactory(
            repository: LauncherRepository,
            slot: ShortcutSlot,
            installedAppSearch: InstalledAppSearch,
        ) = viewModelFactory {
            initializer {
                ShortcutPickerViewModel(slot, repository, installedAppSearch)
            }
        }
    }
}
