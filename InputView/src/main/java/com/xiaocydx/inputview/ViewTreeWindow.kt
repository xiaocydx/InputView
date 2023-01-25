package com.xiaocydx.inputview

import android.app.Activity
import android.graphics.*
import android.view.*
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.Interpolator
import androidx.annotation.CheckResult
import androidx.core.graphics.Insets
import androidx.core.view.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * 初始化[InputView]所需的配置
 *
 * **注意**：该函数需要在[Activity.onCreate]调用。
 *
 * [onApplyWindowInsetsListener]能避免跟内部实现产生冲突，实际效果等同于：
 * ```
 * ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
 *     insets.consumeInsets(typeMask).let { ViewCompat.onApplyWindowInsets(v, it) }
 * }
 * ```
 *
 * [windowInsetsAnimationCallback]能避免跟内部实现产生冲突，实际效果等同于：
 * ```
 * ViewCompat.setWindowInsetsAnimationCallback(window.decorView, callback)
 * ```
 *
 * @param gestureNavBarEdgeToEdge       是否手势导航栏边到边
 * @param onApplyWindowInsetsListener   `window.decorView`的[OnApplyWindowInsetsListener]
 * @param windowInsetsAnimationCallback `window.decorView`的[WindowInsetsAnimationCompat.Callback]
 */
fun InputView.Companion.init(
    activity: Activity,
    gestureNavBarEdgeToEdge: Boolean = false,
    onApplyWindowInsetsListener: OnApplyWindowInsetsListener? = null,
    windowInsetsAnimationCallback: WindowInsetsAnimationCompat.Callback? = null
) {
    ViewTreeWindow(
        activity.window,
        gestureNavBarEdgeToEdge,
        onApplyWindowInsetsListener,
        windowInsetsAnimationCallback
    ).attach()
}

/**
 * 初始化[InputView]所需的配置
 *
 * **注意**：该函数需要在[BottomSheetDialogFragment.onCreateView]调用。
 *
 * [onApplyWindowInsetsListener]能避免跟内部实现产生冲突，实际效果等同于：
 * ```
 * ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
 *     insets.consumeInsets(typeMask).let { ViewCompat.onApplyWindowInsets(v, it) }
 * }
 * ```
 *
 * [windowInsetsAnimationCallback]能避免跟内部实现产生冲突，实际效果等同于：
 * ```
 * ViewCompat.setWindowInsetsAnimationCallback(window.decorView, callback)
 * ```
 *
 * @param gestureNavBarEdgeToEdge       是否手势导航栏边到边
 * @param onApplyWindowInsetsListener   `window.decorView`的[OnApplyWindowInsetsListener]
 * @param windowInsetsAnimationCallback `window.decorView`的[WindowInsetsAnimationCompat.Callback]
 */
fun InputView.Companion.init(
    dialogFragment: BottomSheetDialogFragment,
    gestureNavBarEdgeToEdge: Boolean = false,
    onApplyWindowInsetsListener: OnApplyWindowInsetsListener? = null,
    windowInsetsAnimationCallback: WindowInsetsAnimationCompat.Callback? = null
) {
    val window = dialogFragment.dialog?.window ?: return
    ViewTreeWindow(
        window,
        gestureNavBarEdgeToEdge,
        onApplyWindowInsetsListener,
        windowInsetsAnimationCallback
    ).attach { applyInsets ->
        // 对window.decorView到fragment.view之间的View不分发insets，
        // 目的是去除BottomSheetDialogFragment的边到边实现，自行实现状态栏和导航栏边到边，
        // 并确保fragment.view的子视图树insets分发正常、Android 11以下insets动画回调正常。
        dialogFragment.view?.let { ViewCompat.dispatchApplyWindowInsets(it, applyInsets) }
        WindowInsetsCompat.CONSUMED
    }
}

/**
 * 设置视图树的[ViewTreeWindow]
 */
internal fun View.setViewTreeWindow(window: ViewTreeWindow) {
    setTag(R.id.tag_view_tree_window, window)
}

/**
 * 获取视图树的[ViewTreeWindow]，若未设置，则返回`null`
 */
internal fun View.getViewTreeWindow(): ViewTreeWindow? {
    return getTag(R.id.tag_view_tree_window) as? ViewTreeWindow
}

/**
 * 查找视图树的[ViewTreeWindow]，若查找不到，则返回`null`
 */
internal fun View.findViewTreeWindow(): ViewTreeWindow? {
    var found: ViewTreeWindow? = getViewTreeWindow()
    if (found != null) return found
    var parent: ViewParent? = parent
    while (found == null && parent is View) {
        found = parent.getViewTreeWindow()
        parent = (parent as View).parent
    }
    return found
}

