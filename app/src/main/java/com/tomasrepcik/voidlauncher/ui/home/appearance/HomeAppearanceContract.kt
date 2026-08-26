package com.tomasrepcik.voidlauncher.ui.home.appearance

data class HomeAppearanceState(
    val backgroundUri: String? = null,
    val useBackgroundColors: Boolean = false,
    val background: HomeBackgroundImage? = null,
    val isLoadingBackground: Boolean = false,
)

sealed interface HomeAppearanceAction {
    data class SelectBackground(val uri: String) : HomeAppearanceAction
    data object RestoreDefaultBackground : HomeAppearanceAction
    data class SetUseBackgroundColors(val enabled: Boolean) : HomeAppearanceAction
}

data class HomeAppearanceActions(
    val onChooseBackground: () -> Unit = {},
    val onRestoreDefault: () -> Unit = {},
    val onUseBackgroundColorsChange: (Boolean) -> Unit = {},
)
