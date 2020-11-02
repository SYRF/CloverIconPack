package org.bubbble.iconandkit.ui.apply

data class LauncherItem(
    val name: String,
    val packageName: String,
    val launcherColor: Int,
    val isInstalled: Int = -1,
    val isTips: Boolean
)