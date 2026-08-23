package com.tomasrepcik.voidlauncher.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R

private const val TUTORIAL_PAGE_COUNT = 3
private const val TUTORIAL_ENTER_FADE_MILLIS = 220
private const val TUTORIAL_EXIT_FADE_MILLIS = 160
private const val TUTORIAL_TRANSITION_MILLIS = 320
private const val TUTORIAL_SLIDE_DIVISOR = 3

@Composable
fun NavigationTutorial(
    onFinish: () -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onFinish,
        modifier = Modifier.testTag("navigation_tutorial"),
        text = {
            AnimatedContent(
                targetState = page,
                transitionSpec = { tutorialPageTransition() },
                label = "tutorialPage",
            ) { targetPage ->
                TutorialPage(
                    page = targetPage,
                    content = tutorialContent(targetPage),
                )
            }
        },
        confirmButton = {
            TutorialNextButton(
                page = page,
                onNext = { page += 1 },
                onFinish = onFinish,
            )
        },
        dismissButton = {
            TutorialSecondaryButton(
                page = page,
                onPrevious = { page -= 1 },
                onFinish = onFinish,
            )
        },
    )
}

@Composable
private fun TutorialPage(
    page: Int,
    content: TutorialContent,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Icon(
            imageVector = content.icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = content.title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag("navigation_tutorial_title"),
        )
        Text(
            text = content.body,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(),
        )
        TutorialCallouts(page)
        PageIndicator(selectedPage = page)
    }
}

private fun AnimatedContentTransitionScope<Int>.tutorialPageTransition() =
    (fadeIn(animationSpec = tween(TUTORIAL_ENTER_FADE_MILLIS)) +
        slideInHorizontally(animationSpec = tween(TUTORIAL_TRANSITION_MILLIS)) { width ->
            val offset = width / TUTORIAL_SLIDE_DIVISOR
            if (targetState > initialState) offset else -offset
        })
        .togetherWith(
            fadeOut(animationSpec = tween(TUTORIAL_EXIT_FADE_MILLIS)) +
                slideOutHorizontally(animationSpec = tween(TUTORIAL_TRANSITION_MILLIS)) { width ->
                    val offset = width / TUTORIAL_SLIDE_DIVISOR
                    if (targetState > initialState) -offset else offset
                }
        )
        .using(
            SizeTransform(clip = false) { _, _ ->
                tween(durationMillis = TUTORIAL_TRANSITION_MILLIS)
            }
        )

@Composable
private fun TutorialNextButton(
    page: Int,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    val isLastPage = page == TUTORIAL_PAGE_COUNT - 1
    TextButton(
        onClick = if (isLastPage) onFinish else onNext,
        modifier = Modifier.testTag("navigation_tutorial_next"),
    ) {
        Text(stringResource(if (isLastPage) R.string.done else R.string.next))
    }
}

@Composable
private fun TutorialSecondaryButton(
    page: Int,
    onPrevious: () -> Unit,
    onFinish: () -> Unit,
) {
    val isFirstPage = page == 0
    TextButton(
        onClick = if (isFirstPage) onFinish else onPrevious,
        modifier = Modifier.testTag("navigation_tutorial_secondary"),
    ) {
        Text(stringResource(if (isFirstPage) R.string.skip else R.string.previous))
    }
}

@Composable
private fun tutorialContent(page: Int): TutorialContent = when (page) {
    0 -> TutorialContent(
        icon = Icons.AutoMirrored.Outlined.ArrowForward,
        title = stringResource(R.string.tutorial_navigation_title),
        body = stringResource(R.string.tutorial_navigation_body),
    )
    1 -> TutorialContent(
        icon = Icons.Outlined.TouchApp,
        title = stringResource(R.string.tutorial_shortcuts_title),
        body = stringResource(R.string.tutorial_shortcuts_body),
    )
    else -> TutorialContent(
        icon = Icons.Outlined.Schedule,
        title = stringResource(R.string.tutorial_schedules_title),
        body = stringResource(R.string.tutorial_schedules_body),
    )
}

@Composable
private fun TutorialCallouts(page: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (page) {
            0 -> {
                TutorialCallout(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    text = stringResource(R.string.tutorial_swipe_left),
                )
                TutorialCallout(
                    icon = Icons.AutoMirrored.Outlined.ArrowForward,
                    text = stringResource(R.string.tutorial_swipe_right),
                )
                TutorialCallout(
                    icon = Icons.Outlined.KeyboardArrowUp,
                    text = stringResource(R.string.tutorial_swipe_up),
                )
            }
            1 -> {
                TutorialCallout(
                    icon = Icons.Outlined.TouchApp,
                    text = stringResource(R.string.tutorial_shortcuts_location),
                )
                TutorialCallout(
                    icon = Icons.AutoMirrored.Outlined.ArrowForward,
                    text = stringResource(R.string.tutorial_shortcuts_settings),
                )
            }
            else -> TutorialCallout(
                icon = Icons.Outlined.Schedule,
                text = stringResource(R.string.tutorial_schedule_path),
            )
        }
    }
}

@Composable
private fun TutorialCallout(
    icon: ImageVector,
    text: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PageIndicator(selectedPage: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(TUTORIAL_PAGE_COUNT) { page ->
            Surface(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (page == selectedPage) 10.dp else 8.dp),
                shape = CircleShape,
                color = if (page == selectedPage) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
            ) {}
        }
    }
}

private data class TutorialContent(
    val icon: ImageVector,
    val title: String,
    val body: String,
)
