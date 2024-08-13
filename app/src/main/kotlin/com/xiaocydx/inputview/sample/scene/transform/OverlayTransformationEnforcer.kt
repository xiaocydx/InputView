/*
 * Copyright 2023 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package com.xiaocydx.inputview.sample.scene.transform

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
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.current
import com.xiaocydx.inputview.disableGestureNavBarOffset
import com.xiaocydx.inputview.notifyHideCurrent
import com.xiaocydx.inputview.notifyShow
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformation.EnforcerScope
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformation.State
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformation.StateProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

/**
 * [OverlayTransformation]的执行器
 *
 * @author xcc
 * @date 2024/4/11
 */
@Deprecated(
    message = "实现类的职责不够清晰，调度流程不够完善",
    replaceWith = ReplaceWith("待替换为InputView.createOverlay()")
)
class OverlayTransformationEnforcer<T : Editor, S : State>(
    private val lifecycleOwner: LifecycleOwner,
    private val editorAnimator: FadeEditorAnimator,
    private val editorAdapter: EditorAdapter<T>,
    private val stateProvider: StateProvider<S>
) {
    private var isAttached = false
    private var dispatchingState: S? = null
    private val transformations = mutableListOf<OverlayTransformation<S>>()
    private val enforcerScope = EnforcerScopeImpl(lifecycleOwner.lifecycle)
    private val animationCallback = AnimationCallbackImpl()

    /**
     * 添加[OverlayTransformation]，按添加的顺序执行各个函数，
     * 调用[attach]后再添加，会抛出[IllegalArgumentException]。
     */
    fun add(transformation: OverlayTransformation<S>) = apply {
        require(!isAttached) { "在attach()之前完成添加" }
        if (!transformations.contains(transformation)) {
            transformations.add(transformation)
        }
    }

    /**
     * 关联[inputView]，禁用手势导航栏偏移，覆盖层不做处理，
     * 若[dispatcher]不为`null`，则按返回键会隐藏[Editor]。
     */
    fun attach(inputView: InputView, dispatcher: OnBackPressedDispatcher? = null) {
        if (isAttached) return
        isAttached = true
        inputView.disableGestureNavBarOffset()
        editorAnimator.addAnimationCallback(animationCallback)
        dispatcher?.let(::addToOnBackPressedDispatcher)
    }

    /**
     * 若[editor]为`null`，则通知隐藏，否则通知显示
     */
    fun notify(editor: T?): T? {
        if (editor == null) {
            editorAdapter.notifyHideCurrent()
        } else {
            editorAdapter.notifyShow(editor)
        }
        return editorAdapter.current
    }

    private fun addToOnBackPressedDispatcher(dispatcher: OnBackPressedDispatcher) {
        val callback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                notify(editor = null)
            }
        }
        editorAdapter.addEditorChangedListener { _, current ->
            callback.isEnabled = current != null
        }
        dispatcher.addCallback(lifecycleOwner, callback)
    }

    private inline fun dispatchTransformation(action: OverlayTransformation<S>.() -> Unit) {
        for (i in transformations.indices) transformations[i].action()
    }

    private fun dispatch(state: State) {
        if (state !== dispatchingState) return
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
        private val point = IntArray(2)

        override fun onAnimationPrepare(previous: Editor?, current: Editor?) {
            enforcerScope.coroutineContext.cancelChildren()
            dispatchingState = stateProvider.createState().apply {
                val params = inputView.layoutParams
                require(params.width == MATCH_PARENT)
                require(params.height == MATCH_PARENT)
            }
            dispatchingState!!.setEditor(previous, current)
            dispatchTransformation { prepare(dispatchingState!!) }
        }

        override fun onAnimationStart(animation: AnimationState) {
            val state = dispatchingState ?: return
            state.inputView.getLocationInWindow(point)
            val initial = point[1] + state.inputView.height
            val start = initial - animation.startOffset
            val end = initial - animation.endOffset
            state.setAnchorY(initial, start, end)
            dispatchTransformation { start(state) }
        }

        override fun onAnimationUpdate(animation: AnimationState) {
            val state = dispatchingState ?: return
            if (animation.previous == null || animation.current == null) {
                animation.startView?.alpha = 1f
                animation.endView?.alpha = 1f
            }
            state.setViewAlpha(
                start = editorAnimator.calculateAlpha(animation, start = true),
                end = editorAnimator.calculateAlpha(animation, start = false)
            )
            state.setFraction(
                animated = animation.animatedFraction,
                interpolated = animation.interpolatedFraction
            )
            dispatchTransformation { update(state) }
        }

        override fun onAnimationEnd(animation: AnimationState) {
            val state = dispatchingState ?: return
            dispatchTransformation { end(state) }
            if (state.current != null) {
                enforcerScope.launch { collectAnchorYChange(state) }
            }
            dispatchTransformation { launch(state, enforcerScope) }
        }

        private suspend fun collectAnchorYChange(state: S): Unit = with(state) {
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
            try {
                inputView.viewTreeObserver.addOnDrawListener(listener)
                awaitCancellation()
            } finally {
                inputView.viewTreeObserver.removeOnDrawListener(listener)
            }
        }
    }
}