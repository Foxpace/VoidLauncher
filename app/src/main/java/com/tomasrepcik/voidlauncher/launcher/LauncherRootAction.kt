package com.tomasrepcik.voidlauncher.launcher

import com.tomasrepcik.voidlauncher.storage.launcher.RepositoryWriteResult
import com.tomasrepcik.voidlauncher.launcher.action.LauncherAction
import com.tomasrepcik.voidlauncher.launcher.error.AppError
import kotlinx.coroutines.channels.SendChannel

internal sealed interface LauncherRootAction {
    data class Open(val action: LauncherAction) : LauncherRootAction
    data class ShowError(val error: AppError) : LauncherRootAction
    data class ShowMessage(val message: String) : LauncherRootAction
    data class ShowAppAddedConfirmation(val appLabel: String) : LauncherRootAction
    data object CloseScreen : LauncherRootAction
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
