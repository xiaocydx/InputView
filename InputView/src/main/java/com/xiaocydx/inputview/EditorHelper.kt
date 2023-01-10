package com.xiaocydx.inputview

import android.view.View
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach

/**
 * @author xcc
 * @date 2023/1/10
 */
interface EditorHelper {

    fun View.doOnApplyWindowInsetsCompat(
        block: (view: View, insets: WindowInsetsCompat, initialState: ViewState) -> Unit
    ) {
        val initialState = ViewState(this)
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            block(view, insets, initialState)
            insets
        }
        doOnAttach(ViewCompat::requestApplyInsets)
    }

    fun WindowInsetsCompat.getNavigationBarHeight(): Int {
        return getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
    }

    fun View.supportGestureNavBarEdgeToEdge(insets: WindowInsetsCompat): Boolean {
        val window = getViewTreeWindow() ?: findViewTreeWindow()?.also(::setViewTreeWindow)
        return window?.supportGestureNavBarEdgeToEdge(insets) ?: false
    }
}

private val View.paddingDimensions: ViewDimensions
    get() = ViewDimensions(paddingLeft, paddingTop, paddingRight, paddingBottom)

private val View.marginDimensions: ViewDimensions
    get() = when (val lp: ViewGroup.LayoutParams? = layoutParams) {
        is ViewGroup.MarginLayoutParams -> {
            ViewDimensions(lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin)
        }
        else -> ViewDimensions.EMPTY
    }

data class ViewState(
    val paddings: ViewDimensions = ViewDimensions.EMPTY,
    val margins: ViewDimensions = ViewDimensions.EMPTY
) {
    constructor(view: View) : this(
        paddings = view.paddingDimensions,
        margins = view.marginDimensions
    )
}

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