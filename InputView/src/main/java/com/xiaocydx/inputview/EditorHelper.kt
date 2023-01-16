package com.xiaocydx.inputview

import android.view.View
import androidx.annotation.Px
import androidx.core.view.*
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
     * // recyclerView.layoutParams.height的初始高度是固定值
     *
     * recyclerView.doOnApplyWindowInsets { _, insets, initialState ->
     *     val supportGestureNavBarEdgeToEdge = recyclerView.supportGestureNavBarEdgeToEdge(insets)
     *     val navigationBarHeight = insets.getNavigationBarHeight()
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

        if (isAttachedToWindow) {
            ViewCompat.requestApplyInsets(this)
        }

        getTag(R.id.tag_view_request_apply_insets)
            ?.let { it as? View.OnAttachStateChangeListener }
            ?.let(::removeOnAttachStateChangeListener)

        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                // 当view首次或再次附加到Window时，可能错过WindowInsets分发,
                // 因此主动申请WindowInsets分发，确保调用block完成视图初始化。
                ViewCompat.requestApplyInsets(view)
            }

            override fun onViewDetachedFromWindow(view: View) = Unit
        }
        setTag(R.id.tag_view_request_apply_insets, listener)
        addOnAttachStateChangeListener(listener)
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
        return window?.run { insets.supportGestureNavBarEdgeToEdge } ?: false
    }
}

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