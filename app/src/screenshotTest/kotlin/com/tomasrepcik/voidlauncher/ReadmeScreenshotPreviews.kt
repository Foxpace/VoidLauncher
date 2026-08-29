@file:Suppress("MagicNumber")

package com.tomasrepcik.voidlauncher

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.tomasrepcik.voidlauncher.launcher.AppKey
import com.tomasrepcik.voidlauncher.launcher.InstalledApp
import com.tomasrepcik.voidlauncher.launcher.ResolvedShortcut
import com.tomasrepcik.voidlauncher.launcher.ShortcutSelection
import com.tomasrepcik.voidlauncher.launcher.ShortcutSlot
import com.tomasrepcik.voidlauncher.customization.CustomizationScreen
import com.tomasrepcik.voidlauncher.customization.CustomizationActions
import com.tomasrepcik.voidlauncher.customization.CustomizationUiState
import com.tomasrepcik.voidlauncher.design.components.LocalAppIconContent
import com.tomasrepcik.voidlauncher.drawer.AppDrawerActions
import com.tomasrepcik.voidlauncher.drawer.AppDrawerScreen
import com.tomasrepcik.voidlauncher.drawer.DrawerUiState
import com.tomasrepcik.voidlauncher.home.HomeActions
import com.tomasrepcik.voidlauncher.home.HomeScreen
import com.tomasrepcik.voidlauncher.home.HomeUiState
import com.tomasrepcik.voidlauncher.appearance.HomeAppearanceActions
import com.tomasrepcik.voidlauncher.appearance.HomeAppearanceState
import com.tomasrepcik.voidlauncher.schedule.editor.ScheduleEditorScreen
import com.tomasrepcik.voidlauncher.schedule.editor.ScheduleEditorUiState
import com.tomasrepcik.voidlauncher.design.theme.VoidLauncherTheme
import java.time.DayOfWeek

private const val README_WIDTH_DP = 411
private const val README_HEIGHT_DP = 891
private const val HOME_APP_COUNT = 5
private const val SCHEDULE_APP_COUNT = 4

private val mockIconColors = listOf(
    Color(0xFF6750A4),
    Color(0xFF006A6A),
    Color(0xFF9C4048),
    Color(0xFF386A20),
    Color(0xFF7D5260),
    Color(0xFF3F5F90),
    Color(0xFF8C5000),
    Color(0xFF006C4C),
    Color(0xFF5D5E62),
    Color(0xFF6E5D00),
)

private val sampleApps = listOf(
    sampleApp("Calendar"),
    sampleApp("Camera"),
    sampleApp("Files"),
    sampleApp("Maps"),
    sampleApp("Messages"),
    sampleApp("Music"),
    sampleApp("Notes"),
    sampleApp("Photos"),
    sampleApp("Settings"),
    sampleApp("Signal"),
)

private val sampleShortcuts = listOf(
    ResolvedShortcut(
        slot = ShortcutSlot.LEFT,
        label = "Contacts",
        selection = ShortcutSelection.SystemContacts,
    ),
    ResolvedShortcut(
        slot = ShortcutSlot.RIGHT,
        label = "Camera",
        selection = ShortcutSelection.SystemCamera,
    ),
)

private val homeActions = HomeActions(
    onQueryChange = {},
    onPrimarySearch = {},
    onBrowserSearch = {},
    onPlayStoreSearch = {},
    onMapsSearch = {},
    onAppClicked = {},
    onShortcutClicked = {},
    onOpenDrawer = {},
    onOpenSchedules = {},
    onRemoveHomeApp = {},
    onRenameHomeApp = { _, _ -> },
    onUninstallApp = {},
    onReorderHomeApps = { _, _ -> },
)

private val drawerActions = AppDrawerActions(
    onBack = {},
    onOpenSettings = {},
    onQueryChange = {},
    onAppClicked = {},
    onAddHomeApp = {},
    onRemoveHomeApp = {},
    onUninstallApp = {},
)

@PreviewTest
@Preview(
    name = "README Home",
    widthDp = README_WIDTH_DP,
    heightDp = README_HEIGHT_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
fun ReadmeHome() {
    ReadmePreview {
        HomeScreen(
            state = HomeUiState(
                homeApps = sampleApps.take(HOME_APP_COUNT),
                shortcuts = sampleShortcuts,
                isLoading = false,
            ),
            appearance = HomeAppearanceState(),
            actions = homeActions,
        )
    }
}

@PreviewTest
@Preview(
    name = "README Drawer",
    widthDp = README_WIDTH_DP,
    heightDp = README_HEIGHT_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
fun ReadmeDrawer() {
    ReadmePreview {
        AppDrawerScreen(
            state = DrawerUiState(
                apps = sampleApps,
                pinnedAppKeys = sampleApps.take(HOME_APP_COUNT).mapTo(mutableSetOf()) { it.key },
                sectionLetters = sampleApps.associate { app ->
                    app.key to app.label.first().uppercaseChar()
                },
                alphabetIndex = sampleApps
                    .map { app -> app.label.first().uppercaseChar() }
                    .distinct()
                    .associateWith { letter ->
                        sampleApps.indexOfFirst { app ->
                            app.label.first().uppercaseChar() == letter
                        }
                    },
                isLoading = false,
            ),
            actions = drawerActions,
        )
    }
}

@PreviewTest
@Preview(
    name = "README Schedule",
    widthDp = README_WIDTH_DP,
    heightDp = README_HEIGHT_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
fun ReadmeSchedule() {
    ReadmePreview {
        ScheduleEditorScreen(
            state = ScheduleEditorUiState(
                name = "Deep work",
                days = setOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                ),
                startMinute = 9 * 60,
                endMinute = 17 * 60,
                selectedAppKeys = sampleApps
                    .take(SCHEDULE_APP_COUNT)
                    .mapTo(mutableSetOf()) { it.key },
                installedApps = sampleApps,
                isLoading = false,
            ),
            onBack = {},
            onAction = {},
        )
    }
}

@PreviewTest
@Preview(
    name = "README Customize",
    widthDp = README_WIDTH_DP,
    heightDp = README_HEIGHT_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
fun ReadmeCustomize() {
    ReadmePreview {
        CustomizationScreen(
            state = CustomizationUiState(shortcuts = sampleShortcuts),
            appearance = HomeAppearanceState(),
            appearanceActions = HomeAppearanceActions(),
            actions = CustomizationActions(
                onBack = {},
                onEditShortcut = {},
                onOpenSchedules = {},
                onShowNavigationTutorial = {},
            ),
        )
    }
}

@Composable
private fun ReadmePreview(content: @Composable () -> Unit) {
    VoidLauncherTheme {
        CompositionLocalProvider(
            LocalAppIconContent provides ::ReadmeAppIcon,
            content = content,
        )
    }
}

@Composable
private fun ReadmeAppIcon(
    app: InstalledApp,
    modifier: Modifier,
) {
    val appIndex = sampleApps.indexOfFirst { it.key == app.key }.coerceAtLeast(0)
    val background = mockIconColors[appIndex % mockIconColors.size]
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = app.label.take(1),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun sampleApp(label: String): InstalledApp {
    val id = label.lowercase()
    return InstalledApp(
        key = AppKey(
            packageName = "com.voidlauncher.readme.$id",
            activityName = "$label.Activity",
        ),
        label = label,
        sortLabel = label,
    )
}
