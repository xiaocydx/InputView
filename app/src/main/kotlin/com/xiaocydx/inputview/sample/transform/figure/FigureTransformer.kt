package com.xiaocydx.inputview.sample.transform.figure

import android.graphics.drawable.ColorDrawable
import android.view.Window
import androidx.annotation.ColorInt
import com.xiaocydx.inputview.sample.common.isDispatchTouchEventEnabled
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.transform.ImperfectState
import com.xiaocydx.inputview.transform.Overlay
import com.xiaocydx.inputview.transform.TransformState
import com.xiaocydx.inputview.transform.Transformer

/**
 * [Overlay]的整体背景
 */
class FigureSceneBackground(@ColorInt color: Int) : Transformer() {
    private val background = ColorDrawable(color)

    override fun match(state: ImperfectState) = true

    override fun onPrepare(state: ImperfectState) = with(state) {
        backgroundView.background = background
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

/**
 * [Overlay]调度[Transformer]期间拦截触摸事件，点击`backgroundView`退出当前[FigureScene]
 */
class FigureSceneDispatchTouch(
    private val window: Window,
    private val go: (FigureScene?) -> Unit,
) : Transformer() {
    override fun match(state: ImperfectState) = true

    override fun onPrepare(state: ImperfectState) {
        state.backgroundView.onClick { go(null) }
    }

    override fun onStart(state: TransformState) {
        window.isDispatchTouchEventEnabled = false
    }

    override fun onEnd(state: TransformState) {
        window.isDispatchTouchEventEnabled = true
    }
}