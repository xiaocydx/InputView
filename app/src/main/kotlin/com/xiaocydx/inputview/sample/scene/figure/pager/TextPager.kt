package com.xiaocydx.inputview.sample.scene.figure.pager

import android.widget.TextView
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.scene.figure.FigureSnapshot
import com.xiaocydx.inputview.sample.scene.figure.FigureState
import com.xiaocydx.inputview.sample.scene.figure.overlay.FigureEditor
import com.xiaocydx.inputview.sample.scene.figure.overlay.FigureEditor.Ime
import java.lang.ref.WeakReference

/**
 * @author xcc
 * @date 2024/4/14
 */
class TextPager(
    private val textView: TextView,
    private val showEditor: (FigureEditor?) -> Unit
) {
    private var text: String? = null

    init {
        textView.onClick { showEditor(Ime) }
    }

    fun updateCurrentPage(state: FigureState) {
        if (state.currentText != text) {
            text = state.currentText
            textView.text = if (text.isNullOrEmpty()) "输入文字" else text
        }
    }

    fun snapshot(): FigureSnapshot {
        return FigureSnapshot(textView = WeakReference(textView))
    }
}