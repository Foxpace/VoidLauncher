package com.tomasrepcik.voidlauncher.ui.home.appearance

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

internal class SystemImageSelector(
    private val openDocument: () -> Unit,
) {
    fun chooseImage() = openDocument()
}

@Composable
internal fun rememberSystemImageSelector(
    onImageSelected: (String) -> Unit,
): SystemImageSelector {
    val currentOnImageSelected = rememberUpdatedState(onImageSelected)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.toString()?.let(currentOnImageSelected.value)
    }
    return remember(launcher) {
        SystemImageSelector { launcher.launch(arrayOf("image/*")) }
    }
}
