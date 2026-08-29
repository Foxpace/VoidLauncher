package com.tomasrepcik.voidlauncher.customization.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.appearance.HomeAppearanceActions
import com.tomasrepcik.voidlauncher.appearance.HomeAppearanceSettings
import com.tomasrepcik.voidlauncher.appearance.HomeAppearanceState
import com.tomasrepcik.voidlauncher.customization.CustomizationActions
import com.tomasrepcik.voidlauncher.customization.CustomizationUiState

@Composable
internal fun ColumnScope.CustomizationSettings(
    state: CustomizationUiState,
    appearance: HomeAppearanceState,
    appearanceActions: HomeAppearanceActions,
    actions: CustomizationActions,
    onOpenLicenses: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("customization_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsSectionTitle(stringResource(R.string.home_appearance))
                HomeAppearanceSettings(
                    state = appearance,
                    actions = appearanceActions,
                )
            }
        }
        item {
            ShortcutSettings(
                shortcuts = state.shortcuts,
                onEditShortcut = actions.onEditShortcut,
            )
        }
        item {
            ScheduleSettings(onOpenSchedules = actions.onOpenSchedules)
        }
        item {
            HelpSettings(onShowNavigationTutorial = actions.onShowNavigationTutorial)
        }
        item {
            AboutSettings(onOpenLicenses = onOpenLicenses)
        }
    }
}
