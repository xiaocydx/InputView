package com.xiaocydx.inputview.sample.scene.video.transformer

import com.xiaocydx.inputview.overlay.Content
import com.xiaocydx.inputview.overlay.PrepareState
import com.xiaocydx.inputview.overlay.TransformState
import com.xiaocydx.inputview.overlay.Transformer
import com.xiaocydx.inputview.overlay.alpha
import com.xiaocydx.inputview.overlay.view

/**
 * @author xcc
 * @date 2024/7/24
 */
class ContentFadeChange(
    private val content: Content,
    private val children: Boolean = false
): Transformer {

    override fun match(state: PrepareState): Boolean {
        val startView = state.startViews.content
        val endView = state.endViews.content
        return startView != null && endView != null && state.view(content) != null
    }

    override fun onUpdate(state: TransformState) {
        val view = state.view(content) ?: return
        val alpha = state.alpha(content)
        // if (!children) {
        //     view.alpha = alpha
        // } else if (view is ViewGroup) {
        //     for (i in 0 until view.childCount) {
        //         view.getChildAt(i).alpha = alpha
        //     }
        // }
        view.alpha = alpha
    }
}