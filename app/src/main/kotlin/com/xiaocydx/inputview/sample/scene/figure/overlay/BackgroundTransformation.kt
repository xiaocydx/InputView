package com.xiaocydx.inputview.sample.scene.figure.overlay

import android.graphics.drawable.ColorDrawable
import androidx.core.view.isVisible
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformation

/**
 * @author xcc
 * @date 2024/4/13
 */
class BackgroundTransformation(
    private val showEditor: (FigureEditor?) -> Unit
) : OverlayTransformation<FigureSnapshotState> {
    private val background = ColorDrawable(0xFF111113.toInt())

    override fun prepare(state: FigureSnapshotState) = with(state) {
        container.isVisible = !state.snapshot.isEmpty
        container.onClick { showEditor(null) }
    }

    override fun start(state: FigureSnapshotState) {
        state.container.background = background
    }

    override fun update(state: FigureSnapshotState) = with(state) {
        val alphaFraction = when {
            state.previous != null && state.current != null -> 1f
            state.current != null -> interpolatedFraction
            else -> 1f - interpolatedFraction
        }
        background.mutate().alpha = (255 * alphaFraction).toInt()
    }

    override fun end(state: FigureSnapshotState) = with(state) {
        if (state.current == null) {
            container.background = null
            container.isVisible = false
            container.setOnClickListener(null)
        }
    }
}