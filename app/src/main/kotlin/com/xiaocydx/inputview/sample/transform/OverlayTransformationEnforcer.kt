@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package com.xiaocydx.inputview.sample.transform

import android.os.Looper
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import com.xiaocydx.inputview.AnimationCallback
import com.xiaocydx.inputview.AnimationState
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.EditorAdapter
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.notifyHideCurrent
import com.xiaocydx.inputview.notifyShow
import com.xiaocydx.inputview.sample.transform.OverlayTransformation.EnforcerScope
import com.xiaocydx.inputview.sample.transform.OverlayTransformation.State
import com.xiaocydx.inputview.sample.transform.OverlayTransformation.StateProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * @author xcc
 * @date 2024/4/11
 */
class OverlayTransformationEnforcer<T : Editor, S : State>(
    private val owner: LifecycleOwner,
    private val editorAnimator: FadeEditorAnimator,
    private val editorAdapter: EditorAdapter<T>,
    private val stateProvider: StateProvider<S>
) {
    private var state: S? = null
    private var isAttached = false
    private val transformations = mutableListOf<OverlayTransformation<S>>()
    private val enforceScope = EnforcerScopeImpl(owner.lifecycle)
    private val animationCallback = AnimationCallbackImpl()

    fun add(transformation: OverlayTransformation<S>) = apply {
        require(!isAttached) { "在attach()之前完成添加" }
        if (!transformations.contains(transformation)) {
            transformations.add(transformation)
        }
    }

    fun attach(dispatcher: OnBackPressedDispatcher? = null) {
        if (isAttached) return
        isAttached = true
        editorAnimator.addAnimationCallback(animationCallback)
        dispatcher?.let(::addToOnBackPressedDispatcher)
    }

    fun notify(editor: T?) {
        if (editor == null) {
            editorAdapter.notifyHideCurrent()
        } else {
            editorAdapter.notifyShow(editor)
        }
    }

    private fun addToOnBackPressedDispatcher(dispatcher: OnBackPressedDispatcher) {
        val callback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() = notify(null)
        }
        editorAdapter.addEditorChangedListener { _, current ->
            callback.isEnabled = current != null
        }
        dispatcher.addCallback(owner, callback)
    }

    private inline fun dispatchTransformation(action: OverlayTransformation<S>.() -> Unit) {
        for (i in transformations.indices) transformations[i].action()
    }

    @Suppress("UNCHECKED_CAST")
    private fun dispatch(state: State) {
        require(state === this.state)
        state as S
        dispatchTransformation { prepare(state) }
        dispatchTransformation { start(state) }
        dispatchTransformation { update(state) }
        dispatchTransformation { end(state) }
    }

    private inner class EnforcerScopeImpl(lifecycle: Lifecycle) : EnforcerScope {
        private val parent = lifecycle.coroutineScope.coroutineContext.job
        override val coroutineContext = SupervisorJob(parent) + Dispatchers.Main.immediate
        override fun requestDispatch(state: State) = dispatch(state)
    }

    private inner class AnimationCallbackImpl : AnimationCallback {

        override fun onAnimationPrepare(previous: Editor?, current: Editor?) {
            enforceScope.coroutineContext.cancelChildren()
            state = stateProvider.createState().apply {
                val params = inputView.layoutParams
                require(params.width == MATCH_PARENT)
                require(params.height == MATCH_PARENT)
            }
            state!!.setEditor(previous, current)
            dispatchTransformation { prepare(state!!) }
        }

        override fun onAnimationStart(animation: AnimationState) {
            val state = state ?: return
            // TODO: 补充坐标换算
            val initial = state.inputView.bottom
            val start = initial - animation.startOffset
            val end = initial - animation.endOffset
            state.setAnchorY(initial, start, end)
            dispatchTransformation { start(state) }
        }

        override fun onAnimationUpdate(animation: AnimationState) {
            val state = state ?: return
            if (animation.previous == null || animation.current == null) {
                animation.startView?.alpha = 1f
                animation.endView?.alpha = 1f
            }
            state.setViewAlpha(
                start = editorAnimator.calculateAlpha(animation, start = true),
                end = editorAnimator.calculateAlpha(animation, start = false)
            )
            state.setInterpolatedFraction(animation.interpolatedFraction)
            dispatchTransformation { update(state) }
        }

        override fun onAnimationEnd(animation: AnimationState) {
            val state = state ?: return
            dispatchTransformation { end(state) }
            if (state.current != null) {
                enforceScope.launch { collectAnchorYChange(state) }
            }
            dispatchTransformation { launch(state, enforceScope) }
        }

        private suspend fun collectAnchorYChange(state: S) = with(state) {
            suspendCancellableCoroutine<Unit> { cont ->
                var previousAnchorY = endAnchorY
                val listener = ViewTreeObserver.OnDrawListener {
                    val currentAnchorY = initialAnchorY - inputView.editorOffset
                    if (currentAnchorY != previousAnchorY) {
                        state.setEditor(current, current)
                        state.setAnchorY(initialAnchorY, currentAnchorY, currentAnchorY)
                        dispatch(state)
                        previousAnchorY = currentAnchorY
                    }
                }
                inputView.viewTreeObserver.addOnDrawListener(listener)
                cont.invokeOnCancellation {
                    assert(Thread.currentThread() === Looper.getMainLooper().thread)
                    inputView.viewTreeObserver.removeOnDrawListener(listener)
                }
            }
        }
    }
}