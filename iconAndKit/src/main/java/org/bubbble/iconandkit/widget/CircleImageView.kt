package org.bubbble.iconandkit.widget

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import org.bubbble.iconandkit.R


/**
 * @author Andrew
 * @date 2020/09/05 15:20
 * 圆角矩形图片
 */
class CircleImageView(
    context: Context, attrs: AttributeSet?,
    @AttrRes defStyleAttr: Int
) : AppCompatImageView(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context): this(context, null)

    private val paint = Paint()
    private val paintStroke = Paint().apply {
        // 抗锯齿
        isAntiAlias = true
        // 填充
        style = Paint.Style.FILL
        // 颜色
        color = ContextCompat.getColor(context, R.color.white)
    }
    private var viewMatrix = Matrix()
    private var width = 0F
    private var height = 0F
    private var radius = 0F
    private var strokeWidth = 0F

    init {
        paint.isAntiAlias = true
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.CircleImageView)
        strokeWidth = typeArray.getDimension(R.styleable.CircleImageView_strokeWidth, 0F)
        typeArray.recycle()
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        width = measuredWidth.toFloat()
        height = measuredHeight.toFloat()
        radius = (width.coerceAtMost(height) / 2)
    }

    override fun onDraw(canvas: Canvas?) {
        if (drawable is BitmapDrawable) {
            canvas?.drawCircle(width / 2F, width / 2F, width / 2F, paintStroke)
            paint.shader = initBitmapShader(drawable as BitmapDrawable) // 将着色器设置给画笔

            canvas!!.drawCircle(
                width / 2F,
                width / 2F,
                radius - strokeWidth,
                paint
            ) // 使用画笔在画布上画圆
            return
        }

        super.onDraw(canvas)
    }

    /**
     * 获取ImageView中资源图片的Bitmap，利用Bitmap初始化图片着色器,通过缩放矩阵将原资源图片缩放到铺满整个绘制区域，避免边界填充
     */
    private fun initBitmapShader(drawable: BitmapDrawable): BitmapShader? {
        val bitmap = drawable.bitmap
        val bitmapShader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val scale = (width / bitmap.width).coerceAtLeast(width / bitmap.height)
        viewMatrix.setScale(scale, scale)
        bitmapShader.setLocalMatrix(viewMatrix)
        return bitmapShader
    }

    fun setStrokeWidth(strokeWidth: Float) {
        this.strokeWidth = strokeWidth
        invalidate()
    }
}