internal class ViewTreeWindow(
    private val window: Window,
    private val gestureNavBarEdgeToEdge: Boolean,
    private val onApplyWindowInsetsListener: OnApplyWindowInsetsListener?,
    private val windowInsetsAnimationCallback: WindowInsetsAnimationCompat.Callback?
) {
    private val statusBarType = WindowInsetsCompat.Type.statusBars()
    private val navBarType = WindowInsetsCompat.Type.navigationBars()
    private val imeType = WindowInsetsCompat.Type.ime()

    fun attach(finalInsets: ((applyInsets: WindowInsetsCompat) -> WindowInsetsCompat)? = null) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val contentRoot = (window.decorView as ViewGroup).children.first { it is ViewGroup }
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            val applyInsets = onApplyWindowInsetsListener?.onApplyWindowInsets(v, insets) ?: insets
            ViewCompat.onApplyWindowInsets(v, applyInsets.decorInsets())
            contentRoot.updateMargins(
                top = applyInsets.contentMarginTop,
                bottom = applyInsets.contentMarginBottom
            )
            finalInsets?.invoke(applyInsets) ?: applyInsets
        }

        // SOFT_INPUT_ADJUST_RESIZE用于兼容Android各版本IME的insets分发
        @Suppress("DEPRECATION")
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.setWindowInsetsAnimationCallbackCompat(windowInsetsAnimationCallback)
        window.decorView.setViewTreeWindow(this)
    }

    @CheckResult
    private fun WindowInsetsCompat.decorInsets(): WindowInsetsCompat {
        return if (supportGestureNavBarEdgeToEdge) consumeInsets(navBarType) else this
    }

    @CheckResult
    private fun WindowInsetsCompat.consumeInsets(typeMask: Int): WindowInsetsCompat {
        return WindowInsetsCompat.Builder(this).setInsets(typeMask, Insets.NONE).build()
    }

    private fun View.updateMargins(
        left: Int = marginLeft,
        top: Int = marginTop,
        right: Int = marginRight,
        bottom: Int = marginBottom
    ) {
        val params = layoutParams as? MarginLayoutParams ?: return
        val changed = left != marginLeft || top != marginTop
                || right != marginTop || bottom != marginBottom
        params.setMargins(left, top, right, bottom)
        if (changed) layoutParams = params
    }

    private val WindowInsetsCompat.contentMarginTop: Int
        get() = getInsets(statusBarType).top

    private val WindowInsetsCompat.contentMarginBottom: Int
        get() = if (supportGestureNavBarEdgeToEdge) 0 else navigationBarHeight

    private val WindowInsetsCompat.isGestureNavigationBar: Boolean
        get() {
            val threshold = (24 * window.decorView.resources.displayMetrics.density).toInt()
            return navigationBarHeight <= threshold.coerceAtLeast(66)
        }

    private val WindowInsetsCompat.navigationBarHeight: Int
        get() = getInsets(navBarType).bottom

    val WindowInsetsCompat.imeHeight
        get() = getInsets(imeType).bottom

    val WindowInsetsCompat.imeOffset: Int
        get() {
            if (supportGestureNavBarEdgeToEdge) return imeHeight
            return (imeHeight - navigationBarHeight).coerceAtLeast(0)
        }

    val WindowInsetsCompat.navigationBarOffset: Int
        get() = if (supportGestureNavBarEdgeToEdge) navigationBarHeight else 0

    val WindowInsetsCompat.supportGestureNavBarEdgeToEdge: Boolean
        get() = gestureNavBarEdgeToEdge && isGestureNavigationBar

    /**
     * 传入[view]是为了确保转换出的[WindowInsetsCompat]是正确的结果
     */
    fun WindowInsets.toCompat(view: View): WindowInsetsCompat {
        return WindowInsetsCompat.toWindowInsetsCompat(this, view)
    }

    fun WindowInsetsAnimationCompat.containsImeType(): Boolean {
        return typeMask and imeType == imeType
    }

    fun getRootWindowInsets(): WindowInsetsCompat? {
        return ViewCompat.getRootWindowInsets(window.decorView)
    }

    fun createWindowInsetsController(editText: View): WindowInsetsControllerCompat {
        return WindowInsetsControllerCompat(window, editText)
    }

    fun modifyImeAnimation(durationMillis: Long, interpolator: Interpolator) {
        window.modifyImeAnimationCompat(durationMillis, interpolator)
    }

    fun restoreImeAnimation() {
        window.restoreImeAnimationCompat()
    }
}