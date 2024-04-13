package com.xiaocydx.inputview.sample.scene.figure

import android.view.View
import androidx.annotation.CheckResult
import com.xiaocydx.inputview.sample.scene.figure.overlay.FigureEditor

data class Figure(
    val id: String,
    val coverUrl: String,
    val coverRatio: Float,
    val dubbingName: String
)

data class FigureSnapshot(
    val figure: Figure? = null,
    val figureBounds: FigureBounds? = null,
    val text: String? = null,
    val textBounds: FigureBounds? = null
) {
    @CheckResult
    fun merge(other: FigureSnapshot) = copy(
        figure = figure ?: other.figure,
        figureBounds = figureBounds ?: other.figureBounds,
        text = text ?: other.text,
        textBounds = textBounds ?: other.textBounds
    )
}

data class FigureBounds(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) {
    val width = right - left
    val height = bottom - top

    companion object {
        fun from(view: View) = run {
            val out = IntArray(2)
            view.getLocationOnScreen(out)
            FigureBounds(
                left = out[0],
                top = out[1],
                right = out[0] + view.width,
                bottom = out[1] + view.height
            )
        }
    }
}

data class PageInvisible(val figure: Boolean = false, val text: Boolean = false)

sealed class PendingTransform {
    data class Editor(val value: FigureEditor?, val request: Boolean) : PendingTransform()
    data class Begin(val snapshot: FigureSnapshot, val editor: FigureEditor?) : PendingTransform()
}