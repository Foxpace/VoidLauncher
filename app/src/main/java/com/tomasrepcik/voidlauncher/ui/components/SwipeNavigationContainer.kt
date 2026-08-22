package com.tomasrepcik.voidlauncher.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs

data class SwipeNavigationConfig(
    val horizontalThreshold: Dp = 72.dp,
    val bottomActivationZone: Dp = 28.dp,
    val bottomThreshold: Dp = 36.dp,
    val bottomInset: Dp = 0.dp,
)

data class SwipeNavigationActions(
    val onOpen: (() -> Unit)? = null,
    val onClose: (() -> Unit)? = null,
    val onBottomSwipeUp: (() -> Unit)? = null,
) {
    val isEmpty: Boolean
        get() = onOpen == null && onClose == null && onBottomSwipeUp == null
}

@Composable
fun SwipeNavigationContainer(
    modifier: Modifier = Modifier,
    config: SwipeNavigationConfig = SwipeNavigationConfig(),
    actions: SwipeNavigationActions = SwipeNavigationActions(),
    content: @Composable BoxScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val swipeModifier = modifier.swipeInput(config, actions, layoutDirection)
    Box(modifier = swipeModifier, content = content)
}

private fun Modifier.swipeInput(
    config: SwipeNavigationConfig,
    actions: SwipeNavigationActions,
    layoutDirection: LayoutDirection,
): Modifier = if (actions.isEmpty) {
    this
} else {
    pointerInput(config, actions, layoutDirection) {
        detectNavigationGestures(config, actions, layoutDirection)
    }
}

private suspend fun PointerInputScope.detectNavigationGestures(
    config: SwipeNavigationConfig,
    actions: SwipeNavigationActions,
    layoutDirection: LayoutDirection,
) {
    val thresholds = GestureThresholds(
        horizontal = config.horizontalThreshold.toPx(),
        bottomActivation = config.bottomActivationZone.toPx(),
        bottomSwipe = config.bottomThreshold.toPx(),
        bottomInset = config.bottomInset.toPx(),
    )
    awaitEachGesture {
        detectNavigationGesture(thresholds, actions, layoutDirection)
    }
}

private suspend fun AwaitPointerEventScope.detectNavigationGesture(
    thresholds: GestureThresholds,
    actions: SwipeNavigationActions,
    layoutDirection: LayoutDirection,
) {
    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    val tracker = GestureTracker(
        startedFromBottom = thresholds.startedFromBottom(size.height, down.position.y),
        canSwipeFromBottom = actions.onBottomSwipeUp != null,
        thresholds = thresholds,
    )
    var change: PointerInputChange?
    var gesture: NavigationGesture?
    do {
        change = nextChange(down.id)
        gesture = change?.let(tracker::update)
    } while (change?.pressed == true && gesture == null)

    if (gesture != null && change != null) {
        change.consume()
        actions.invoke(gesture, layoutDirection)
    }
}

private suspend fun AwaitPointerEventScope.nextChange(pointerId: PointerId): PointerInputChange? =
    awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == pointerId }

private data class GestureThresholds(
    val horizontal: Float,
    val bottomActivation: Float,
    val bottomSwipe: Float,
    val bottomInset: Float,
) {
    fun startedFromBottom(height: Int, y: Float): Boolean {
        val bottom = height - bottomInset
        return y in (bottom - bottomActivation)..bottom
    }
}

private class GestureTracker(
    private val startedFromBottom: Boolean,
    private val canSwipeFromBottom: Boolean,
    private val thresholds: GestureThresholds,
) {
    private var totalX = 0f
    private var totalY = 0f

    fun update(change: PointerInputChange): NavigationGesture? {
        val delta = change.positionChange()
        totalX += delta.x
        totalY += delta.y
        val isVertical = abs(totalY) > abs(totalX)
        val isHorizontal = abs(totalX) > abs(totalY)
        return when {
            canSwipeFromBottom && startedFromBottom && isVertical && totalY <= -thresholds.bottomSwipe -> {
                NavigationGesture.BottomSwipeUp
            }
            isHorizontal && totalX <= -thresholds.horizontal -> NavigationGesture.SwipeLeft
            isHorizontal && totalX >= thresholds.horizontal -> NavigationGesture.SwipeRight
            else -> null
        }
    }
}

private enum class NavigationGesture {
    SwipeLeft,
    SwipeRight,
    BottomSwipeUp,
}

private fun SwipeNavigationActions.invoke(
    gesture: NavigationGesture,
    layoutDirection: LayoutDirection,
) {
    when (gesture) {
        NavigationGesture.BottomSwipeUp -> onBottomSwipeUp?.invoke()
        NavigationGesture.SwipeLeft -> {
            if (layoutDirection == LayoutDirection.Ltr) onOpen?.invoke() else onClose?.invoke()
        }
        NavigationGesture.SwipeRight -> {
            if (layoutDirection == LayoutDirection.Ltr) onClose?.invoke() else onOpen?.invoke()
        }
    }
}
