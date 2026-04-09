package com.tomasrepcik.voidlauncher.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R

@Composable
fun LauncherSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    testTag: String? = null,
    onSubmit: (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFocused by remember { mutableStateOf(false) }

    fun dismiss() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun submit() {
        dismiss()
        onSubmit?.invoke()
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            )
            .onFocusChanged { isFocused = it.isFocused }
            .fillMaxWidth()
            .then(
                if (testTag != null) {
                    Modifier.testTag(testTag)
                } else {
                    Modifier
                }
            ),
        placeholder = { Text(placeholderText) },
        trailingIcon = {
            val icon = if (isFocused) Icons.Outlined.Close else Icons.Outlined.Search
            val contentDescription = if (isFocused) {
                stringResource(R.string.dismiss_search)
            } else {
                stringResource(R.string.search)
            }
            IconButton(onClick = if (isFocused) ::dismiss else ::submit) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = if (onSubmit != null) ImeAction.Search else ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onSearch = { submit() },
            onDone = { submit() },
        ),
    )
}
