package org.bubbble.bubbble.utils

import android.widget.ImageView
import com.bumptech.glide.Glide

/**
 * @author Andrew
 * @date 2020/09/25 15:43
 */

fun ImageView.load(res: Int) {
    Glide.with(this).load(res).into(this)
}