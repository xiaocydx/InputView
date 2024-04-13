package com.xiaocydx.inputview.sample.debug

import android.content.Context
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.widget.LinearLayoutCompat
import com.google.android.material.textview.MaterialTextView
import com.xiaocydx.inputview.sample.common.dp
import com.xiaocydx.inputview.sample.common.matchParent
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.common.wrapContent
import com.xiaocydx.insets.consumeInsets
import com.xiaocydx.insets.navigationBars
import com.xiaocydx.insets.requestApplyInsetsCompat
import com.xiaocydx.insets.setOnApplyWindowInsetsListenerCompat
import com.xiaocydx.insets.statusBars

/**
 * @author xcc
 * @date 2024/4/13
 */
class InsetsDebugView(context: Context) : LinearLayoutCompat(context) {
    private val statusBarsDebug = MaterialTextView(context)
    private val navigationBarsDebug = MaterialTextView(context)
    private var isStatusBarsDebugEnabled = true
    private var isNavigationBarsDebugEnabled = true

    init {
        orientation = VERTICAL
        addView(statusBarsDebug, wrapContent, 44.dp)
        addView(navigationBarsDebug, wrapContent, 44.dp)

        statusBarsDebug.setTextColor(0xFFAA454D.toInt())
        navigationBarsDebug.setTextColor(0xFFAA454D.toInt())
        statusBarsDebug.onClick {
            isStatusBarsDebugEnabled = !isStatusBarsDebugEnabled
            statusBarsDebug.text = when {
                isStatusBarsDebugEnabled -> "StatusBarsDebug已开启"
                else -> "StatusBarsDebug已关闭"
            }
            statusBarsDebug.requestApplyInsetsCompat()
        }
        navigationBarsDebug.onClick {
            isNavigationBarsDebugEnabled = !isNavigationBarsDebugEnabled
            navigationBarsDebug.text = when {
                isNavigationBarsDebugEnabled -> "NavigationBarsDebug已开启"
                else -> "NavigationBarsDebug已关闭"
            }
            navigationBarsDebug.requestApplyInsetsCompat()
        }
        statusBarsDebug.performClick()
        navigationBarsDebug.performClick()
    }

    fun attach(window: Window) {
        val contentParent = window.findViewById<ViewGroup>(android.R.id.content)
        contentParent.addView(this, matchParent, matchParent)
        contentParent.setOnApplyWindowInsetsListenerCompat { _, insets ->
            var typeMask = 0
            if (isStatusBarsDebugEnabled) typeMask = typeMask or statusBars()
            if (isNavigationBarsDebugEnabled) typeMask = typeMask or navigationBars()
            insets.consumeInsets(typeMask)
        }
    }

    companion object {
        fun attach(window: Window) {
            InsetsDebugView(window.decorView.context).attach(window)
        }
    }
}