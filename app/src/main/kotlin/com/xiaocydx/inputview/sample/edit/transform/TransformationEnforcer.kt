@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package com.xiaocydx.inputview.sample.edit.transform

import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.xiaocydx.inputview.AnimationCallback
import com.xiaocydx.inputview.AnimationState
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.notifyHideCurrent
import com.xiaocydx.inputview.notifyShow
import com.xiaocydx.inputview.sample.edit.VideoEditor
import com.xiaocydx.inputview.sample.edit.VideoEditorAdapter
import com.xiaocydx.inputview.sample.edit.transform.Transformation.State
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * @author xcc
 * @date 2024/4/10
 */
class TransformationEnforcer(
    private val lifecycle: Lifecycle,
    private val inputView: InputView,
    private val container: ViewGroup
) : AnimationCallback {
    private val animator = requireNotNull(inputView.editorAnimator as? FadeEditorAnimator)
    private val adapter = requireNotNull(inputView.editorAdapter as? VideoEditorAdapter)
    private var state: State? = null
    private var translationY = 0
    private var animationEndJob: Job? = null
    private var previous: VideoEditor? = null
    private var canBegin = true
    private var transformation: Transformation? = null

    fun attach(transformation: Transformation) = apply {
        this.transformation = transformation
        animator.addAnimationCallback(this)
        adapter.addEditorChangedListener { _, current -> begin(current) }
    }

    @Suppress("INVISIBLE_MEMBER")
    fun begin(current: VideoEditor?) {
        if (!canBegin) return
        animator.endAnimation()
        animationEndJob?.cancel()
        state = State(inputView, container, previous, current)
        previous = current
        transformation?.attach(state!!)
        canBegin = false
        if (current == null) adapter.notifyHideCurrent() else adapter.notifyShow(current)
        canBegin = true
    }

    override fun onAnimationStart(animation: AnimationState) {
        val state = state ?: return
        var start = container.top
        var end = start - (animation.endOffset - animation.startOffset)
        when {
            animation.startOffset == 0 -> {
                translationY = container.height
                start += translationY
            }
            animation.endOffset == 0 -> {
                translationY = container.height
                end += translationY
            }
            else -> translationY = 0
        }
        inputView.translationY = translationY.toFloat()
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
            inputView.translationY = translationY * fraction
        }
        transformation?.update(state)
    }

    override fun onAnimationEnd(animation: AnimationState) {
        val state = state ?: return
        transformation?.end(state)
        val handler = CoroutineExceptionHandler { _, _ -> }
        animationEndJob = lifecycle.coroutineScope.launch(handler) {
            transformation?.launch(state, this)
        }
    }
}