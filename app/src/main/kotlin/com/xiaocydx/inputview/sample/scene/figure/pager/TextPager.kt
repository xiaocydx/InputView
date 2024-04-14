package com.xiaocydx.inputview.sample.scene.figure.pager

import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.lifecycle.Lifecycle
import com.xiaocydx.inputview.sample.common.launchRepeatOnLifecycle
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.scene.figure.ViewBounds
import com.xiaocydx.inputview.sample.scene.figure.FigureSnapshot
import com.xiaocydx.inputview.sample.scene.figure.FigureState
import com.xiaocydx.inputview.sample.scene.figure.overlay.FigureEditor
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach

/**
 * @author xcc
 * @date 2024/4/14
 */
class TextPager(
    private val lifecycle: Lifecycle,
    private val textView: TextView,
    private val showEditor: (FigureEditor?) -> Unit,
    private val figureState: StateFlow<FigureState>
) {

    fun init() = apply {
        textView.onClick { showEditor(FigureEditor.INPUT) }
        var text: String? = null
        var isInVisible: Boolean? = null
        figureState.onEach {
            if (it.currentText != text) {
                text = it.currentText
                textView.text = if (text.isNullOrEmpty()) "输入文字" else text
            }
            if (it.pageInvisible.text != isInVisible) {
                isInVisible = it.pageInvisible.text
                textView.isInvisible = isInVisible!!
            }
        }.launchRepeatOnLifecycle(lifecycle)
    }

    fun snapshot(): FigureSnapshot {
        return FigureSnapshot(textBounds = ViewBounds.from(textView))
    }
}