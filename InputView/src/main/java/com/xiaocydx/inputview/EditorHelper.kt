package com.xiaocydx.inputview

import android.view.View
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.recyclerview.widget.RecyclerView

/**
 * 提供帮助处理手势导航栏边到边的函数
 *
 * @author xcc
 * @date 2023/1/10
 */
interface EditorHelper {

    /**
     * 当分发到[WindowInsetsCompat]时，调用[block]
     *
     * 以处理[RecyclerView]的手势导航栏边到边为例：
     * ```
     * // layoutParams.height是固定高度
     * val initialHeight = recyclerView.layoutParams.height
     *
     * recyclerView.doOnApplyWindowInsetsCompat { _, insets, initialState ->
     *     val supportGestureNavBarEdgeToEdge = recyclerView.supportGestureNavBarEdgeToEdge(insets)
     *     val navigationBarHeight = insets.getNavigationBarHeight()
     *
     *     // 1. 若支持手势导航栏边到边，则增加高度，否则保持初始高度
     *     val height = when {
     *         !supportGestureNavBarEdgeToEdge -> initialHeight
     *         else -> navigationBarHeight + initialHeight
     *     }
     *     if (recyclerView.layoutParams.height != height) {
     *         recyclerView.updateLayoutParams { this.height = height }
     *     }
     *
     *     // 2. 若支持手势导航栏边到边，则增加paddingBottom，否则保持初始paddingBottom
     *     recyclerView.updatePadding(bottom = when {
     *         !supportGestureNavBarEdgeToEdge -> initialState.paddings.bottom
     *         else -> navigationBarHeight + initialState.paddings.bottom
     *     }
     *
     *     // 3. 由于支持手势导航栏边到边会增加paddingBottom，因此将clipToPadding设为false，
     *     // 使得recyclerView滚动时，能将内容绘制在paddingBottom区域，当滚动到底部时，
     *     // 留出paddingBottom区域，内容不会被手势导航栏遮挡。
     *     recyclerView.clipToPadding = !supportGestureNavBarEdgeToEdge
     * }
     * ```
     */
    fun View.doOnApplyWindowInsetsCompat(
        block: (view: View, insets: WindowInsetsCompat, initialState: ViewState) -> Unit
    ) {
        val initialState = ViewState(this)
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            block(view, insets, initialState)
            insets
        }
        // 此时可能已经错过分发，因此主动申请分发，
        // 确保调用一次block，完成视图的初始化逻辑。
        doOnAttach(ViewCompat::requestApplyInsets)
    }

    /**
     * 从[WindowInsetsCompat]获取导航栏的高度
     */
    fun WindowInsetsCompat.getNavigationBarHeight(): Int {
        return getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
    }

    /**
     * 是否支持手势导航栏边到边
     *
     * @return true表示支持，返回结果关联了`InputView.init()`的初始化配置。
     */
    fun View.supportGestureNavBarEdgeToEdge(insets: WindowInsetsCompat): Boolean {
        val window = getViewTreeWindow() ?: findViewTreeWindow()?.also(::setViewTreeWindow)
        return window?.supportGestureNavBarEdgeToEdge(insets) ?: false
    }
}

private val View.paddingDimensions: ViewDimensions
    get() = ViewDimensions(paddingLeft, paddingTop, paddingRight, paddingBottom)

private val View.marginDimensions: ViewDimensions
    get() = when (val params: ViewGroup.LayoutParams? = layoutParams) {
        is ViewGroup.MarginLayoutParams -> params.run {
            ViewDimensions(leftMargin, topMargin, rightMargin, bottomMargin)
        }
        else -> ViewDimensions.EMPTY
    }

/**
 * 视图的初始状态
 */
data class ViewState(
    /**
     * 视图的初始paddings
     */
    val paddings: ViewDimensions = ViewDimensions.EMPTY,

    /**
     * 视图的初始margins
     */
    val margins: ViewDimensions = ViewDimensions.EMPTY
) {
    constructor(view: View) : this(
        paddings = view.paddingDimensions,
        margins = view.marginDimensions
    )
}

/**
 * 视图的初始参数
 */
data class ViewDimensions(
    @Px val left: Int,
    @Px val top: Int,
    @Px val right: Int,
    @Px val bottom: Int
) {
    companion object {
        val EMPTY = ViewDimensions(0, 0, 0, 0)
    }
}