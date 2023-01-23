package com.xiaocydx.inputview

import android.app.Activity
import android.app.Dialog
import android.view.*
import android.view.animation.Interpolator
import androidx.annotation.CheckResult
import androidx.core.graphics.Insets
import androidx.core.view.*
import androidx.fragment.app.DialogFragment

/**
 * 初始化[InputView]所需的配置
 *
 * **注意**：该函数需要在[Activity.onCreate]调用，暂时不支持[Dialog]和[DialogFragment]。
 *
 * [onApplyWindowInsetsListener]能避免跟内部实现产生冲突，实际效果等同于：
 * ```
 * ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
 *     modifyInsets(insets).let { ViewCompat.onApplyWindowInsets(v, it) }
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
    window: Window,
    gestureNavBarEdgeToEdge: Boolean = false,
    onApplyWindowInsetsListener: OnApplyWindowInsetsListener? = null,
    windowInsetsAnimationCallback: WindowInsetsAnimationCompat.Callback? = null
) {
    ViewTreeWindow(
        window,
        gestureNavBarEdgeToEdge,
        onApplyWindowInsetsListener,
        windowInsetsAnimationCallback
    ).attach()
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

    fun attach() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            val applyInsets = onApplyWindowInsetsListener?.onApplyWindowInsets(v, insets) ?: insets
            ViewCompat.onApplyWindowInsets(v, applyInsets.decorWindowInsets())
            applyInsets
        }

        val content = window.decorView.findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
            v.updatePadding(top = insets.contentPaddingTop, bottom = insets.contentPaddingBottom)
            insets
        }

        // 兼容Android各版本IME的WindowInsets分发
        @Suppress("DEPRECATION")
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.setWindowInsetsAnimationCallbackCompat(windowInsetsAnimationCallback)
        window.decorView.setViewTreeWindow(this)
    }

    @CheckResult
    private fun WindowInsetsCompat.decorWindowInsets(): WindowInsetsCompat {
        return if (supportGestureNavBarEdgeToEdge) consumeNavigationBarHeight() else this
    }

    @CheckResult
    private fun WindowInsetsCompat.consumeNavigationBarHeight(): WindowInsetsCompat {
        val oldInsets = getInsets(navBarType)
        val newInsets = Insets.of(oldInsets.left, oldInsets.top, oldInsets.right, 0)
        return WindowInsetsCompat.Builder(this).setInsets(navBarType, newInsets).build()
    }

    private val WindowInsetsCompat.contentPaddingTop: Int
        get() = getInsets(statusBarType).top

    private val WindowInsetsCompat.contentPaddingBottom: Int
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