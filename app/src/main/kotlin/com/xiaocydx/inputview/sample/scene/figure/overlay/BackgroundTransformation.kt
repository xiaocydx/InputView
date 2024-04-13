package com.xiaocydx.inputview.sample.scene.figure.overlay

import android.graphics.drawable.ColorDrawable
import androidx.core.view.isVisible
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformation
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformation.ContainerState

/**
 * @author xcc
 * @date 2024/4/13
 */
class BackgroundTransformation(
    private val submit: (FigureEditor?) -> Unit
) : OverlayTransformation<ContainerState> {
    private val background = ColorDrawable(0xFF111113.toInt())

    override fun prepare(state: ContainerState) = with(state) {
        container.isVisible = true
        container.onClick { submit(null) }
    }

    override fun start(state: ContainerState) {
        state.container.background = background
    }

    override fun update(state: ContainerState) = with(state) {
        val alphaFraction = when {
            state.previous != null && state.current != null -> 1f
            state.current != null -> interpolatedFraction
            else -> 1f - interpolatedFraction
        }
        background.mutate().alpha = (255 * alphaFraction).toInt()
    }

    override fun end(state: ContainerState) = with(state) {
        if (state.current == null) {
            container.background = null
            container.isVisible = false
            container.setOnClickListener(null)
        }
    }
}