/*
 * Copyright 2022 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaocydx.inputview.sample.common

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import android.view.View.MeasureSpec.UNSPECIFIED
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.annotation.Px
import androidx.core.view.isGone
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager

/**
 * 自定义ViewGroup的测量、布局相关扩展
 *
 * @author xcc
 * @date 2022/6/2
 */
abstract class CustomLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    final override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        onMeasureChildren(widthMeasureSpec, heightMeasureSpec)
    }

    /**
     * [CustomLayout]的子类需要重写[onMeasure]，
     * 定义抽象函数[onMeasureChildren]，作为重写提示。
     */
    protected abstract fun onMeasureChildren(widthMeasureSpec: Int, heightMeasureSpec: Int)

    /**
     * [CustomLayout]的子类一般不作为滚动容器，因此重写返回`false`，
     * 防止产生按压延时，详细逻辑可以看[View.onTouchEvent]的[ACTION_DOWN]分支。
     */
    override fun shouldDelayChildPressedState(): Boolean = false

    protected inline fun addView(child: View, width: Int, height: Int, block: LayoutParams.() -> Unit) {
        addView(child, LayoutParams(width, height).also(block))
    }

    override fun generateLayoutParams(attrs: AttributeSet) = LayoutParams(context, attrs)

    override fun generateDefaultLayoutParams() = LayoutParams(wrapContent, wrapContent)

    override fun generateLayoutParams(p: ViewGroup.LayoutParams) = LayoutParams(p)

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?) = p is LayoutParams

    class LayoutParams : MarginLayoutParams {
        constructor(c: Context, attrs: AttributeSet) : super(c, attrs)
        constructor(width: Int, height: Int) : super(width, height)
        constructor(source: MarginLayoutParams) : super(source)
        constructor(source: ViewGroup.LayoutParams) : super(source)
    }

    @get:Px
    @setparam:Px
    protected var MarginLayoutParams.horizontalMargin: Int
        get() = leftMargin + rightMargin
        set(size) = updateMargins(left = size, right = size)

    @get:Px
    @setparam:Px
    protected var MarginLayoutParams.verticalMargin: Int
        get() = topMargin + bottomMargin
        set(size) = updateMargins(top = size, bottom = size)

    @get:Px
    @setparam:Px
    protected var View.horizontalPadding: Int
        get() = paddingLeft + paddingRight
        set(size) = updatePadding(left = size, right = size)

    @get:Px
    @setparam:Px
    protected var View.verticalPadding: Int
        get() = paddingTop + paddingBottom
        set(size) = updatePadding(top = size, bottom = size)

    @get:Px
    protected val View.horizontalMargin: Int
        get() = marginLeft + marginRight

    @get:Px
    protected val View.verticalMargin: Int
        get() = marginTop + marginBottom

    @get:Px
    protected val View.measuredWidthWithMargins: Int
        get() = measuredWidth + horizontalMargin

    @get:Px
    protected val View.measuredHeightWithMargins: Int
        get() = measuredHeight + verticalMargin

    /**
     * [TypedValue.complexToDimensionPixelSize]的舍入逻辑，
     * 用于确保[dp]、[sp]转换的px值，和xml解析转换的px值一致。
     */
    @Px
    private inline fun Float.toRoundingPx(): Int {
        return (if (this >= 0) this + 0.5f else this - 0.5f).toInt()
    }

    @get:Px
    protected val Int.dp: Int
        get() = toFloat().dp

    @get:Px
    protected val Float.dp: Int
        get() = (this * resources.displayMetrics.density).toRoundingPx()

    @get:Px
    protected val Int.sp: Int
        get() = toFloat().sp

    @get:Px
    protected val Float.sp: Int
        get() = (this * resources.displayMetrics.scaledDensity).toRoundingPx()

    @Px
    protected fun Int.toMeasureSpecSize(): Int {
        return MeasureSpec.getSize(this)
    }

    protected fun Int.toMeasureSpecMode(): Int {
        return MeasureSpec.getMode(this)
    }

    protected fun Int.toExactlyMeasureSpec(): Int {
        return MeasureSpec.makeMeasureSpec(this, MeasureSpec.EXACTLY)
    }

    protected fun Int.toAtMostMeasureSpec(): Int {
        return MeasureSpec.makeMeasureSpec(this, MeasureSpec.AT_MOST)
    }

    protected fun View.requireParentView(): ViewGroup {
        requireNotNull(parent) { "parent为空" }
        return requireNotNull(parent as? ViewGroup) { "parent的类型不是ViewGroup" }
    }

    /**
     * [defaultWidthMeasureSpec]的意图，是给到View最大可用size
     *
     * ### [UNSPECIFIED]场景
     * `parent.onMeasure()`传入的`widthMeasureSpec`，其测量模式可能为[UNSPECIFIED]，
     * 例如`parent`作为[RecyclerView]的`itemView`，`layoutManager`是水平方向的[LinearLayoutManager]，
     * 当`parent.layoutParams.width = wrapContent`时，`widthMeasureSpec`的测量模式为[UNSPECIFIED]，
     * 详细逻辑可以看[LayoutManager.measureChildWithMargins]。
     *
     * ### [wrapContent]分支
     * 若`parent`的`widthMeasureSpec`的测量模式为[UNSPECIFIED]，
     * 则`super.onMeasure()`会将宽度测量为[getSuggestedMinimumWidth]，
     * 这不符合[defaultWidthMeasureSpec]的意图，因此[wrapContent]分支，
     * 不返回`parent.measuredWidth.toAtMostMeasureSpec()`，
     * 而是返回`Int.MAX_VALUE.toAtMostMeasureSpec()`。
     */
    protected fun View.defaultWidthMeasureSpec(): Int = when (layoutParams.width) {
        matchParent -> requireParentView().measuredWidth.toExactlyMeasureSpec()
        wrapContent -> Int.MAX_VALUE.toAtMostMeasureSpec()
        else -> layoutParams.width.toExactlyMeasureSpec()
    }

    /**
     * [defaultHeightMeasureSpec]的意图，是给到View最大可用size
     *
     * ### [UNSPECIFIED]场景
     * `parent.onMeasure()`传入的`heightMeasureSpec`，其测量模式可能为[UNSPECIFIED]，
     * 例如`parent`作为[RecyclerView]的`itemView`，`layoutManager`是垂直方向的[LinearLayoutManager]，
     * 当`parent.layoutParams.height = wrapContent`时，`heightMeasureSpec`的测量模式为[UNSPECIFIED]，
     * 详细逻辑可以看[LayoutManager.measureChildWithMargins]。
     *
     * ### [wrapContent]分支
     * 若`parent`的`heightMeasureSpec`的测量模式为[UNSPECIFIED]，
     * 则`super.onMeasure()`会将高度测量为[getSuggestedMinimumHeight]，
     * 这不符合[defaultHeightMeasureSpec]的意图，因此[wrapContent]分支，
     * 不返回`parent.measuredHeight.toAtMostMeasureSpec()`，
     * 而是返回`Int.MAX_VALUE.toAtMostMeasureSpec()`。
     */
    protected fun View.defaultHeightMeasureSpec(): Int = when (layoutParams.height) {
        matchParent -> requireParentView().measuredHeight.toExactlyMeasureSpec()
        wrapContent -> Int.MAX_VALUE.toAtMostMeasureSpec()
        else -> layoutParams.height.toExactlyMeasureSpec()
    }

    protected fun View.maxWidthMeasureSpec(@Px size: Int): Int = when (layoutParams.width) {
        matchParent -> size.toExactlyMeasureSpec()
        wrapContent -> size.toAtMostMeasureSpec()
        else -> layoutParams.width.coerceAtMost(size).toExactlyMeasureSpec()
    }

    protected fun View.maxHeightMeasureSpec(@Px size: Int): Int = when (layoutParams.height) {
        matchParent -> size.toExactlyMeasureSpec()
        wrapContent -> size.toAtMostMeasureSpec()
        else -> layoutParams.height.coerceAtMost(size).toExactlyMeasureSpec()
    }

    protected fun View.measureWith(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (isGone) return
        measure(widthMeasureSpec, heightMeasureSpec)
    }

    protected fun View.measureWithDefault() {
        if (isGone) return
        measure(defaultWidthMeasureSpec(), defaultHeightMeasureSpec())
    }

    protected fun View.layout(x: Int, y: Int) {
        if (isGone) return
        layout(x, y, x + measuredWidth, y + measuredHeight)
    }

    protected fun View.layoutFromRight(x: Int, y: Int) {
        if (isGone) return
        val left = requireParentView().measuredWidth - x - measuredWidth
        layout(left, y, left + measuredWidth, y + measuredHeight)
    }

    /**
     * [View]布局到[other]的中心
     *
     * @param other   父View或者同级子View
     * @param offsetX 水平方向偏移
     * @param offsetY 垂直方向偏移
     */
    protected fun View.layoutToCenter(other: View, offsetX: Int = 0, offsetY: Int = 0) {
        if (isGone) return
        val x = alignCenterX(other) + offsetX
        val y = alignCenterY(other) + offsetY
        layout(x, y, x + measuredWidth, y + measuredHeight)
    }

    /**
     * [View]的水平中心对齐[other]的水平中心
     *
     * @param other 父View或者同级子View
     * @return 对齐后的`view.left`值
     */
    @CheckResult
    protected fun View.alignCenterX(other: View): Int {
        return if (other === parent) {
            alignCenterX(left = 0, right = requireParentView().width)
        } else {
            alignCenterX(other.left, other.right)
        }
    }

    /**
     * [View]的水平中心对齐[left]到[right]的中心
     *
     * @return 对齐后的`view.left`值
     */
    @CheckResult
    protected fun View.alignCenterX(left: Int, right: Int): Int {
        return left + (right - left - measuredWidth) / 2
    }

    /**
     * [View]的垂直中心对齐[other]的垂直中心
     *
     * @param other 父View或者同级子View
     * @return 对齐后的`view.top`值
     */
    @CheckResult
    protected fun View.alignCenterY(other: View): Int {
        return if (other === parent) {
            alignCenterY(top = 0, bottom = requireParentView().height)
        } else {
            alignCenterY(other.top, other.bottom)
        }
    }

    /**
     * [View]的垂直中心对齐[top]到[bottom]的中心
     *
     * @return 对齐后的`view.top`值
     */
    @CheckResult
    protected fun View.alignCenterY(top: Int, bottom: Int): Int {
        return top + (bottom - top - measuredHeight) / 2
    }

    protected companion object {
        const val matchParent = ViewGroup.LayoutParams.MATCH_PARENT
        const val wrapContent = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}