package com.xiaocydx.inputview.sample.transform.figure.pager

import android.view.View
import android.widget.TextView
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.transform.figure.FigureScene
import com.xiaocydx.inputview.sample.transform.figure.FigureState
import java.lang.ref.WeakReference

/**
 * @author xcc
 * @date 2024/4/14
 */
class TextPager(
    private val textView: TextView,
    private val go: (FigureScene?) -> Unit
) {
    private var text: String? = null

    init {
        textView.onClick { go(FigureScene.InputText) }
    }

    fun updateCurrentPage(state: FigureState) {
        if (state.currentText != text) {
            text = state.currentText
            textView.text = if (text.isNullOrEmpty()) "输入文字" else text
        }
    }

    fun textView(): WeakReference<View> {
        return WeakReference(textView)
    }
}