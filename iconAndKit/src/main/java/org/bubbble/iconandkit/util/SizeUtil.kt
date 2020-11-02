package org.bubbble.life.util

import android.content.res.Resources
import android.util.TypedValue

/**
 * @author Andrew
 * @date 2020/08/06 10:15
 * 尺寸转换工具
 */

val Float.dp
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        Resources.getSystem().displayMetrics)


val Int.dp
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics).toInt()

val Int.half
    get() = this / 2

val Float.half
    get() = this / 2