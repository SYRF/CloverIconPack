package org.bubbble.iconandkit.ui.request

import com.lollipop.iconcore.ui.IconHelper

data class RequestItem(
    val info: IconHelper.AppInfo?,
    val adapted: Int?,
    val notAdapt: Int?,
    val isHeader: Boolean)