package com.xiaocydx.inputview

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.view.*
import androidx.annotation.CheckResult
import androidx.core.graphics.Insets
import androidx.core.view.*
import androidx.fragment.app.DialogFragment

/**
 * 初始化[InputView]所需的配置
 *
 * **注意**：该函数需要在[Activity.onCreate]调用，暂时不支持[Dialog]和[DialogFragment]。
 *
 * @param statusBarEdgeToEdge     是否状态栏边到边
 * @param gestureNavBarEdgeToEdge 是否手势导航栏边到边
 */
fun InputView.Companion.init(
    window: Window,
    statusBarEdgeToEdge: Boolean = false,
    gestureNavBarEdgeToEdge: Boolean = false
) {
    ViewTreeWindow(window, statusBarEdgeToEdge, gestureNavBarEdgeToEdge).attach()
}

/**
 * 查找视图树的[ViewTreeWindow]，若查找不到，则返回`null`
 */
internal fun View.findViewTreeWindow(): ViewTreeWindow? {
    var found: ViewTreeWindow? = getTag(R.id.tag_view_tree_window) as? ViewTreeWindow
    if (found != null) return found
    var parent: ViewParent? = parent
    while (found == null && parent is View) {
        found = parent.getTag(R.id.tag_view_tree_window) as? ViewTreeWindow
        parent = (parent as View).parent
    }
    return found
}

internal class ViewTreeWindow(
    private val window: Window,
    private val statusBarEdgeToEdge: Boolean,
    private val gestureNavBarEdgeToEdge: Boolean
) {
    private val statusBarType = WindowInsetsCompat.Type.statusBars()
    private val navBarType = WindowInsetsCompat.Type.navigationBars()
    private val imeType = WindowInsetsCompat.Type.ime()

    fun attach() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            ViewCompat.onApplyWindowInsets(window.decorView, insets.decorWindowInsets())
            insets
        }

        val content = window.decorView.findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(content) { _, insets ->
            content.updatePadding(
                top = insets.contentPaddingTop(),
                bottom = insets.contentPaddingBottom()
            )
            insets
        }

        if (statusBarEdgeToEdge) {
            // FIXME: 暂时用透明背景
            window.statusBarColor = Color.TRANSPARENT
        }

        // 兼容Android各版本IME的WindowInsets分发
        @Suppress("DEPRECATION")
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.decorView.setTag(R.id.tag_view_tree_window, this)
    }

    @CheckResult
    private fun WindowInsetsCompat.decorWindowInsets(): WindowInsetsCompat {
        var outcome = this
        if (statusBarEdgeToEdge) {
            outcome = outcome.consumeStatusBarrHeight()
        }
        if (outcome.supportGestureNavBarEdgeToEdge()) {
            outcome = outcome.consumeNavigationBarHeight()
        }
        return outcome
    }

    @CheckResult
    private fun WindowInsetsCompat.consumeStatusBarrHeight(): WindowInsetsCompat {
        val oldInsets = getInsets(statusBarType)
        val newInsets = Insets.of(oldInsets.left, 0, oldInsets.right, oldInsets.bottom)
        return WindowInsetsCompat.Builder(this).setInsets(statusBarType, newInsets).build()
    }

    @CheckResult
    private fun WindowInsetsCompat.consumeNavigationBarHeight(): WindowInsetsCompat {
        val oldInsets = getInsets(navBarType)
        val newInsets = Insets.of(oldInsets.left, oldInsets.top, oldInsets.right, 0)
        return WindowInsetsCompat.Builder(this).setInsets(navBarType, newInsets).build()
    }

    private fun WindowInsetsCompat.contentPaddingTop(): Int = when {
        statusBarEdgeToEdge -> 0
        else -> getStatusBarHeight(this)
    }

    private fun WindowInsetsCompat.contentPaddingBottom(): Int = when {
        supportGestureNavBarEdgeToEdge() -> 0
        else -> getNavigationBarHeight(this)
    }

    private fun WindowInsetsCompat.supportGestureNavBarEdgeToEdge(): Boolean {
        return gestureNavBarEdgeToEdge && isGestureNavigationBar()
    }

    private fun WindowInsetsCompat.isGestureNavigationBar(): Boolean {
        val threshold = (24 * window.decorView.resources.displayMetrics.density).toInt()
        return getNavigationBarHeight(this) <= threshold.coerceAtLeast(66)
    }

    fun getRootWindowInsets(): WindowInsetsCompat? {
        return ViewCompat.getRootWindowInsets(window.decorView)
    }

    fun getStatusBarHeight(insets: WindowInsetsCompat): Int {
        return insets.getInsets(statusBarType).top
    }

    fun getNavigationBarHeight(insets: WindowInsetsCompat): Int {
        return insets.getInsets(navBarType).bottom
    }

    fun getImeHeight(insets: WindowInsetsCompat): Int {
        return insets.getInsets(imeType).bottom
    }

    fun getImeOffset(insets: WindowInsetsCompat): Int {
        val imeHeight = getImeHeight(insets)
        if (insets.supportGestureNavBarEdgeToEdge()) return imeHeight
        val navBarHeight = getNavigationBarHeight(insets)
        return (imeHeight - navBarHeight).coerceAtLeast(0)
    }

    fun getNavigationOffset(insets: WindowInsetsCompat): Int = when {
        !insets.supportGestureNavBarEdgeToEdge() -> 0
        else -> getNavigationBarHeight(insets)
    }

    fun containsImeType(typeMask: Int): Boolean {
        return typeMask and imeType == imeType
    }

    fun createWindowInsetsController(editText: View): WindowInsetsControllerCompat {
        return WindowInsetsControllerCompat(window, editText)
    }
}