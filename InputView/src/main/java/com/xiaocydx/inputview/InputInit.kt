package com.xiaocydx.inputview

import android.app.Activity
import android.view.View
import android.view.ViewParent
import android.view.Window
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * 初始化[InputView]所需的WindowInsets配置
 *
 * **注意**：该函数需要在[Activity.onCreate]调用。
 */
fun InputView.Companion.init(window: Window) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.imeWindowInsetsDispatchCompat()
    window.setViewTreeWindow()
    val content = window.decorView.findViewById<View>(android.R.id.content)
    // TODO: 支持导航栏edge-to-edge
    ViewCompat.setOnApplyWindowInsetsListener(content) { _, insets ->
        content.updatePadding(
            top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top,
            bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        )
        insets
    }
}

/**
 * 兼容Android各版本IME的WindowInsets分发
 */
@Suppress("DEPRECATION")
private fun Window.imeWindowInsetsDispatchCompat() {
    setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
}

/**
 * 设置视图树的[Window]
 */
private fun Window.setViewTreeWindow() {
    decorView.setTag(R.id.tag_view_tree_window, this)
}

/**
 * 查找[setViewTreeWindow]设置的[Window]，若查找不到，则返回`null`
 */
internal fun View.findViewTreeWindow(): Window? {
    var found: Window? = getTag(R.id.tag_view_tree_window) as? Window
    if (found != null) return found
    var parent: ViewParent? = parent
    while (found == null && parent is View) {
        found = parent.getTag(R.id.tag_view_tree_window) as? Window
        parent = (parent as View).parent
    }
    return found
}