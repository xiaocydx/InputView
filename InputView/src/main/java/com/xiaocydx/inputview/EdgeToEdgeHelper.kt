package com.xiaocydx.inputview

import android.view.View
import androidx.annotation.Px
import androidx.core.view.*
import androidx.recyclerview.widget.RecyclerView

/**
 * 提供实现边到边的辅助函数
 *
 * @author xcc
 * @date 2023/1/10
 */
interface EdgeToEdgeHelper {

    /**
     * 获取状态栏高度
     */
    val WindowInsetsCompat.statusBarHeight: Int
        get() = getInsets(WindowInsetsCompat.Type.statusBars()).top

    /**
     * 获取导航栏高度
     */
    val WindowInsetsCompat.navigationBarHeight: Int
        get() = getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

    /**
     * 当分发到[WindowInsetsCompat]时，调用[block]
     *
     * 以实现[RecyclerView]的手势导航栏边到边为例：
     * ```
     * // recyclerView.layoutParams.height的初始高度是固定值
     *
     * recyclerView.doOnApplyWindowInsets { _, insets, initialState ->
     *     val navigationBarHeight = insets.navigationBarHeight
     *     val supportGestureNavBarEdgeToEdge = recyclerView.supportGestureNavBarEdgeToEdge(insets)
     *
     *     // 1. 若支持手势导航栏边到边，则增加高度，否则保持初始高度
     *     val height = when {
     *         !supportGestureNavBarEdgeToEdge -> initialState.params.height
     *         else -> navigationBarHeight + initialState.params.height
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
    fun View.doOnApplyWindowInsets(
        block: (view: View, insets: WindowInsetsCompat, initialState: ViewState) -> Unit
    ) {
        val initialState = ViewState(this)
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            block(view, insets, initialState)
            insets
        }
        // 当view首次或再次附加到window时，可能错过insets分发,
        // 因此主动申请insets分发，确保调用block完成视图初始化。
        requestApplyInsetsOnAttach()
    }

    /**
     * 当附加到window时，申请insets分发
     */
    fun View.requestApplyInsetsOnAttach() {
        if (isAttachedToWindow) {
            ViewCompat.requestApplyInsets(this)
        }
        removeRequestApplyInsetsOnAttach()
        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                ViewCompat.requestApplyInsets(view)
            }

            override fun onViewDetachedFromWindow(view: View) = Unit
        }
        setTag(R.id.tag_view_request_apply_insets, listener)
        addOnAttachStateChangeListener(listener)
    }

    /**
     * 移除[requestApplyInsetsOnAttach]的设置
     */
    fun View.removeRequestApplyInsetsOnAttach() {
        getTag(R.id.tag_view_request_apply_insets)
            ?.let { it as? View.OnAttachStateChangeListener }
            ?.let(::removeOnAttachStateChangeListener)
    }

    /**
     * 是否支持手势导航栏边到边
     *
     * @return true表示支持，返回结果关联了`InputView.init()`的初始化配置。
     */
    fun View.supportGestureNavBarEdgeToEdge(insets: WindowInsetsCompat): Boolean {
        return getOrFindViewTreeWindow()?.run { insets.supportGestureNavBarEdgeToEdge } ?: false
    }

    companion object : EdgeToEdgeHelper
}

/**
 * 不需要实现[EdgeToEdgeHelper]的便捷函数
 */
inline fun <R> withEdgeToEdgeHelper(block: EdgeToEdgeHelper.() -> R): R = with(EdgeToEdgeHelper, block)

/**
 * 视图的初始状态
 */
data class ViewState internal constructor(
    /**
     * 视图的初始params
     */
    val params: ViewParams,

    /**
     * 视图的初始paddings
     */
    val paddings: ViewPaddings,
) {
    internal constructor(view: View) : this(ViewParams(view), ViewPaddings(view))
}

/**
 * 视图的初始params
 */
data class ViewParams internal constructor(
    @Px val width: Int,
    @Px val height: Int,
    @Px val marginLeft: Int,
    @Px val marginTop: Int,
    @Px val marginRight: Int,
    @Px val marginBottom: Int
) {
    internal constructor(view: View) : this(
        width = view.layoutParams?.width ?: 0,
        height = view.layoutParams?.height ?: 0,
        marginLeft = view.marginLeft,
        marginTop = view.marginTop,
        marginRight = view.marginRight,
        marginBottom = view.marginBottom
    )
}

/**
 * 视图的初始paddings
 */
data class ViewPaddings internal constructor(
    @Px val left: Int,
    @Px val top: Int,
    @Px val right: Int,
    @Px val bottom: Int
) {
    internal constructor(view: View) : this(
        left = view.paddingLeft,
        top = view.paddingTop,
        right = view.paddingRight,
        bottom = view.paddingBottom
    )
}