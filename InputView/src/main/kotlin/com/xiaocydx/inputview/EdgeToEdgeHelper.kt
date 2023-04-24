/*
 * Copyright 2023 xiaocydx
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

package com.xiaocydx.inputview

import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.annotation.Px
import androidx.core.graphics.Insets
import androidx.core.view.*
import androidx.core.view.WindowInsetsCompat.Type.*
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.inputview.compat.requestApplyInsetsCompat
import com.xiaocydx.inputview.compat.setOnApplyWindowInsetsListenerCompat

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
        get() = getInsets(statusBars()).top

    /**
     * 获取导航栏高度
     */
    val WindowInsetsCompat.navigationBarHeight: Int
        get() = getInsets(navigationBars()).bottom

    /**
     * 消费指定的[InsetsType]类型集
     */
    @CheckResult
    fun WindowInsetsCompat.consume(@InsetsType typeMask: Int): WindowInsetsCompat {
        if (typeMask <= 0) return this
        return WindowInsetsCompat.Builder(this).setInsets(typeMask, Insets.NONE).build()
    }

    /**
     * 是否为手势导航栏
     */
    fun WindowInsetsCompat.isGestureNavigationBar(view: View): Boolean {
        val threshold = (24 * view.resources.displayMetrics.density).toInt()
        val stableHeight = getInsetsIgnoringVisibility(navigationBars()).bottom
        return stableHeight <= threshold.coerceAtLeast(66)
    }

    /**
     * 是否支持手势导航栏边到边
     *
     * @return 返回结果关联了`InputView.init()`的初始化配置。
     */
    fun WindowInsetsCompat.supportGestureNavBarEdgeToEdge(view: View): Boolean {
        val window = view.getOrFindViewTreeWindow() ?: return false
        return window.gestureNavBarEdgeToEdge && isGestureNavigationBar(view)
    }

    /**
     * 当分发到[WindowInsetsCompat]时，调用[block]
     *
     * 以实现[RecyclerView]的手势导航栏边到边为例：
     * ```
     * // recyclerView.layoutParams.height的初始高度是固定值
     *
     * recyclerView.doOnApplyWindowInsets { _, insets, initialState ->
     *     val navigationBarHeight = insets.navigationBarHeight
     *     val supportGestureNavBarEdgeToEdge = insets.supportGestureNavBarEdgeToEdge(recyclerView)
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
        setOnApplyWindowInsetsListenerCompat { view, insets ->
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
            requestApplyInsetsCompat()
        }
        removeRequestApplyInsetsOnAttach()
        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                view.requestApplyInsetsCompat()
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
     * 更新`margins`，有改变才申请重新布局
     */
    fun View.updateMargins(
        left: Int = marginLeft,
        top: Int = marginTop,
        right: Int = marginRight,
        bottom: Int = marginBottom
    ) {
        val params = layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val changed = left != marginLeft || top != marginTop
                || right != marginTop || bottom != marginBottom
        params.setMargins(left, top, right, bottom)
        if (changed) layoutParams = params
    }

    companion object : EdgeToEdgeHelper
}

/**
 * 不需要实现[EdgeToEdgeHelper]的便捷函数
 */
@Suppress("FunctionName")
inline fun <R> EdgeToEdgeHelper(block: EdgeToEdgeHelper.() -> R): R = with(EdgeToEdgeHelper, block)

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
    constructor(view: View) : this(ViewParams(view), ViewPaddings(view))
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