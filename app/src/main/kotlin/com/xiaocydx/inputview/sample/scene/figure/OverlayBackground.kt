package com.xiaocydx.inputview.sample.scene.figure

import android.graphics.drawable.ColorDrawable
import androidx.annotation.ColorInt
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.transform.ImperfectState
import com.xiaocydx.inputview.transform.TransformState
import com.xiaocydx.inputview.transform.Transformer

/**
 * @author xcc
 * @date 2024/8/15
 */
class OverlayBackground(
    @ColorInt color: Int,
    private val goScene: (FigureScene?) -> Unit,
) : Transformer() {
    private val background = ColorDrawable(color)

    override fun match(state: ImperfectState) = true

    override fun onPrepare(state: ImperfectState) = with(state) {
        backgroundView.background = background
        backgroundView.onClick { goScene(null) }
    }

    override fun onUpdate(state: TransformState) = with(state) {
        val alphaFraction = when {
            previous != null && current != null -> 1f
            current != null -> interpolatedFraction
            else -> 1f - interpolatedFraction
        }
        background.mutate().alpha = (255 * alphaFraction).toInt()
    }
}