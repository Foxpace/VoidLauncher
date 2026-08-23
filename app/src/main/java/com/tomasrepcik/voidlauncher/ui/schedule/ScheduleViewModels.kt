package com.tomasrepcik.voidlauncher.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepository
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepositoryState
import com.tomasrepcik.voidlauncher.data.repository.RepositoryMutationOutcome
import com.tomasrepcik.voidlauncher.domain.error.AppError
import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule
import com.tomasrepcik.voidlauncher.domain.schedule.ScheduleMutation
import com.tomasrepcik.voidlauncher.domain.search.InstalledAppSearch
import java.time.DayOfWeek
import java.util.UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduleListUiState(
    val schedules: List<AppSchedule> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface ScheduleListIntent {
    data class Delete(val id: String) : ScheduleListIntent
    data class SetEnabled(val schedule: AppSchedule, val enabled: Boolean) : ScheduleListIntent
}

sealed interface ScheduleEffect {
    data object Saved : ScheduleEffect
    data class Failed(val error: AppError) : ScheduleEffect
}

class ScheduleListViewModel(
    private val repository: LauncherRepository,
) : ViewModel() {
    private val effectChannel = Channel<ScheduleEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    val uiState: StateFlow<ScheduleListUiState> = repository.state.map { state ->
        val launcher = (state as? LauncherRepositoryState.Ready)?.launcher
        ScheduleListUiState(
            schedules = launcher?.schedules.orEmpty(),
            isLoading = launcher == null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScheduleListUiState(),
    )

    fun onIntent(intent: ScheduleListIntent) {
        when (intent) {
            is ScheduleListIntent.Delete -> viewModelScope.launch {
                repository.mutateSchedule(ScheduleMutation.Delete(intent.id)).sendFailure()
            }
            is ScheduleListIntent.SetEnabled -> viewModelScope.launch {
                val updated = intent.schedule.copy(enabled = intent.enabled)
                repository.mutateSchedule(ScheduleMutation.Save(updated)).sendFailure()
            }
        }
    }

    private suspend fun RepositoryMutationOutcome.sendFailure() {
        if (this is RepositoryMutationOutcome.Failed) effectChannel.send(ScheduleEffect.Failed(error))
    }

    companion object {
        fun provideFactory(repository: LauncherRepository) = viewModelFactory {
            initializer { ScheduleListViewModel(repository) }
        }
    }
}

data class ScheduleEditorUiState(
    val id: String? = null,
    val name: String = DEFAULT_SCHEDULE_NAME,
    val days: Set<DayOfWeek> = WEEKDAYS,
    val startMinute: Int = DEFAULT_START_MINUTE,
    val endMinute: Int = DEFAULT_END_MINUTE,
    val selectedAppKeys: Set<AppKey> = emptySet(),
    val enabled: Boolean = true,
    val appQuery: String = "",
    val isAppPickerOpen: Boolean = false,
    val installedApps: List<InstalledApp> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
) {
    val canSave: Boolean
        get() = days.isNotEmpty() && selectedAppKeys.isNotEmpty() && !isSaving
}

sealed interface ScheduleEditorIntent {
    data class NameChanged(val value: String) : ScheduleEditorIntent
    data class DayToggled(val day: DayOfWeek) : ScheduleEditorIntent
    data class DaysChanged(val days: Set<DayOfWeek>) : ScheduleEditorIntent
    data class StartTimeChanged(val minute: Int) : ScheduleEditorIntent
    data class EndTimeChanged(val minute: Int) : ScheduleEditorIntent
    data class AppToggled(val key: AppKey) : ScheduleEditorIntent
    data class AppQueryChanged(val value: String) : ScheduleEditorIntent
    data class EnabledChanged(val enabled: Boolean) : ScheduleEditorIntent
    data object OpenAppPicker : ScheduleEditorIntent
    data object CloseAppPicker : ScheduleEditorIntent
    data object Save : ScheduleEditorIntent
}

class ScheduleEditorViewModel(
    private val repository: LauncherRepository,
    private val scheduleId: String?,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val installedAppSearch: InstalledAppSearch = InstalledAppSearch(),
) : ViewModel() {
    private val draft = MutableStateFlow<ScheduleEditorUiState?>(null)
    private val saving = MutableStateFlow(false)
    private val effectChannel = Channel<ScheduleEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    val uiState: StateFlow<ScheduleEditorUiState> = combine(
        draft,
        repository.state,
        saving,
    ) { currentDraft, repositoryState, isSaving ->
        val launcher = (repositoryState as? LauncherRepositoryState.Ready)?.launcher
        val base = currentDraft ?: ScheduleEditorUiState()
        val filteredApps = installedAppSearch.filter(base.appQuery, launcher?.installedApps.orEmpty())
        base.copy(
            installedApps = filteredApps.sortedByDescending { it.key in base.selectedAppKeys },
            isLoading = currentDraft == null || launcher == null,
            isSaving = isSaving,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScheduleEditorUiState(),
    )

    init {
        viewModelScope.launch {
            val launcher = repository.state.filterIsInstance<LauncherRepositoryState.Ready>()
                .first().launcher
            val existing = launcher.schedules.firstOrNull { schedule -> schedule.id == scheduleId }
            draft.value = existing?.toEditorState() ?: ScheduleEditorUiState(
                selectedAppKeys = launcher.pinnedHomeApps.mapTo(mutableSetOf()) { it.key },
                isLoading = false,
            )
        }
    }

    fun onIntent(intent: ScheduleEditorIntent) {
        when (intent) {
            is ScheduleEditorIntent.NameChanged -> updateDraft { copy(name = intent.value) }
            is ScheduleEditorIntent.DayToggled -> updateDraft {
                copy(days = days.toggle(intent.day))
            }
            is ScheduleEditorIntent.DaysChanged -> updateDraft { copy(days = intent.days) }
            is ScheduleEditorIntent.StartTimeChanged -> updateDraft {
                copy(startMinute = intent.minute)
            }
            is ScheduleEditorIntent.EndTimeChanged -> updateDraft {
                copy(endMinute = intent.minute)
            }
            is ScheduleEditorIntent.AppToggled -> updateDraft {
                copy(selectedAppKeys = selectedAppKeys.toggle(intent.key))
            }
            is ScheduleEditorIntent.AppQueryChanged -> updateDraft { copy(appQuery = intent.value) }
            is ScheduleEditorIntent.EnabledChanged -> updateDraft { copy(enabled = intent.enabled) }
            ScheduleEditorIntent.OpenAppPicker -> updateDraft { copy(isAppPickerOpen = true) }
            ScheduleEditorIntent.CloseAppPicker -> updateDraft {
                copy(isAppPickerOpen = false, appQuery = "")
            }
            ScheduleEditorIntent.Save -> save()
        }
    }

    private fun updateDraft(transform: ScheduleEditorUiState.() -> ScheduleEditorUiState) {
        draft.update { current -> current?.transform() }
    }

    private fun save() {
        val current = draft.value ?: return
        if (!current.canSave || saving.value) return
        saving.value = true
        viewModelScope.launch {
            val schedule = AppSchedule(
                id = current.id ?: newId(),
                name = current.name.trim().ifEmpty { DEFAULT_SCHEDULE_NAME },
                days = current.days,
                startMinute = current.startMinute,
                endMinute = current.endMinute,
                appKeys = current.selectedAppKeys,
                enabled = current.enabled,
            )
            when (val outcome = repository.mutateSchedule(ScheduleMutation.Save(schedule))) {
                RepositoryMutationOutcome.Completed -> effectChannel.send(ScheduleEffect.Saved)
                is RepositoryMutationOutcome.Failed -> effectChannel.send(ScheduleEffect.Failed(outcome.error))
            }
            saving.value = false
        }
    }

    companion object {
        fun provideFactory(
            repository: LauncherRepository,
            scheduleId: String?,
            installedAppSearch: InstalledAppSearch,
        ) = viewModelFactory {
            initializer { ScheduleEditorViewModel(repository, scheduleId, installedAppSearch = installedAppSearch) }
        }
    }
}

private fun AppSchedule.toEditorState() = ScheduleEditorUiState(
    id = id,
    name = name,
    days = days,
    startMinute = startMinute,
    endMinute = endMinute,
    selectedAppKeys = appKeys,
    enabled = enabled,
    isLoading = false,
)

private fun <T> Set<T>.toggle(value: T): Set<T> =
    if (value in this) this - value else this + value

private const val DEFAULT_START_MINUTE = 9 * 60
private const val DEFAULT_END_MINUTE = 17 * 60
private const val DEFAULT_SCHEDULE_NAME = "My schedule"
