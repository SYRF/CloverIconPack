/*
 * Copyright (C) 2016 Jared Rummler <jared.rummler@gmail.com>
 * Copyright (C) 2016 Tim Malseed
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.bubbble.iconandkit.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.bubbble.iconandkit.R;
import org.bubbble.iconandkit.util.Utils;


/**
 * A base {@link RecyclerView}, which does the following:
 *
 * <ul>
 * <li> NOT intercept a touch unless the scrolling velocity is below a predefined threshold.
 * <li> Enable fast scroller.
 * </ul>
 */
public class FastScrollRecyclerView extends RecyclerView implements RecyclerView.OnItemTouchListener {

  private static final int SCROLL_DELTA_THRESHOLD_DP = 4;
  private static final int DEFAULT_HIDE_DELAY = 1000;

  // 滚动位置状态
  private final ScrollPositionState scrollPositionState = new ScrollPositionState();
  // 背景内边距
  private final Rect backgroundPadding = new Rect();
  // 快速滚动条
  FastScrollBar fastScrollBar;
  // 快速滚动始终启用
  boolean fastScrollAlwaysEnabled;
  // 增量阈值
  private float deltaThreshold;
  // 隐藏延迟
  private int hideDelay;
  // 保持最后已知的滚动增量/速度沿y轴。
  int lastDy;
  private int downX;
  private int downY;
  private int lastY;

  final Runnable hide = new Runnable() {

    @Override public void run() {
      if (!fastScrollBar.isDraggingThumb()) {
        fastScrollBar.animateScrollbar(false);
      }
    }
  };

  public FastScrollRecyclerView(Context context) {
    this(context, null);
  }

