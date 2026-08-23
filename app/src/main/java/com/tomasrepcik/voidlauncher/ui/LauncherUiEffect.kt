package com.tomasrepcik.voidlauncher.ui

import com.tomasrepcik.voidlauncher.data.repository.RepositoryMutationOutcome
import com.tomasrepcik.voidlauncher.domain.action.LauncherAction
import com.tomasrepcik.voidlauncher.domain.error.AppError
import kotlinx.coroutines.channels.SendChannel

internal sealed interface LauncherUiEffect {
    data class Action(val action: LauncherAction) : LauncherUiEffect
    data class Error(val error: AppError) : LauncherUiEffect
    data class Feedback(val message: String) : LauncherUiEffect
    data object Completed : LauncherUiEffect
}

internal suspend fun SendChannel<LauncherUiEffect>.sendOutcome(
    outcome: RepositoryMutationOutcome,
    sendCompletion: Boolean = false,
) {
    when (outcome) {
        RepositoryMutationOutcome.Completed -> if (sendCompletion) send(LauncherUiEffect.Completed)
        is RepositoryMutationOutcome.Failed -> send(LauncherUiEffect.Error(outcome.error))
    }
}
