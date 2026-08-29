package com.tomasrepcik.voidlauncher.ui

import com.tomasrepcik.voidlauncher.data.repository.RepositoryWriteResult
import com.tomasrepcik.voidlauncher.domain.action.LauncherAction
import com.tomasrepcik.voidlauncher.domain.error.AppError
import kotlinx.coroutines.channels.SendChannel

internal sealed interface LauncherRootAction {
    data class Open(val action: LauncherAction) : LauncherRootAction
    data class ShowError(val error: AppError) : LauncherRootAction
    data class ShowMessage(val message: String) : LauncherRootAction
    data class ShowConfirmation(val confirmation: LauncherConfirmation) : LauncherRootAction
    data object CloseScreen : LauncherRootAction
}

internal sealed interface LauncherConfirmation {
    data class AppAddedToHome(val appLabel: String) : LauncherConfirmation
}

internal suspend fun SendChannel<LauncherRootAction>.sendWriteResult(
    result: RepositoryWriteResult,
    sendCompletion: Boolean = false,
) {
    when (result) {
        RepositoryWriteResult.Completed -> if (sendCompletion) send(LauncherRootAction.CloseScreen)
        is RepositoryWriteResult.Failed -> send(LauncherRootAction.ShowError(result.error))
    }
}
