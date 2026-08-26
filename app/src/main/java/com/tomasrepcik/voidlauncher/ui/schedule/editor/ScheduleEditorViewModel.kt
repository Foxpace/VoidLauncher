package com.tomasrepcik.voidlauncher.ui.schedule.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.repository.HomeAppsData
import com.tomasrepcik.voidlauncher.data.repository.HomeAppsRepository
import com.tomasrepcik.voidlauncher.data.repository.InstalledAppsRepository
import com.tomasrepcik.voidlauncher.data.repository.ScheduleRepository
import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule
import com.tomasrepcik.voidlauncher.domain.search.InstalledAppSearch
import com.tomasrepcik.voidlauncher.ui.LauncherRootAction
import com.tomasrepcik.voidlauncher.ui.sendWriteResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class ScheduleEditorRepositoryData(
    val schedules: List<AppSchedule>?,
    val installedApps: List<InstalledApp>?,
    val homeApps: HomeAppsData?,
) {
    val isReady: Boolean
        get() = schedules != null && installedApps != null && homeApps != null
}

class ScheduleEditorViewModel(
    private val schedules: ScheduleRepository,
    installedApps: InstalledAppsRepository,
    homeApps: HomeAppsRepository,
    private val scheduleId: String?,
    installedAppSearch: InstalledAppSearch,
    private val scheduleIdFactory: ScheduleIdFactory,
) : ViewModel() {
    private val draft = MutableStateFlow<ScheduleEditorUiState?>(null)
    private val saving = MutableStateFlow(false)
    private val rootActionChannel = Channel<LauncherRootAction>(Channel.BUFFERED)
    private val navigationChannel = Channel<ScheduleEditorNavigationEvent>(Channel.BUFFERED)
    internal val rootActions = rootActionChannel.receiveAsFlow()
    internal val navigation = navigationChannel.receiveAsFlow()

    private val repositoryData = combine(
        schedules.schedules,
        installedApps.apps,
        homeApps.data,
    ) { currentSchedules, currentApps, currentHomeApps ->
        ScheduleEditorRepositoryData(currentSchedules, currentApps, currentHomeApps)
    }

    val uiState: StateFlow<ScheduleEditorUiState> = combine(
        draft,
        repositoryData,
        saving,
    ) { currentDraft, data, isSaving ->
        val base = currentDraft ?: ScheduleEditorUiState()
        val filteredApps = installedAppSearch.filter(base.appQuery, data.installedApps.orEmpty())
        base.copy(
            installedApps = filteredApps.sortedByDescending { it.key in base.selectedAppKeys },
            isLoading = currentDraft == null || !data.isReady,
            isSaving = isSaving,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScheduleEditorUiState(),
    )

    init {
        viewModelScope.launch {
            val data = repositoryData.first { it.isReady }
            val existing = data.schedules.orEmpty().firstOrNull { schedule -> schedule.id == scheduleId }
            val defaultHomeApps = data.homeApps?.apps.orEmpty()
            draft.value = existing?.toEditorState() ?: ScheduleEditorUiState(
                selectedAppKeys = defaultHomeApps.mapTo(mutableSetOf()) { it.key },
                isLoading = false,
            )
        }
    }

    fun onAction(action: ScheduleEditorAction) {
        when (action) {
            ScheduleEditorAction.Back ->
                navigationChannel.trySend(ScheduleEditorNavigationEvent.Back)
            ScheduleEditorAction.SaveSchedule -> saveSchedule()
            is ScheduleEditorAction.Update -> updateDraft { reduce(action) }
        }
    }

    private fun updateDraft(transform: ScheduleEditorUiState.() -> ScheduleEditorUiState) {
        draft.update { current -> current?.transform() }
    }

    private fun saveSchedule() {
        val current = draft.value ?: return
        if (!current.canSave || saving.value) return
        saving.value = true
        viewModelScope.launch {
            val schedule = AppSchedule(
                id = current.id ?: scheduleIdFactory.create(),
                name = current.name.trim().ifEmpty { DEFAULT_SCHEDULE_NAME },
                days = current.days,
                startMinute = current.startMinute,
                endMinute = current.endMinute,
                appKeys = current.selectedAppKeys,
                enabled = current.enabled,
            )
            rootActionChannel.sendWriteResult(
                schedules.save(schedule),
                sendCompletion = true,
            )
            saving.value = false
        }
    }
}
