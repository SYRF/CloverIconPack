package org.bubbble.iconandkit.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.View.MeasureSpec.*
import android.view.ViewConfiguration
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.OverScroller
import androidx.core.view.*
import org.bubbble.iconandkit.R
import java.util.*

open class SmoothNestedScrollLayout : LinearLayout, NestedScrollingParent2, NestedScrollingChild2 {
    private var mFirstY = 0f
    private var mParentHelper: NestedScrollingParentHelper? = null
    private var mChildHelper: NestedScrollingChildHelper? = null
    private var mTopViewId = 0
    private var mInnerOffsetId = 0
    private var mContentViewId = 0
    private val mInnerOffsetView = ArrayList<View>(1)
    private val mTopStableView = ArrayList<View>(1)
    private var mContentView: View? = null
    private var mTopScrollHeight = 0
    private var mScroller: OverScroller? = null
    private var mVelocityTracker: VelocityTracker? = null
    private var mTouchSlop = 0
    private var mMaximumVelocity = 0
    private var mMinimumVelocity = 0
    private var mLastY = 0f
    private var mDragging = false
    private var mTarget: View? = null
    private var mScrollType = ViewCompat.TYPE_TOUCH
    private var mInsetTargetScrollView: View? = null
    private var mTargetLastY = 0
    private var mScrollControlDelegate: OnScrollControlDelegate? = null
    private var mScrollListener: onScrollListener? = null
    private var mInflated = false
    private var mOuterCoverTopHeight = 0
    private var mOuterCoverBottomHeight = 0
    private var innerOffsetHeight = 0
    @Volatile var mScrollY = 0

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        orientation = VERTICAL
        mScroller = OverScroller(context)
        val config = ViewConfiguration.get(context)
        mTouchSlop = config.scaledTouchSlop
        mMaximumVelocity = config.scaledMaximumFlingVelocity
        mMinimumVelocity = config.scaledMinimumFlingVelocity
        mParentHelper = NestedScrollingParentHelper(this)
        mChildHelper = NestedScrollingChildHelper(this)
        // ...because why else would you be using this widget?
        isNestedScrollingEnabled = true
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.SmoothNestedScrollLayout)
            mTopViewId =
                a.getResourceId(R.styleable.SmoothNestedScrollLayout_nest_scroll_top_view, 0)
            mInnerOffsetId =
                a.getResourceId(R.styleable.SmoothNestedScrollLayout_nest_scroll_inner_header, 0)
            mContentViewId =
                a.getResourceId(R.styleable.SmoothNestedScrollLayout_nest_scroll_content, 0)
            a.recycle()
        }
    }

    override fun getNestedScrollAxes(): Int {
        Log.e(TAG, "getNestedScrollAxes")
        return mParentHelper!!.nestedScrollAxes
    }

    override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int): Boolean {
        return onStartNestedScroll(child, target, nestedScrollAxes, ViewCompat.TYPE_TOUCH)
    }

    override fun onNestedScrollAccepted(child: View, target: View, nestedScrollAxes: Int) {
        Log.d(TAG, "onNestedScrollAccepted")
        mParentHelper!!.onNestedScrollAccepted(child, target, nestedScrollAxes)
    }

    override fun onStopNestedScroll(target: View) {
        Log.d(TAG, "onStopNestedScroll")
        mTarget = null
        mParentHelper!!.onStopNestedScroll(target)
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int
    ) {
        Log.d(TAG, "onNestedScroll")
    }

    // NestedScrollingParent2 interfaces
    override fun onStartNestedScroll(view: View, target: View, axes: Int, type: Int): Boolean {
        Log.d(TAG, "Start Nested Scroll $type")
        mTarget = target
        mScrollType = type
        clearSelfFling()
        return true
    }

    override fun onNestedScrollAccepted(
        child: View,
        target: View,
        nestedScrollAxes: Int,
        type: Int
    ) {
        Log.d(TAG, "accept Start Nested Scroll $type")
        mTarget = target
        mScrollType = type
        clearSelfFling()
        mParentHelper!!.onNestedScrollAccepted(child, target, nestedScrollAxes, type)
    }

    override fun onStopNestedScroll(view: View, type: Int) {
        Log.d(TAG, "onStopNestedScroll $type")
        if (mScrollType == type) {
            mTarget = null
        }
        mParentHelper!!.onStopNestedScroll(view, type)
    }

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int, type: Int
    ) {
        if (type == ViewCompat.TYPE_TOUCH) {
            Log.d(TAG, "onNestedScroll ignore touch $type")
            return
        }
        if (dyUnconsumed == 0) {
            Log.d(TAG, "onNestedScroll ignore dy = 0")
            return
        }
        val consumedY = consumeY(target, dyUnconsumed)
        if (consumedY != 0) {
            Log.d(TAG, "onNestedScroll type $type consumed $consumedY")
            scrollBy(0, consumedY)
        } else {
            // 这步很必要，这里表示子view和父view都不消耗这个fling了，必须让子view停掉冗余的fling。否者这一次
            // fling会自然慢慢结束，这个过程又没有真实的滚动。这期间如果再次触发一次别的scroll，就会导致同时有
            // 两个scroll，引起滑动冲突。同理在这期间，我们在on touch里也做了这样的检测判断，确保不会发生冲突。
            // 理论上，在onTouchEvent里做同样的处理已经足够避免冲突，但这里仍保留以结束掉fling避免浪费CPU。
            Log.d(TAG, "onNestedScroll stop it type $type")
            ViewCompat.stopNestedScroll(target, type)
        }
    }

    // 第一代方法
    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        onNestedPreScroll(target, dx, dy, consumed, ViewCompat.TYPE_TOUCH)
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        val consumedY = consumeY(target, dy)
        if (consumedY != 0) {
            Log.d(TAG, "onNestedPreScroll type $type consumed $consumedY")
            scrollBy(0, consumedY)
            consumed[1] = consumedY
            return
        }
        Log.d(TAG, "onNestedPreScroll unconsumed type $type")
    }

    private fun consumeY(target: View, dy: Int): Int {
        // dy 代表滑动的距离，是正是负代表滑动方向。
        // 读代码 RecyclerView和ScrollView发现，dy = mLastY - currentY，是用前一次减这一次活动的deltaY
        // 也就是手指从上往下滑，currentY 肯定大于 mLastY，所以 dy < 0 是从上往下滑动。
        if (dy > 0) {
            // 手指从下往上滑动，如果top还没有完全隐藏，则必须消耗dy以隐藏top，执行 scroll hide top动作
            // getScrollY()会返回控件坐标向上偏移量，正数表示向上偏移，负数表示向下偏移
            val sY = scrollY
            if (sY < topScrollHeight) {
                return Math.min(topScrollHeight - sY, dy)
            }
        } else if (dy < 0) {
            // 从上往下滑，scroll Y >=0 标识 Y 向下已有偏移，计算list是否到顶
            val sY = scrollY
            if (sY > 0 && !target.canScrollVertically(-1)) {
                return Math.max(dy, -sY) //这里的值都是负数，取绝对值小的，所以要用max
            }
        }
        return 0
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        return super.onNestedPreFling(target, velocityX, velocityY)
    }

    fun setContentView(view: View?): SmoothNestedScrollLayout {
        mContentView = view
        return this
    }

    fun setContentViewId(id: Int): SmoothNestedScrollLayout {
        mContentViewId = id
        if (id > 0 && mInflated) {
            mContentView = findViewById(id)
        }
        return this
    }

    fun setTopView(topView: View?): SmoothNestedScrollLayout {
        if (topView != null) {
            mTopStableView.add(topView)
        } else {
            mTopStableView.clear()
        }
        return this
    }

    fun setTopViewId(topId: Int): SmoothNestedScrollLayout {
        mTopViewId = topId
        if (topId > 0 && mInflated) {
            val v = findViewById<View>(topId)
            if (v != null) {
                mTopStableView.add(v)
            }
        }
        return this
    }

    fun setInnerOffsetView(view: View): SmoothNestedScrollLayout {
        if (!mInnerOffsetView.contains(view)) {
            mInnerOffsetView.add(view)
        }
        return this
    }

    private fun calculateInnerOffsetHeight(): Int {
        var h = 0
        for (v in mInnerOffsetView) {
            h += v.measuredHeight
        }
        innerOffsetHeight = h
        return h
    }

    fun setOuterCoverTopMargin(height: Int): SmoothNestedScrollLayout {
        if (mOuterCoverTopHeight != height) {
            mOuterCoverTopHeight = height
            requestLayout()
        }
        return this
    }

    fun setOuterCoverBottomMargin(height: Int): SmoothNestedScrollLayout {
        if (mOuterCoverBottomHeight != height) {
            mOuterCoverBottomHeight = height
            requestLayout()
        }
        return this
    }

    private fun initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
    }

    private fun recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker!!.recycle()
            mVelocityTracker = null
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        when (action) {
            MotionEvent.ACTION_MOVE -> {
                if (mDragging) {
                    return true
                }
                val y = ev.y
                if (y > mTopScrollHeight + innerOffsetHeight - scrollY) {
                    return false
                }
                val dy = Math.abs(mLastY - ev.y)
                if (dy > mTouchSlop) {
                    // 只有手指滑动距离大于阈值时，才会开始拦截
                    // Start scrolling!
                    parent.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_DOWN -> {
                clearAllFling()
                mLastY = ev.y
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> mDragging = false
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        initVelocityTrackerIfNotExists()
        mVelocityTracker!!.addMovement(event)
        val action = event.action
        val y = event.y
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                clearAllFling()
                mLastY = y
                mFirstY = y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = y - mLastY
                if (!mDragging && Math.abs(y - mFirstY) > mTouchSlop) {
                    mDragging = true
                }
                if (mDragging) {
                    scrollBy(0, (-dy).toInt())
                }
                mLastY = y
                Log.d(TAG, "dispatchNestedPreScroll")

            }
            MotionEvent.ACTION_CANCEL -> {
                mDragging = false
                recycleVelocityTracker()
                if (!mScroller!!.isFinished) {
                    mScroller!!.abortAnimation()
                }
            }
            MotionEvent.ACTION_UP -> {
                mDragging = false
                mVelocityTracker!!.computeCurrentVelocity(1000, mMaximumVelocity.toFloat())
                val velocityY = mVelocityTracker!!.yVelocity.toInt()
                if (Math.abs(velocityY) > mMinimumVelocity) {
                    fling(-velocityY)
                }
                recycleVelocityTracker()
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Log.d(TAG, "onMeasure height:" + getSize(heightMeasureSpec))
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (mTopStableView.size > 0) {
            mTopScrollHeight = 0
            for (stableView in mTopStableView) {

                val lp = stableView.layoutParams
                val selfWidthSpecMode = getMode(widthMeasureSpec)
                val selfWidthSpecSize = getSize(widthMeasureSpec)
                var childWidthSpec = 0
                when (lp.width) {
                    MATCH_PARENT -> {
                        childWidthSpec =
                            if (selfWidthSpecMode == EXACTLY || selfWidthSpecMode == AT_MOST) {
                                makeMeasureSpec(selfWidthSpecSize, EXACTLY)
                            } else {
                                makeMeasureSpec(0, UNSPECIFIED)
                            }
                    }

                    WRAP_CONTENT -> {
                        childWidthSpec =
                            if (selfWidthSpecMode == EXACTLY || selfWidthSpecMode == AT_MOST) {
                                makeMeasureSpec(selfWidthSpecSize, AT_MOST)
                            } else {
                                makeMeasureSpec(0, UNSPECIFIED)
                            }
                    }

                    else -> {
                        childWidthSpec = makeMeasureSpec(lp.width, EXACTLY)
                    }
                }

                stableView.measure(childWidthSpec, makeMeasureSpec(lp.height, UNSPECIFIED))
                mTopScrollHeight += stableView.measuredHeight
            }

        }
        if (mContentView != null) {
            val innerOffsetHeight = calculateInnerOffsetHeight()
            var contentMH = mContentView!!.measuredHeight
            Log.d(
                TAG, "onMeasure content height:" + contentMH +
                        " top:" + mTopScrollHeight +
                        " total:" + measuredHeight + " inner:" + innerOffsetHeight +
                        " outer:" + mOuterCoverTopHeight
            )
            // 修改params.height的方式
//                ViewGroup.LayoutParams params = mContentView.getLayoutParams();
//                params.height = getMeasuredHeight() - innerOffsetHeight;
            //设置params后，需重新调用super.onMeasure();
            var contentH = measuredHeight - innerOffsetHeight - mOuterCoverTopHeight
            if (contentH > mOuterCoverBottomHeight) {
                contentH -= mOuterCoverBottomHeight
            }
            mContentView!!.measure(
                widthMeasureSpec,
                makeMeasureSpec(contentH, MeasureSpec.AT_MOST)
            )
            contentMH = mContentView!!.measuredHeight
            Log.d(TAG, "onMeasure content adjusted height:$contentMH")
        }
        setMeasuredDimension(
            measuredWidth,
            mTopScrollHeight + measuredHeight
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        mInflated = true
        if (mTopViewId > 0) {
            setTopViewId(mTopViewId)
        }
        if (mInnerOffsetId > 0) {
            mInnerOffsetView.add(findViewById(mInnerOffsetId))
        }
        if (mContentViewId > 0) {
            mContentView = findViewById(mContentViewId)
        }
    }

    val topScrollHeight: Int
        get() = mTopScrollHeight - mOuterCoverTopHeight

    fun fling(velocityY: Int) {
        mTargetLastY = 0
        // 第三步-二小步，fling调整最大Y偏移，以便把fling传递给nestedScrollingChild。
        mScroller!!.fling(0, scrollY, 0, velocityY, 0, 0, 0, Int.MAX_VALUE)
        invalidate()
    }

    fun setScrollChild(view: View?) {
        mInsetTargetScrollView = view
    }

    private fun findChildScrollView(dy: Int): View? {
        var delegateView = mContentView
        if (delegateView != null && delegateView.canScrollVertically(dy)) {
            return delegateView
        }
        delegateView = mInsetTargetScrollView
        if (delegateView != null
            && delegateView.canScrollVertically(dy)
        ) {
            return delegateView
        }
        if (mScrollControlDelegate != null) {
            delegateView = mScrollControlDelegate!!.scrollChildView
            if (delegateView != null && delegateView.canScrollVertically(dy)) {
                return delegateView
            }
        }
        return null
    }

    private fun clearSelfFling() {
        if (!mScroller!!.isFinished) {
            mScroller!!.abortAnimation()
        }
        mTargetLastY = 0
    }

    private fun clearAllFling() {
        if (!mScroller!!.isFinished) {
            mScroller!!.abortAnimation()
        }
        mTargetLastY = 0
        if (mTarget != null) {
            Log.d(TAG, "force stop nest scroll from child")
            ViewCompat.stopNestedScroll(mTarget!!, mScrollType)
        }
    }

    override fun scrollTo(x: Int, y: Int) {
        var y = y
        val topScrollHeight = topScrollHeight
        if (y > topScrollHeight) {
            super.scrollTo(x, topScrollHeight)
            y -= topScrollHeight
            val dy = y - mTargetLastY
            val scrollView = findChildScrollView(dy)
            if (scrollView != null) {
                scrollView.scrollBy(x, dy)
                mTargetLastY += dy
                Log.d(TAG, "scrollTo transfer $y")
            } else {
                Log.d(TAG, "scrollTo but container view can not scroll")
                clearSelfFling()
            }
            return
        }
        if (y < 0) {
            y = 0
        }
        if (y != scrollY) {
            super.scrollTo(x, y)
        }
    }

    override fun computeScroll() {
        if (mScroller!!.computeScrollOffset()) {

            Log.d(TAG, "computeScroll to " + mScroller!!.currY)
            scrollTo(0, mScroller!!.currY)
            invalidate()
            return
        }
        Log.d(TAG, "computeScroll end")
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH)
        super.onScrollChanged(l, t, oldl, oldt)
        mScrollY = t
        dispatchNestedPreScroll(0, t - oldt, null, null, ViewCompat.TYPE_TOUCH)
        Log.d(TAG, "onScrollChanged dy: ${t - oldt} scrollY: $t")
        if (mScrollListener != null) {
            mScrollListener!!.onNestScrolling(t - oldt, t)
        }
    }

    fun setControlDelegate(c: OnScrollControlDelegate?): SmoothNestedScrollLayout {
        mScrollControlDelegate = c
        return this
    }

    fun setScrollListener(l: onScrollListener?): SmoothNestedScrollLayout {
        mScrollListener = l
        return this
    }

    interface OnScrollControlDelegate {
        val scrollChildView: View?
    }

    interface onScrollListener {
        fun onNestScrolling(dy: Int, scrollY: Int)
    }

    companion object {
        private const val TAG = "ExNestScroll"
    }

    // NestedScrollingChild2
    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        return mChildHelper!!.startNestedScroll(axes, type)
    }

    override fun stopNestedScroll(type: Int) {
        return mChildHelper!!.stopNestedScroll(type)
    }

    override fun hasNestedScrollingParent(type: Int): Boolean {
        return mChildHelper!!.hasNestedScrollingParent(type)
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return mChildHelper!!.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            offsetInWindow,
            type
        )
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return mChildHelper!!.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
    }


    // NestedScrollingChild
    override fun setNestedScrollingEnabled(enabled: Boolean) {
        mChildHelper!!.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return mChildHelper!!.isNestedScrollingEnabled
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return startNestedScroll(axes, ViewCompat.TYPE_TOUCH)
    }

    override fun stopNestedScroll() {
        stopNestedScroll(ViewCompat.TYPE_TOUCH)
    }

    override fun hasNestedScrollingParent(): Boolean {
        return hasNestedScrollingParent(ViewCompat.TYPE_TOUCH)
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
        dyUnconsumed: Int, offsetInWindow: IntArray?
    ): Boolean {
        return mChildHelper!!.dispatchNestedScroll(
            dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
            offsetInWindow
        )
    }
    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ): Boolean {
        return dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, ViewCompat.TYPE_TOUCH)
    }

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return mChildHelper!!.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return mChildHelper!!.dispatchNestedPreFling(velocityX, velocityY)
    }
}