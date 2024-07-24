package com.xiaocydx.inputview.sample.scene.video.transformer

import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.transition.updateLayoutGravity
import com.xiaocydx.inputview.overlay.Content
import com.xiaocydx.inputview.overlay.PrepareState
import com.xiaocydx.inputview.overlay.TransformState
import com.xiaocydx.inputview.overlay.Transformer
import com.xiaocydx.inputview.overlay.view

/**
 * @author xcc
 * @date 2024/7/24
 */
class ContentTranslation(private val content: Content) : Transformer {

    override fun match(state: PrepareState): Boolean {
        val view = state.view(content)
        return view != null && view.layoutParams?.height != MATCH_PARENT
    }

    override fun onPrepare(state: PrepareState) {
        state.view(content)?.updateLayoutGravity(Gravity.BOTTOM)
    }

    override fun onUpdate(state: TransformState) {
        if (state.previous == null || state.current == null) {
            val fraction = when (state.previous) {
                null -> 1f - state.interpolatedFraction
                else -> state.interpolatedFraction
            }
            val matchHeight = state.view(content)?.height ?: 0
            val offset = (matchHeight * fraction).toInt()
            state.rootView.translationY = offset.toFloat()
        }
        state.view(content)?.translationY = -state.currentOffset.toFloat()
    }

    override fun onEnd(state: TransformState) {
        state.rootView.translationY = 0f
    }
}