package com.xiaocydx.inputview.sample.scene.video.transformer

import android.animation.RectEvaluator
import android.graphics.Rect
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.transition.getBounds
import androidx.transition.setLeftTopRightBottomCompat
import com.xiaocydx.inputview.transform.Content
import com.xiaocydx.inputview.transform.PrepareState
import com.xiaocydx.inputview.transform.TransformState
import com.xiaocydx.inputview.transform.Transformer
import com.xiaocydx.inputview.transform.view

/**
 * @author xcc
 * @date 2024/7/24
 */
class ContentBoundsChange(private val content: Content) : Transformer {
    private val startBounds = Rect()
    private val endBounds = Rect()
    private val currentBounds = Rect()
    private val boundsEvaluator = RectEvaluator(currentBounds)
    private var canTransform = false

    override fun match(state: PrepareState): Boolean {
        val startView = state.startViews.content?.takeIf { it.layoutParams?.height != MATCH_PARENT }
        val endView = state.endViews.content?.takeIf { it.layoutParams?.height != MATCH_PARENT }
        return startView != null && endView != null && state.view(content) != endView
    }

    override fun onStart(state: TransformState) {
        state.startViews.content?.getBounds(startBounds)
        state.endViews.content?.getBounds(endBounds)
        canTransform = startBounds != endBounds
    }

    override fun onUpdate(state: TransformState) {
        if (!canTransform) return
        boundsEvaluator.evaluate(state.interpolatedFraction, startBounds, endBounds)
        state.view(content)?.setLeftTopRightBottomCompat(currentBounds)
    }
}