@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package com.xiaocydx.inputview.sample.edit.transform

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.xiaocydx.inputview.AnimationCallback
import com.xiaocydx.inputview.AnimationState
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.EditorAdapter
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.notifyHideCurrent
import com.xiaocydx.inputview.notifyShow
import com.xiaocydx.inputview.sample.edit.transform.Transformation.State
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * @author xcc
 * @date 2024/4/11
 */
class TransformationEnforcer<T : Editor, S : State>(
    private val lifecycle: Lifecycle,
    private val animator: FadeEditorAnimator,
    private val adapter: EditorAdapter<T>,
    private val createState: () -> S
) : AnimationCallback {
    private var state: S? = null
    private var translationY = 0
    private var animationEndJob: Job? = null
    private var transformation: Transformation<S>? = null
    private val handler = CoroutineExceptionHandler { _, _ -> }

    fun attach(transformation: Transformation<S>) {
        this.transformation = transformation
        animator.addAnimationCallback(this)
    }

    fun notify(editor: T?) {
        if (editor == null) {
            adapter.notifyHideCurrent()
        } else {
            adapter.notifyShow(editor)
        }
    }

    override fun onAnimationPrepare(previous: Editor?, current: Editor?) {
        animationEndJob?.cancel()
        state = createState()
        state!!.setEditor(previous, current)
        transformation?.prepare(state!!)
    }

    override fun onAnimationStart(animation: AnimationState) {
        val state = state ?: return
        var start = state.container.top
        var end = start - (animation.endOffset - animation.startOffset)
        when {
            animation.startOffset == 0 -> {
                translationY = state.container.height
                start += translationY
            }
            animation.endOffset == 0 -> {
                translationY = state.container.height
                end += translationY
            }
            else -> translationY = 0
        }
        state.inputView.translationY = translationY.toFloat()
        state.setAnchorY(start, end)
        transformation?.start(state)
    }

    override fun onAnimationUpdate(animation: AnimationState) {
        val state = state ?: return
        if (animation.previous == null || animation.current == null) {
            animation.startView?.alpha = 1f
            animation.endView?.alpha = 1f
        }
        state.setViewAlpha(
            start = animator.calculateAlpha(animation, start = true),
            end = animator.calculateAlpha(animation, start = false)
        )
        state.setInterpolatedFraction(animation.interpolatedFraction)
        if (translationY > 0f) {
            var fraction = state.interpolatedFraction
            if (animation.startOffset == 0) fraction = 1 - fraction
            state.inputView.translationY = translationY * fraction
        }
        transformation?.update(state)
    }

    override fun onAnimationEnd(animation: AnimationState) {
        val state = state ?: return
        transformation?.end(state)
        animationEndJob = lifecycle.coroutineScope.launch(handler) {
            transformation?.launch(state, this)
        }
    }
}