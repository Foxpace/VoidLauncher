package com.tomasrepcik.voidlauncher.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun SwipeNavigationContainer(
    modifier: Modifier = Modifier,
    swipeThreshold: Dp = 72.dp,
    bottomSwipeActivationZone: Dp = 28.dp,
    bottomSwipeThreshold: Dp = 36.dp,
    bottomSwipeBottomInset: Dp = 0.dp,
    onOpen: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    onBottomSwipeUp: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val resolvedModifier = if (onOpen == null && onClose == null && onBottomSwipeUp == null) {
        modifier
    } else {
        modifier.pointerInput(
            layoutDirection,
            onOpen,
            onClose,
            onBottomSwipeUp,
            swipeThreshold,
            bottomSwipeActivationZone,
            bottomSwipeThreshold,
            bottomSwipeBottomInset,
        ) {
            val horizontalThresholdPx = swipeThreshold.toPx()
            val bottomActivationPx = bottomSwipeActivationZone.toPx()
            val bottomThresholdPx = bottomSwipeThreshold.toPx()
            val bottomInsetPx = bottomSwipeBottomInset.toPx()

            awaitEachGesture {
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial,
                )
                val bottomBandTop = size.height - bottomInsetPx - bottomActivationPx
                val bottomBandBottom = size.height - bottomInsetPx
                val startedFromBottom = down.position.y in bottomBandTop..bottomBandBottom
                var totalDeltaX = 0f
                var totalDeltaY = 0f
                var handled = false

                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                    val delta = change.positionChange()
                    totalDeltaX += delta.x
                    totalDeltaY += delta.y

                    when {
                        startedFromBottom &&
                            onBottomSwipeUp != null &&
                            totalDeltaY <= -bottomThresholdPx &&
                            abs(totalDeltaY) > abs(totalDeltaX) -> {
                            onBottomSwipeUp.invoke()
                            handled = true
                        }

                        abs(totalDeltaX) > abs(totalDeltaY) &&
                            totalDeltaX <= -horizontalThresholdPx -> {
                            if (layoutDirection == LayoutDirection.Ltr) {
                                onOpen?.invoke()
                            } else {
                                onClose?.invoke()
                            }
                            handled = true
                        }

                        abs(totalDeltaX) > abs(totalDeltaY) &&
                            totalDeltaX >= horizontalThresholdPx -> {
                            if (layoutDirection == LayoutDirection.Ltr) {
                                onClose?.invoke()
                            } else {
                                onOpen?.invoke()
                            }
                            handled = true
                        }
                    }

                    if (handled) {
                        change.consume()
                        break
                    }

                    if (!change.pressed) {
                        break
                    }
                }
            }
        }
    }

    Box(modifier = resolvedModifier, content = content)
}
