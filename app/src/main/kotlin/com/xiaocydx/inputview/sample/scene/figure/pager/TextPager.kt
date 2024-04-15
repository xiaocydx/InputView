package com.xiaocydx.inputview.sample.scene.figure.pager

import android.widget.TextView
import androidx.core.view.isInvisible
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.scene.figure.FigureSnapshot
import com.xiaocydx.inputview.sample.scene.figure.FigureState
import com.xiaocydx.inputview.sample.scene.figure.ViewBounds
import com.xiaocydx.inputview.sample.scene.figure.overlay.FigureEditor
import com.xiaocydx.inputview.sample.scene.figure.overlay.FigureEditor.INPUT

/**
 * @author xcc
 * @date 2024/4/14
 */
class TextPager(
    private val textView: TextView,
    private val showEditor: (FigureEditor?) -> Unit
) {
    private var text: String? = null
    private var isInVisible: Boolean? = null

    init {
        textView.onClick { showEditor(INPUT) }
    }

    fun updateCurrentPage(state: FigureState) {
        if (state.currentText != text) {
            text = state.currentText
            textView.text = if (text.isNullOrEmpty()) "输入文字" else text
        }
        if (state.pageInvisible.text != isInVisible) {
            isInVisible = state.pageInvisible.text
            textView.isInvisible = isInVisible!!
        }
    }

    fun snapshot(): FigureSnapshot {
        return FigureSnapshot(textBounds = ViewBounds.from(textView))
    }
}