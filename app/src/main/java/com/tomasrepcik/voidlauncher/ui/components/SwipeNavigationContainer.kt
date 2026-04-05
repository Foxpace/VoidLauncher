package com.tomasrepcik.voidlauncher.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun SwipeNavigationContainer(
    modifier: Modifier = Modifier,
    swipeThreshold: Dp = 72.dp,
    onOpen: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val resolvedModifier = if (onOpen == null && onClose == null) {
        modifier
    } else {
        modifier.pointerInput(layoutDirection, onOpen, onClose, swipeThreshold) {
            val thresholdPx = swipeThreshold.toPx()
            var horizontalDragTotal = 0f

            detectHorizontalDragGestures(
                onHorizontalDrag = { _, dragAmount ->
                    horizontalDragTotal += dragAmount
                },
                onDragEnd = {
                    when {
                        horizontalDragTotal <= -thresholdPx -> {
                            if (layoutDirection == LayoutDirection.Ltr) {
                                onOpen?.invoke()
                            } else {
                                onClose?.invoke()
                            }
                        }

                        horizontalDragTotal >= thresholdPx -> {
                            if (layoutDirection == LayoutDirection.Ltr) {
                                onClose?.invoke()
                            } else {
                                onOpen?.invoke()
                            }
                        }
                    }
                    horizontalDragTotal = 0f
                },
                onDragCancel = {
                    horizontalDragTotal = 0f
                },
            )
        }
    }

    Box(modifier = resolvedModifier, content = content)
}
