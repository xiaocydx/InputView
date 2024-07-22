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

package com.xiaocydx.inputview.overlay

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.xiaocydx.inputview.AnimationCallback
import com.xiaocydx.inputview.AnimationState
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.EditorAdapter
import com.xiaocydx.inputview.EditorMode
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.disableGestureNavBarOffset
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.initCompat
import com.xiaocydx.inputview.notifyHideCurrent
import com.xiaocydx.inputview.notifyShow

/**
 * @author xcc
 * @date 2024/7/22
 */
class OverlayEnforcer<C : Content, E : Editor>(
    private val window: Window,
    private val lifecycleOwner: LifecycleOwner,
    private val contentAdapter: ContentAdapter<C>,
    private val editorAdapter: EditorAdapter<E>,
) {
    private val inputView = InputView(window.decorView.context)
    private val contentView = ContentContainer(window.decorView.context)
    private val contentHost = ContentHostImpl()
    private val transformers = mutableListOf<OverlayTransformer>()
    private val transformState = TransformStateImpl()

    fun requestTransform() {
        if (transformState.isDispatching) return
        // TODO: 检验state的有效性，处理重复分发
        dispatchTransformer { onPrepare(transformState) }
        dispatchTransformer { onStart(transformState) }
        dispatchTransformer { onUpdate(transformState) }
        dispatchTransformer { onEnd(transformState) }
    }

    fun addTransformer(transformer: OverlayTransformer) {
        if (transformers.contains(transformer)) return
        // TODO: 增加排序处理？
        // TODO: Fragment.view创建阶段添加，需要追上调度
        transformers.add(transformer)
    }

    fun removeTransformer(transformer: OverlayTransformer) {
        transformers.remove(transformer)
    }

    fun notify(scene: OverlayScene<C, E>?) {
        val current = transformState.current
        if (current?.content === scene?.content
                && current?.editor === scene?.editor) {
            return
        }
        transformState.previous = current
        transformState.current = scene
        if (scene == null) {
            editorAdapter.notifyHideCurrent()
        } else {
            editorAdapter.notifyShow(scene.editor)
        }
    }

    fun attachToWindow(
        compat: Boolean = false,
        parent: ViewGroup = window.findViewById(android.R.id.content),
        inputViewInitializer: ((inputView: InputView) -> Unit)? = null
    ): Boolean {
        val statusBarEdgeToEdge = true
        val gestureNavBarEdgeToEdge = true
        val first = if (!compat) {
            InputView.init(window, statusBarEdgeToEdge, gestureNavBarEdgeToEdge)
        } else {
            InputView.initCompat(window, gestureNavBarEdgeToEdge)
        }
        if (!first) return false

        inputViewInitializer?.invoke(inputView)
        inputView.disableGestureNavBarOffset()
        inputView.editorMode = EditorMode.ADJUST_PAN
        inputView.editorAdapter = editorAdapter
        inputView.editorAnimator.addAnimationCallback(AnimationCallbackImpl())
        contentAdapter.onAttachedToHost(contentHost)

        val root = FrameLayout(window.decorView.context)
        root.addView(contentView, MATCH_PARENT, MATCH_PARENT)
        root.addView(inputView, MATCH_PARENT, MATCH_PARENT)
        if (parent.id != android.R.id.content) {
            parent.addView(root)
        } else {
            lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (!source.lifecycle.currentState.isAtLeast(CREATED)) return
                    source.lifecycle.removeObserver(this)
                    val finalParent = window.findViewById<ViewGroup>(android.R.id.content)
                    finalParent.addView(root)
                }
            })
        }
        return true
    }

    private inline fun dispatchTransformer(action: OverlayTransformer.() -> Unit) {
        for (i in transformers.indices.reversed()) transformers[i].action()
    }

    private inner class ContentHostImpl : ContentHost {
        override val current: Content?
            get() = transformState.current?.content
        override val container = contentView

        override fun addTransformer(transformer: OverlayTransformer) {
            this@OverlayEnforcer.addTransformer(transformer)
        }

        override fun removeTransformer(transformer: OverlayTransformer) {
            this@OverlayEnforcer.removeTransformer(transformer)
        }
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private inner class AnimationCallbackImpl : AnimationCallback {
        private val point = IntArray(2)

        override fun onAnimationPrepare(previous: Editor?, current: Editor?) {
            assert(transformState.previous?.editor === previous)
            assert(transformState.current?.editor === current)
            transformState.isDispatching = true
            dispatchTransformer { onPrepare(transformState) }
        }

        override fun onAnimationStart(animation: AnimationState) {
            transformState.inputView.getLocationInWindow(point)
            val initial = point[1] + transformState.inputView.height
            // TODO: 支持修改anchorY
            transformState.initialAnchorY = initial
            transformState.startAnchorY = initial - animation.startOffset
            transformState.endAnchorY = initial - animation.endOffset
            dispatchTransformer { onStart(transformState) }
        }

        override fun onAnimationUpdate(animation: AnimationState) {
            if (animation.previous == null || animation.current == null) {
                animation.startView?.alpha = 1f
                animation.endView?.alpha = 1f
            }
            // TODO: 设置alpha
            transformState.animatedFraction = animation.animatedFraction
            transformState.interpolatedFraction = animation.interpolatedFraction
            dispatchTransformer { onUpdate(transformState) }
        }

        override fun onAnimationEnd(animation: AnimationState) {
            dispatchTransformer { onEnd(transformState) }
            transformState.isDispatching = false
        }
    }

    private inner class TransformStateImpl : TransformState {
        override val inputView = this@OverlayEnforcer.inputView
        override val container = this@OverlayEnforcer.contentView
        override var previous: OverlayScene<C, E>? = null
        override var current: OverlayScene<C, E>? = null
        override var initialAnchorY = 0
        override var startAnchorY = 0
        override var endAnchorY = 0
        override var currentAnchorY = 0
        override var animatedFraction = 0f
        override var interpolatedFraction = 0f
        var isDispatching = false
    }
}