  public FastScrollRecyclerView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public FastScrollRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.FastScrollRecyclerView);
    fastScrollAlwaysEnabled = ta.getBoolean(R.styleable.FastScrollRecyclerView_fastScrollAlwaysEnabled, false);
    hideDelay = ta.getInt(R.styleable.FastScrollRecyclerView_fastScrollHideDelay, DEFAULT_HIDE_DELAY);
    ta.recycle();
    deltaThreshold = getResources().getDisplayMetrics().density * SCROLL_DELTA_THRESHOLD_DP;
    fastScrollBar = new FastScrollBar(this, attrs);
    fastScrollBar.setDetachThumbOnFastScroll();
    addOnScrollListener(new OnScrollListener() {

      @Override public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        if (fastScrollAlwaysEnabled) return;
        switch (newState) {
          case SCROLL_STATE_DRAGGING:
            removeCallbacks(hide);
            fastScrollBar.animateScrollbar(true);
            break;
          case SCROLL_STATE_IDLE:
            hideScrollBar();
            break;
        }
      }

      @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        // 更新偏移量
        lastDy = dy;
        // 更新滚动条的偏移量
        onUpdateScrollbar(dy);
      }
    });
  }

  public void reset() {
    fastScrollBar.reattachThumbToScroll();
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    addOnItemTouchListener(this);
  }

  /**
   * We intercept the touch handling only to support fast scrolling when initiated from the
   * scroll bar.  Otherwise, we fall back to the default RecyclerView touch handling.
   * 我们拦截触摸处理仅支持从滚动条启动时的快速滚动。 否则，我们将回退到默认的RecyclerView触摸处理。
   */
  @Override public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent ev) {
    return handleTouchEvent(ev);
  }

  @Override public void onTouchEvent(RecyclerView rv, MotionEvent ev) {
    handleTouchEvent(ev);
  }

  /**
   * Handles the touch event and determines whether to show the fast scroller (or updates it if
   * it is already showing).
   * 处理触摸事件并确定是否显示快速滚动条（或者更新它是否它已经显示出来了）。
   */
  private boolean handleTouchEvent(MotionEvent ev) {
    int action = ev.getAction();
    int x = (int) ev.getX();
    int y = (int) ev.getY();
    switch (action) {
      case MotionEvent.ACTION_DOWN:
        // Keep track of the down positions
        // 跟踪向下位置
        downX = x;
        downY = lastY = y;
        if (shouldStopScroll(ev)) {
          stopScroll();
        }
        fastScrollBar.handleTouchEvent(ev, downX, downY, lastY);
        break;
      case MotionEvent.ACTION_MOVE:
        lastY = y;
        fastScrollBar.handleTouchEvent(ev, downX, downY, lastY);
        break;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        onFastScrollCompleted();
        fastScrollBar.handleTouchEvent(ev, downX, downY, lastY);
        break;
    }
    return fastScrollBar.isDraggingThumb();
  }

  @Override public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    // DO NOT REMOVE, NEEDED IMPLEMENTATION FOR M BUILDS
    // 不要删除，需要为M BUILDS实施
  }

  /**
   * Returns whether this {@link MotionEvent} should trigger the scroll to be stopped.
   * 返回此{@link MotionEvent}是否应触发滚动停止。
   */
  protected boolean shouldStopScroll(MotionEvent ev) {
    if (ev.getAction() == MotionEvent.ACTION_DOWN) {
      if ((Math.abs(lastDy) < deltaThreshold && getScrollState() != RecyclerView.SCROLL_STATE_IDLE)) {
        // 现在触摸事件被传递到{@link WidgetCell}，直到触摸顺序越过触摸斜面。
        return true;
      }
    }
    return false;
  }

  public void updateBackgroundPadding(Rect padding) {
    backgroundPadding.set(padding);
  }

  public Rect getBackgroundPadding() {
    return backgroundPadding;
  }

  /**
   * Returns the scroll bar width when the user is scrolling.
   * 用户滚动时返回滚动条宽度。
   */
  public int getMaxScrollbarWidth() {
    return fastScrollBar.getThumbMaxWidth();
  }

  /**
   * Returns the available scroll height:
   * 返回可用的滚动高度：
   * AvailableScrollHeight = Total height of the all items - last page height
   * AvailableScrollHeight =所有项目的总高度 - 最后一页高度
   *
   * This assumes that all rows are the same height.
   * 这假设所有行都是相同的高度。
   */
  protected int getAvailableScrollHeight(int rowCount, int rowHeight) {
    Log.e("FastScroll", "rowCount：" + rowCount + "       rowHeight:" + rowHeight);
    // 最后一页的高度
    int visibleHeight = getHeight() - backgroundPadding.top - backgroundPadding.bottom;
    Log.e("FastScroll", "最后一页的高度：" + visibleHeight + "    getHeight: " + getHeight());
    // 所有item的高度
    int scrollHeight = getPaddingTop() + rowCount * rowHeight + getPaddingBottom();
    Log.e("FastScroll", "所有item的高度：" + scrollHeight + "  getPadding" + getPaddingTop());
    return scrollHeight - visibleHeight;
  }

  /**
   * Returns the available scroll bar height:
   * 返回可用的滚动条高度：
   * AvailableScrollBarHeight = Total height of the visible view - thumb height
   * AvailableScrollBarHeight =可见视图的总高度 - 拇指高度
   */
  protected int getAvailableScrollBarHeight() {
    int visibleHeight = getHeight() - backgroundPadding.top - backgroundPadding.bottom;
    return visibleHeight - fastScrollBar.getThumbHeight();
  }

  public boolean isFastScrollAlwaysEnabled() {
    return fastScrollAlwaysEnabled;
  }

  protected void hideScrollBar() {
    if (!fastScrollAlwaysEnabled) {
      removeCallbacks(hide);
      postDelayed(hide, hideDelay);
    }
  }

  public void setThumbActiveColor(@ColorInt int color) {
    fastScrollBar.setThumbActiveColor(color);
  }

  public void setTrackInactiveColor(@ColorInt int color) {
    fastScrollBar.setThumbInactiveColor(color);
  }

  public void setPopupBackgroundColor(@ColorInt int color) {
    fastScrollBar.setPopupBackgroundColor(color);
  }

  public void setPopupTextColor(@ColorInt int color) {
    fastScrollBar.setPopupTextColor(color);
  }

  public FastScrollBar getFastScrollBar() {
    return fastScrollBar;
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);

    // Draw the ScrollBar AFTER the ItemDecorations are drawn over
    onUpdateScrollbar(0);
    fastScrollBar.draw(canvas);
  }

  /**
   * Updates the scrollbar thumb offset to match the visible scroll of the recycler view.  It does
   * this by mapping the available scroll area of the recycler view to the available space for the
   * scroll bar.
   * 更新滚动条拇指偏移以匹配回收器视图的可见滚动。 确实如此
   * 这可以通过将回收器视图的可用滚动区域映射到可用空间来实现
   * 滚动条。
   * @param scrollPosState
   *     the current scroll position
   *     当前的滚动位置
   * @param rowCount
   *     the number of rows, used to calculate the total scroll height (assumes that
   *     all rows are the same height)
   *     行数，用于计算总滚动高度（假设所有行的高度相同）
   */
  protected void synchronizeScrollBarThumbOffsetToViewScroll(ScrollPositionState scrollPosState, int rowCount) {
    // Only show the scrollbar if there is height to be scrolled
    // 如果要滚动高度，则仅显示滚动条
    int availableScrollBarHeight = getAvailableScrollBarHeight();
    int availableScrollHeight = getAvailableScrollHeight(rowCount, scrollPosState.rowHeight);
    if (availableScrollHeight <= 0) {
//      fastScrollBar.setThumbOffset(-1, -1);
//      return;

      Log.e("FastScroll", "高度为0");
    }

    // Calculate the current scroll position, the scrollY of the recycler view accounts for the
    // 计算当前滚动位置，回收者视图帐户的scrollY
    // view padding, while the scrollBarY is drawn right up to the background padding (ignoring
    // padding)

    //查看填充，同时scrollBarY被绘制到背景填充（忽略边距)
    int scrollY = getPaddingTop() +
        Math.round(((scrollPosState.rowIndex - scrollPosState.rowTopOffset) * scrollPosState.rowHeight));
    int scrollBarY =
        backgroundPadding.top + (int) (((float) scrollY / availableScrollHeight) * availableScrollBarHeight);

    // Calculate the position and size of the scroll bar
    // 计算滚动条的位置和大小
    int scrollBarX;
    if (Utils.INSTANCE.isRtl(getResources())) {
      scrollBarX = backgroundPadding.left;
    } else {
      scrollBarX = getWidth() - backgroundPadding.right - fastScrollBar.getThumbWidth();
    }
    fastScrollBar.setThumbOffset(scrollBarX, scrollBarY);
  }

  /**
   * <p>Maps the touch (from 0..1) to the adapter position that should be visible.</p>
   * 将触摸（从0..1）映射到应该可见的适配器位置
   *
   * <p>Override in each subclass of this base class.</p>
   * 覆盖此基类的每个子类。
   */
  public String scrollToPositionAtProgress(float touchFraction) {
    int itemCount = getAdapter().getItemCount();
    if (itemCount == 0) {
      return "";
    }
    int spanCount = 1;
    int rowCount = itemCount;
    if (getLayoutManager() instanceof GridLayoutManager) {
      spanCount = ((GridLayoutManager) getLayoutManager()).getSpanCount();
      rowCount = (int) Math.ceil((double) rowCount / spanCount);
    }

    // Stop the scroller if it is scrolling
    // 如果滚动滚动，则停止滚动条
    stopScroll();

    // 获取RV item信息
    getCurScrollState(scrollPositionState);

    float itemPos = itemCount * touchFraction;

    int availableScrollHeight = getAvailableScrollHeight(rowCount, scrollPositionState.rowHeight);

    //The exact position of our desired item
    // 我们所需物品的确切位置
    int exactItemPos = (int) (availableScrollHeight * touchFraction);

    //Scroll to the desired item. The offset used here is kind of hard to explain.
    //滚动到所需的项目。 这里使用的偏移量很难解释。
    //If the position we wish to scroll to is, say, position 10.5, we scroll to position 10,
    //如果我们希望滚动到的位置是位置10.5，我们滚动到位置10，
    //and then offset by 0.5 * rowHeight. This is how we achieve smooth scrolling.
    //然后偏移0.5 * rowHeight。 这就是我们实现平滑滚动的方式。
    LinearLayoutManager layoutManager = ((LinearLayoutManager) getLayoutManager());
    layoutManager.scrollToPositionWithOffset(spanCount * exactItemPos / scrollPositionState.rowHeight,
        -(exactItemPos % scrollPositionState.rowHeight));

    Log.e("FastScroll", "正在滚动，LinearLayoutManager   行数："+ rowCount + "  列数：" + spanCount);
    Log.e("FastScroll", "正在滚动，LinearLayoutManager   高度："+ availableScrollHeight + "  位置：" + exactItemPos);

    if (!(getAdapter() instanceof SectionedAdapter)) {
      return "";
    }

    int posInt = (int) ((touchFraction == 1) ? itemPos - 1 : itemPos);

    SectionedAdapter sectionedAdapter = (SectionedAdapter) getAdapter();
    return sectionedAdapter.getSectionName(posInt);
  }

  /**
   * <p>Updates the bounds for the scrollbar.</p>
   * 更新滚动条的边界。列表滚动时调用
   *
   * <p>Override in each subclass of this base class.</p>
   */
  public void onUpdateScrollbar(int dy) {

    // item总数量(行数)
    int rowCount = 0;

    // 获取item总数量
    if (getAdapter() != null){
        rowCount = getAdapter().getItemCount();
    }

    // 如果是GridLayoutManager
    if (getLayoutManager() instanceof GridLayoutManager) {

      // 计算行数
//      rowCount = (int) Math.ceil((double) rowCount / spanCount);
      // 获取第一个可见item
      final int firstVisiblePosition = ((GridLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();
      // 获取最后一个可见item
      final int lastVisiblePosition = ((GridLayoutManager) getLayoutManager()).findLastVisibleItemPosition();
      ViewHolder Holder = findViewHolderForLayoutPosition(firstVisiblePosition);
      if (Holder != null && Holder.itemView instanceof TextView) {
        return;
      }
      // 获取GridLayoutManager列数
      int spanCount = ((GridLayoutManager) getLayoutManager()).getSpanCount();
      rowCount = (int) Math.ceil((double) rowCount / spanCount);

    }
    // Skip early if, there are no items
    // 如果没有项目，请尽早跳过。
    if (rowCount == 0) {
      fastScrollBar.setThumbOffset(-1, -1);
      return;
    }

    // 获取RV item信息
    getCurScrollState(scrollPositionState);
    // Skip early if, there no child laid out in the container.
    // 如果没有孩子在容器中摆放，请尽早跳过。
    if (scrollPositionState.rowIndex < 0) {
      fastScrollBar.setThumbOffset(-1, -1);
      return;
    }
    // 更新滚动条的位置
    synchronizeScrollBarThumbOffsetToViewScroll(scrollPositionState, rowCount);
  }

  /**
   * <p>Override in each subclass of this base class.</p>
   */
  public void onFastScrollCompleted() {
  }

  /**
   * Returns information about the item that the recycler view is currently scrolled to.
   * 返回有关回收器视图当前滚动到的项的信息。
   */
  protected void getCurScrollState(ScrollPositionState stateOut) {
    // 初始化状态
    stateOut.rowIndex = -1;
    stateOut.rowTopOffset = -1;
    stateOut.rowHeight = -1;

    // 如果没有行，请不执行
    int rowCount = getAdapter().getItemCount();
    if (rowCount == 0) {
      Log.e("FastScroll", "获取Item数为0");
      return;
    }
    // 子View数量为0，请不执行
    View child = getChildAt(0);
    if (child == null) {
      return;
    }
    // 设置第一个可见的索引
    stateOut.rowIndex = getChildPosition(child);
    // 如果是GridLayoutManager
    if (getLayoutManager() instanceof GridLayoutManager) {
      // 重新计算第一个可见的索引
      stateOut.rowIndex = stateOut.rowIndex / ((GridLayoutManager) getLayoutManager()).getSpanCount();
    }

    // 第一个可见索引的偏移量
    stateOut.rowTopOffset = getLayoutManager().getDecoratedTop(child) / (float) child.getHeight();
    // 获取并设置行高
    stateOut.rowHeight = calculateRowHeight(child.getHeight() + child.getPaddingTop() + child.getPaddingBottom());
  }

  /**
   * Calculates the row height based on the average of the visible children, to handle scrolling
   * through children with different heights gracefully
   * 根据可见子项的平均值计算行高，以便优雅地处理不同高度的子项滚动
   *
   * @return 行高
   */
  protected int calculateRowHeight(int fallbackHeight) {
    LayoutManager layoutManager = getLayoutManager();

    //如果是GridLayoutManager，直接返回第一个可见child高度
    if (layoutManager instanceof GridLayoutManager) {
      Log.e("FastScroll", "IsGridLayoutManager");
      // 获取第一个可见item
      final int firstVisiblePosition = ((GridLayoutManager) layoutManager).findFirstVisibleItemPosition();
      // 获取最后一个可见item
      final int lastVisiblePosition = ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
//      ViewHolder viewHolder = findViewHolderForLayoutPosition(firstVisiblePosition);
//      if (viewHolder != null && viewHolder.itemView instanceof TextView && lastVisiblePosition - firstVisiblePosition > 0) {
//        View child = findViewHolderForLayoutPosition(firstVisiblePosition+1).itemView;
//        return child.getHeight() + child.getPaddingTop() + child.getPaddingBottom();
//      }


      final int height = getHeight();
      final int paddingTop = getPaddingTop();
      final int paddingBottom = getPaddingBottom();
      // 可见多少行，如10.5f，完全10行，一半可见
      float visibleRows = 0f;
      // item总数量(行数)
      int rowCount = 0;
      // 获取GridLayoutManager列数
      int spanCount = ((GridLayoutManager) getLayoutManager()).getSpanCount();
      rowCount = (int) Math.ceil((double) rowCount / spanCount);

      if (rowCount > 0) {

        return Math.round((height - (paddingTop + 0F) + paddingBottom)) / rowCount;
      }
      return fallbackHeight;

    } else if (layoutManager instanceof LinearLayoutManager){
      // 如果是LinearLayoutManager

      // 获取第一个可见item
      final int firstVisiblePosition = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
      // 获取最后一个可见item
      final int lastVisiblePosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();

      // 如果最后一个可见item 位置id 大于 第一个可见item的id
      if (lastVisiblePosition > firstVisiblePosition) {
        final int height = getHeight();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();

        // 可见多少行，如10.5f，完全10行，一半可见
        float visibleRows = 0f;

        for (int position = firstVisiblePosition; position <= lastVisiblePosition; position++) {
          ViewHolder viewHolder = findViewHolderForLayoutPosition(position);
          if (viewHolder == null || viewHolder.itemView == null) {
            continue;
          }

          final View itemView = viewHolder.itemView;
          final int itemHeight = itemView.getHeight();
          if (itemHeight == 0) {
            continue;
          }

          // Finds how much of the itemView is actually visible.
          // 查找itemView实际可见的数量。
          // This allows smooth changes of the scrollbar thumb height
          // 这允许平滑地改变滚动条拇指高度
          final int visibleHeight = itemHeight
                  - Math.max(0, paddingBottom - layoutManager.getDecoratedTop(itemView)) // How much is cut at the top
                  - Math.max(0, paddingBottom + layoutManager.getDecoratedBottom(itemView) - height); // How much is cut at the bottom

          visibleRows += visibleHeight / (float) itemHeight;
        }

        // 计算平均item高度
        return Math.round((height - (paddingTop + paddingBottom)) / visibleRows);
      }
    }

    // 返回平均item高度
    return fallbackHeight;
  }

  /**
   * Iterface to implement in your {@link Adapter} to show a popup next to the scroller
   * 在{@link Adapter}中实现的界面，以显示滚动条旁边的弹出窗口
   */
  public interface SectionedAdapter {

    /**
     * @param position
     *     the item position
     * @return the section name for this item
     */
    @NonNull String getSectionName(int position);
  }

  /**
   * The current scroll state of the recycler view.  We use this in onUpdateScrollbar()
   * and scrollToPositionAtProgress() to determine the scroll position of the recycler view so
   * that we can calculate what the scroll bar looks like, and where to jump to from the fast
   * scroller.
   *
   * 回收者视图的当前滚动状态。 我们在onUpdateScrollbar（）和scrollToPositionAtProgress（）中使用它来确定回收器视图的滚动位置，
   * 以便我们可以计算滚动条的外观，以及从快速跳转到的位置卷轴。
   */
  public static class ScrollPositionState {

    // The index of the first visible row
    // 第一个可见行的索引
    public int rowIndex;
    // The offset of the first visible row, in percentage of the height
    // 第一个可见行的偏移量，以高度的百分比表示
    public float rowTopOffset;
    // The height of a given row (they are currently all the same height)
    // 给定行的高度（它们当前都是相同的高度）
    public int rowHeight;
  }

}