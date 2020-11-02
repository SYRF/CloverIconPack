package org.bubbble.iconandkit.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.RecyclerView
import org.bubbble.iconandkit.R
import org.bubbble.life.util.dp


class MaxHeightRecyclerView(context: Context, attrs: AttributeSet?,
                            @StyleRes defStyleRes:Int): RecyclerView(context,attrs,defStyleRes) {

    constructor(context: Context, attrs: AttributeSet?):this(context,attrs,0){
        initialize(context, attrs!!)
    }

    constructor(context: Context):this(context,null)

    private var maxHeight = 0

    private fun initialize(context: Context, attrs: AttributeSet){
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.MaxHeightRecyclerView)
        maxHeight = typeArray.getLayoutDimension(R.styleable.MaxHeightRecyclerView_maxHeight, maxHeight)
        typeArray.recycle()
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        if (childCount > 0) {
            val height: Int
            val child: View = getChildAt(0)
            val params =
                child.layoutParams as LayoutParams
            child.measure(
                widthSpec,
                MeasureSpec.makeMeasureSpec(
                    0,
                    MeasureSpec.UNSPECIFIED
                )
            )
            // item个数
            val itemCount = adapter!!.itemCount
            // item高度
            val item: Int =
                child.measuredHeight + paddingTop + paddingBottom + params.topMargin + params.bottomMargin
            // 把item的高度转成px
            val max: Int = itemCount * item.dp
            height = max.coerceAtMost(maxHeight)
            setMeasuredDimension(widthSpec, height)
        } else {
            super.onMeasure(widthSpec, heightSpec)
        }


    }
}