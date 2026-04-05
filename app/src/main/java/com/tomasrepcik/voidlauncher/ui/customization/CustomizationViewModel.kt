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
    private val repository: LauncherRepository,
) : ViewModel() {

    val uiState: StateFlow<CustomizationUiState> =
        repository.observeBottomShortcuts().map { shortcuts ->
            CustomizationUiState(
                shortcuts = shortcuts.sortedBy { it.slot.ordinal },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CustomizationUiState(),
        )

    init {
        viewModelScope.launch {
            repository.ensureDefaults()
        }
    }

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
)

class ShortcutPickerViewModel(
    private val slot: ShortcutSlot,
    private val repository: LauncherRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<ShortcutPickerUiState> = combine(
        repository.observeInstalledApps(),
        query,
    ) { installedApps, currentQuery ->
        val filtered = if (currentQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter {
                it.label.contains(currentQuery.trim(), ignoreCase = true)
            }
        }
        ShortcutPickerUiState(
            query = currentQuery,
            apps = filtered,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ShortcutPickerUiState(),
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
        ) = viewModelFactory {
            initializer {
                ShortcutPickerViewModel(slot, repository)
            }
        }
    }
}
