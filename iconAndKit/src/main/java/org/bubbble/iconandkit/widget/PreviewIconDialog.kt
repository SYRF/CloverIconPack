package org.bubbble.iconandkit.widget

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.lollipop.iconcore.ui.IconImageView
import org.bubbble.iconandkit.R
import org.bubbble.iconandkit.base.BackPressedListener
import org.bubbble.iconandkit.ui.adapt.IconsItem
import org.bubbble.iconandkit.util.ColorUtil
import java.util.*

/**
 * @author Andrew
 * @date 2020/10/31 12:39
 */
class PreviewIconDialog :
    BackPressedListener,
    ValueAnimator.AnimatorUpdateListener,
    Animator.AnimatorListener {

    private var dialogView: View? = null

    private var targetView: View? = null

    private val locationOffset = IntArray(2)

    private val sizeOffset = FloatArray(2)

    private val maskView: View?
        get() {
            return dialogView?.findViewById(R.id.mask_view)
        }

    private val backgroundView: View?
    get() {
        return dialogView?.findViewById(R.id.phone_background)
    }

    private val previewView: IconImageView?
    get() {
        return dialogView?.findViewById(R.id.preview_icon)
    }

    private val previewName: TextView?
    get() {
        return dialogView?.findViewById(R.id.preview_name)
    }

    private var progress = 0F

    private var maskColor = Color.parseColor("#8A000000")

    private val animator = ValueAnimator().apply {
        addUpdateListener(this@PreviewIconDialog)
        addListener(this@PreviewIconDialog)
    }

    fun attach(context: Activity) {
        remove()
        val rootGroup = findGroup(context.window.decorView)
        val view = LayoutInflater.from(context).inflate(
            R.layout.dialog_preview_icon, rootGroup, false)
        dialogView = view
        view.setOnClickListener {
            dismiss()
        }
        view.visibility = View.INVISIBLE
        rootGroup?.addView(view)
    }

    private fun findGroup(rootGroup: View?): ViewGroup? {
        rootGroup ?: return null
        if (rootGroup is CoordinatorLayout) {
            return rootGroup
        }
        if (rootGroup is FrameLayout) {
            return rootGroup
        }
        if (rootGroup is ViewGroup) {
            val views = LinkedList<View>()
            views.add(rootGroup)
            // 按层次遍历
            while (views.isNotEmpty()) {
                val view = views.removeFirst()
                if (view is CoordinatorLayout) {
                    return view
                }
                if (view is FrameLayout) {
                    return view
                }
                if (view is ViewGroup) {
                    for (i in 0 until view.childCount) {
                        views.addLast(view.getChildAt(i))
                    }
                }
            }
        }
        return null
    }

    fun onDestroy() {
        remove()
    }

    private fun remove() {
        animator.cancel()
        targetView?.visibility = View.VISIBLE
        targetView = null
        dialogView?.let { dialog ->
            dialog.parent?.let {
                if (it is ViewManager) {
                    it.removeView(dialog)
                }
            }
        }
        dialogView = null
    }

    fun show(view: View, icon: IconsItem) {
        val preview = previewView?:return
        val previewName = previewName?:return

        setMaskColor(preview.context, icon.icon)
        targetView?.let {
            it.visibility = View.VISIBLE
        }

        targetView = view

        progress = 0F
        preview.apply {
            scaleX = 1F
            scaleY = 1F
            translationX = 0F
            translationY = 0F
        }
        preview.loadIcon(icon.icon)
        previewName.text = icon.name
        initInfo(view, preview)
        doAnimation(true)
    }

    private fun initInfo(view: View, preview: View) {
        sizeOffset[0] = view.width * 1F / preview.width
        sizeOffset[1] = view.height * 1F / preview.height

        locationOffset[0] = 0
        locationOffset[1] = 0
        getLocationOffset(preview, view, locationOffset)
    }

    private fun getLocationOffset(self: View, target: View, intArray: IntArray) {
        val selfLoc = IntArray(2)
        self.getLocationInWindow(selfLoc)
        selfLoc[0] -= self.translationX.toInt()
        selfLoc[1] -= self.translationY.toInt()
        selfLoc[0] += self.width / 2
        selfLoc[1] += self.height / 2
        val targetLoc = IntArray(2)
        target.getLocationInWindow(targetLoc)
        targetLoc[0] -= target.translationX.toInt()
        targetLoc[1] -= target.translationY.toInt()
        targetLoc[0] += target.width / 2
        targetLoc[1] += target.height / 2
        intArray[0] = targetLoc[0] - selfLoc[0]
        intArray[1] = targetLoc[1] - selfLoc[1]
    }

    private fun setMaskColor(context: Context, icon: Int) {
        val isDark = ContextCompat.getColor(context, R.color.mask_background) == Color.parseColor("#8A000000")
        if (isDark) {
            maskColor = ContextCompat.getColor(context, R.color.mask_background)
            setBackgroundColor(Color.TRANSPARENT, maskColor)
        } else {
            val builder: Palette.Builder = Palette.from(drawableToBitmap(context, icon))
            builder.generate { palette -> //获取到充满活力的这种色调
                maskColor = if (palette != null && palette.vibrantSwatch != null) {
                    ColorUtils.setAlphaComponent(palette.vibrantSwatch!!.rgb, 204)
                } else if (palette != null && palette.lightVibrantSwatch != null) {
                    ColorUtils.setAlphaComponent(palette.lightVibrantSwatch!!.rgb, 204)
                }else if (palette != null && palette.mutedSwatch != null) {
                    ColorUtils.setAlphaComponent(palette.mutedSwatch!!.rgb, 204)
                }else {
                    Toast.makeText(context, "获取不到颜色", Toast.LENGTH_SHORT).show()
                    ContextCompat.getColor(context, R.color.mask_background)
                }
                setBackgroundColor(Color.TRANSPARENT, maskColor)
            }
        }
    }

    private fun setBackgroundColor(startColor: Int, endColor: Int) {
        val mask = maskView?:return
        val colorChange = ValueAnimator.ofFloat(0F, 1F)
        colorChange.duration = 300L
        colorChange.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
                mask.setBackgroundColor(startColor)
                mask.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animator?) {
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationRepeat(animation: Animator?) {
            }

        })
        colorChange.addUpdateListener {
            val color = ColorUtil.getColor(startColor, endColor, it.animatedValue as Float)
            maskColor = color
            mask.setBackgroundColor(color)
        }
        colorChange.start()
    }

    private fun drawableToBitmap(context: Context, id: Int): Bitmap {
        return BitmapFactory.decodeResource(context.resources, id)
    }

    private fun dismiss() {
        doAnimation(false)
        setBackgroundColor(maskColor, Color.TRANSPARENT)
        closePanel()
    }

    private fun doAnimation(isShow: Boolean) {
        val endValue = if (isShow) { 1F } else { 0F }
        animator.cancel()
        animator.setFloatValues(progress, endValue)
        animator.start()
    }

    override fun onBackPressed(): Boolean {
        if (dialogView?.visibility == View.VISIBLE) {
            dismiss()
            return true
        }
        return false
    }

    private fun onUpdate() {
        val preview = previewView?:return
        val background = backgroundView?:return
        val previewName = previewName?:return
        preview.apply {
            val x = (1 - sizeOffset[0]) * progress + sizeOffset[0]
            val y = (1 - sizeOffset[1]) * progress + sizeOffset[1]
            scaleX = x
            scaleY = y
            translationX = locationOffset[0] * (1 - progress)
            translationY = locationOffset[1] * (1 - progress)
        }
        background.alpha = progress
        background.translationY = background.height - (background.height * progress)

        previewName.translationY = previewName.height - (previewName.height * progress)
        previewName.alpha = progress
    }

    private fun closePanel() {
    }

    private fun openPanel() {
    }

    override fun onAnimationUpdate(animation: ValueAnimator?) {
        if (animation == animator) {
            progress = animation.animatedValue as Float
            onUpdate()
        }
    }

    override fun onAnimationStart(animation: Animator?) {
        if (animation == animator && dialogView?.visibility != View.VISIBLE) {
            dialogView?.visibility = View.VISIBLE
            previewName?.visibility = View.VISIBLE
            backgroundView?.visibility = View.VISIBLE
        }
        targetView?.let {
            it.visibility = View.INVISIBLE
        }
    }

    override fun onAnimationEnd(animation: Animator?) {
        if (animation == animator) {
            if (progress < 0.1F) {
                dialogView?.visibility = View.INVISIBLE
                targetView?.visibility = View.VISIBLE
            }
            if (progress > 0.9F) {
                openPanel()
            }
        }
    }

    override fun onAnimationCancel(animation: Animator?) {
    }

    override fun onAnimationRepeat(animation: Animator?) {
    }
}