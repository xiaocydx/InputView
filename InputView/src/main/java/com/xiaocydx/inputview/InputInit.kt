package com.xiaocydx.inputview

import android.view.View
import android.view.ViewParent
import android.view.Window
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

fun InputView.Companion.init(window: Window) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.windowInsetsDispatchCompat()
    window.decorView.setViewTreeWindow(window)
    val content = window.decorView.findViewById<View>(android.R.id.content)
    ViewCompat.setOnApplyWindowInsetsListener(content) { _, insets ->
        content.updatePadding(
            top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top,
            bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        )
        insets
    }
}

@Suppress("DEPRECATION")
private fun Window.windowInsetsDispatchCompat() {
    setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
}

private fun View.setViewTreeWindow(window: Window) {
    setTag(R.id.tag_view_tree_window, window)
}

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