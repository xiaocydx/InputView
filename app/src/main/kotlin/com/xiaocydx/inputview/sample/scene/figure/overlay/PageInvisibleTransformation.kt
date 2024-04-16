package com.xiaocydx.inputview.sample.scene.figure.overlay

import com.xiaocydx.inputview.sample.scene.figure.PageInvisible
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformation

/**
 * @author xcc
 * @date 2024/4/16
 */
class PageInvisibleTransformation(
    private val getPageInvisible: () -> PageInvisible,
    private val setPageInvisible: (PageInvisible) -> Unit
) : OverlayTransformation<FigureSnapshotState> {

    override fun start(state: FigureSnapshotState) {
        setPageInvisible(start = true, state)
    }

    override fun end(state: FigureSnapshotState) {
        setPageInvisible(start = false, state)
    }

    private fun setPageInvisible(start: Boolean, state: FigureSnapshotState) = with(state) {
        if (snapshot.isEmpty) return
        if (previous != null && current != null) return
        val pageInvisible = getPageInvisible()
        setPageInvisible(when (previous ?: current) {
            FigureEditor.INPUT, FigureEditor.EMOJI -> pageInvisible.copy(text = start)
            FigureEditor.GRID, FigureEditor.DUBBING -> pageInvisible.copy(figure = start)
            else -> return
        })
    }
}