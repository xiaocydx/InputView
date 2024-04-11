@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package com.xiaocydx.inputview.sample.edit.transform

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
import com.xiaocydx.inputview.sample.edit.transform.Transformation.EnforcerScope
import com.xiaocydx.inputview.sample.edit.transform.Transformation.State
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
class TransformationEnforcer<T : Editor, S : State>(
    private val owner: LifecycleOwner,
    private val editorAnimator: FadeEditorAnimator,
    private val editorAdapter: EditorAdapter<T>,
    private val createState: () -> S
) : AnimationCallback {
    private var state: S? = null
    private var transformation: Transformation<S>? = null
    private val enforceScope = EnforcerScopeImpl(owner.lifecycle)

    fun attach(transformation: Transformation<S>) {
        this.transformation = transformation
        editorAnimator.addAnimationCallback(this)
    }

    fun addToOnBackPressedDispatcher(dispatcher: OnBackPressedDispatcher) {
        val callback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() = notify(null)
        }
        editorAdapter.addEditorChangedListener { _, current ->
            callback.isEnabled = current != null
        }
        dispatcher.addCallback(owner, callback)
    }

    fun notify(editor: T?) {
        if (editor == null) {
            editorAdapter.notifyHideCurrent()
        } else {
            editorAdapter.notifyShow(editor)
        }
    }

    override fun onAnimationPrepare(previous: Editor?, current: Editor?) {
        enforceScope.coroutineContext.cancelChildren()
        state = createState().apply {
            val params = inputView.layoutParams
            require(params.width == MATCH_PARENT)
            require(params.height == MATCH_PARENT)
            require(container.parent === inputView)
        }
        state!!.setEditor(previous, current)
        transformation?.prepare(state!!)
    }

    override fun onAnimationStart(animation: AnimationState) {
        val state = state ?: return
        val initial = state.inputView.bottom
        val start = initial - animation.startOffset
        val end = initial - animation.endOffset
        state.setAnchorY(initial, start, end)
        transformation?.start(state)
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
        transformation?.update(state)
    }

    override fun onAnimationEnd(animation: AnimationState) {
        val state = state ?: return
        transformation?.end(state)
        if (state.current != null) {
            enforceScope.launch { collectAnchorYChange(state) }
        }
        transformation?.launch(state, enforceScope)
    }

    private suspend fun collectAnchorYChange(state: S) = with(state) {
        // TODO: 尝试简化
        suspendCancellableCoroutine<Unit> { cont ->
            var old = container.bottom
            val listener = ViewTreeObserver.OnDrawListener {
                val new = container.bottom
                if (new != old) {
                    state.setEditor(current, current)
                    state.setAnchorY(initialAnchorY, new, new)
                    dispatch(state)
                    old = new
                }
            }
            container.viewTreeObserver.addOnDrawListener(listener)
            cont.invokeOnCancellation {
                assert(Thread.currentThread() === Looper.getMainLooper().thread)
                container.viewTreeObserver.removeOnDrawListener(listener)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun dispatch(state: State) {
        require(state === this.state)
        state as S
        transformation?.prepare(state)
        transformation?.start(state)
        transformation?.update(state)
        transformation?.end(state)
    }

    private inner class EnforcerScopeImpl(lifecycle: Lifecycle) : EnforcerScope {
        private val parent = lifecycle.coroutineScope.coroutineContext.job
        override val coroutineContext = SupervisorJob(parent) + Dispatchers.Main.immediate
        override fun requestDispatch(state: State) = dispatch(state)
    }
}