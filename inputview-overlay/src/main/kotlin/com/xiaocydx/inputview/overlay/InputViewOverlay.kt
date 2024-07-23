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

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.xiaocydx.inputview.overlay

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
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
import com.xiaocydx.inputview.isVisible
import com.xiaocydx.inputview.notifyHideCurrent
import com.xiaocydx.inputview.notifyShow
import com.xiaocydx.inputview.overlay.Overlay.Scene

/**
 * @author xcc
 * @date 2024/7/23
 */
class InputViewOverlay<C : Content, E : Editor> internal constructor(
    private val window: Window,
    private val lifecycle: Lifecycle,
    private val contentAdapter: ContentAdapter<C>,
    private val editorAdapter: EditorAdapter<E>,
) : Overlay<C, E> {
    private val transformState = TransformStateImpl()
    private val transformers = mutableListOf<Transformer>()

    override fun hasTransformer(transformer: Transformer): Boolean {
        return transformers.contains(transformer)
    }

    override fun addTransformer(transformer: Transformer) {
        if (hasTransformer(transformer)) return
        transformers.add(transformer)
    }

    override fun removeTransformer(transformer: Transformer) {
        transformers.remove(transformer)
    }

    override fun requestTransform() {
        // TODO: 检验state的有效性，处理重复分发
        if (!transformState.isInitialized) return
        if (transformState.isDispatching) return
        transformState.isDispatching = true
        dispatchTransformer { onPrepare(transformState) }
        dispatchTransformer { onStart(transformState) }
        dispatchTransformer { onUpdate(transformState) }
        dispatchTransformer { onEnd(transformState) }
        transformState.isDispatching = false
    }

    override fun notify(scene: Scene<C, E>?) {
        if (!transformState.isInitialized) return
        if (transformState.isSameScene(scene)) return
        transformState.setCurrentScene(scene)
        if (scene == null) {
            contentAdapter.notifyHideCurrent()
            editorAdapter.notifyHideCurrent()
        } else {
            contentAdapter.notifyShow(scene.content)
            editorAdapter.notifyShow(scene.editor)
        }
    }

    override fun attachToWindow(
        initCompat: Boolean,
        rootParent: ViewGroup?,
        initializer: ((inputView: InputView) -> Unit)?
    ): Boolean = with(transformState) {
        val first = if (initCompat) {
            InputView.initCompat(window, gestureNavBarEdgeToEdge)
        } else {
            InputView.init(window, statusBarEdgeToEdge, gestureNavBarEdgeToEdge)
        }
        if (!first) return false
        attachToParent(rootParent)

        initializer?.invoke(inputView)
        val editorAnimator = inputView.editorAnimator
        inputView.disableGestureNavBarOffset()
        inputView.editorMode = EditorMode.ADJUST_PAN
        inputView.editorAdapter = editorAdapter
        editorAnimator.addAnimationCallback(AnimationCallbackImpl())

        contentView.setAdapter(contentAdapter)
        contentView.setRemovePreviousImmediately(!editorAnimator.canRunAnimation)
        contentAdapter.onAttachedToHost(ContentHostImpl())
        return true
    }

    private fun windowRootParentId(): Int {
        return android.R.id.content
    }

    private fun Window.rootParent(): ViewGroup {
        return findViewById(windowRootParentId())
    }

    private inline fun Lifecycle.doOnCreated(crossinline action: () -> Unit) {
        addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (!source.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) return
                source.lifecycle.removeObserver(this)
                action()
            }
        })
    }

    private inline fun dispatchTransformer(action: Transformer.() -> Unit) {
        // TODO: reversed待定
        for (i in transformers.indices.reversed()) transformers[i].action()
    }

    private inner class ContentHostImpl : ContentHost {
        override val current: Content?
            get() = transformState.contentView.current
        override val container: ViewGroup
            get() = transformState.contentView

        override fun showChecked(content: Content): Boolean {
            return transformState.contentView.showChecked(content)
        }

        override fun hideChecked(content: Content): Boolean {
            return transformState.contentView.hideChecked(content)
        }

        override fun addTransformer(transformer: Transformer) {
            this@InputViewOverlay.addTransformer(transformer)
        }

        override fun removeTransformer(transformer: Transformer) {
            this@InputViewOverlay.removeTransformer(transformer)
        }
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private inner class AnimationCallbackImpl : AnimationCallback {

        override fun onAnimationPrepare(previous: Editor?, current: Editor?) {
            transformState.checkEditor(previous, current)
            transformState.contentView.consumePendingChange()
            transformState.root.isVisible = true
            transformState.isDispatching = true
            dispatchTransformer { onPrepare(transformState) }
        }

        override fun onAnimationStart(animation: AnimationState) {
            // TODO: 支持修改anchorY
            transformState.setAnchorY(animation)
            dispatchTransformer { onStart(transformState) }
        }

        override fun onAnimationUpdate(animation: AnimationState) {
            // TODO: 设置alpha
            transformState.setAlpha(animation)
            transformState.setFraction(animation)
            dispatchTransformer { onUpdate(transformState) }
        }

        override fun onAnimationEnd(animation: AnimationState) {
            dispatchTransformer { onEnd(transformState) }
            transformState.contentView.removeChangeRecordPrevious()
            transformState.root.isVisible = transformState.current != null
            transformState.isDispatching = false
        }
    }

    private inner class TransformStateImpl : TransformState {
        lateinit var root: FrameLayout; private set
        override lateinit var inputView: InputView; private set
        override lateinit var contentView: ContentContainer; private set
        override var previous: Scene<C, E>? = null; private set
        override var current: Scene<C, E>? = null; private set
        override var initialAnchorY = 0; private set
        override var startAnchorY = 0; private set
        override var endAnchorY = 0; private set
        override var currentAnchorY = 0; private set
        override var animatedFraction = 0f; private set
        override var interpolatedFraction = 0f; private set

        private val point = IntArray(2)
        var isInitialized = false; private set
        var isDispatching = false

        fun attachToParent(rootParent: ViewGroup?) {
            isInitialized = true
            val context = window.rootParent().context
            root = FrameLayout(context)
            inputView = InputView(context)
            contentView = ContentContainer(context)
            root.isVisible = false
            root.addView(contentView, MATCH_PARENT, MATCH_PARENT)
            root.addView(inputView, MATCH_PARENT, MATCH_PARENT)
            root.setTransformerHost(this@InputViewOverlay)
            if (rootParent == null || rootParent.id == windowRootParentId()) {
                lifecycle.doOnCreated { window.rootParent().addView(root) }
            } else {
                rootParent.addView(root)
            }
        }

        fun isSameScene(scene: Scene<C, E>?): Boolean {
            return current?.content === scene?.content
                    && current?.editor === scene?.editor
        }

        fun setCurrentScene(scene: Scene<C, E>?) {
            previous = current
            current = scene
        }

        fun checkEditor(previous: Editor?, current: Editor?) {
            check(this.previous?.editor === previous) { "previous.editor不一致" }
            check(this.current?.editor === current) { "current.editor不一致" }
        }

        fun setAnchorY(animation: AnimationState) {
            inputView.getLocationInWindow(point)
            val initial = point[1] + inputView.height
            initialAnchorY = initial
            startAnchorY = initial - animation.startOffset
            endAnchorY = initial - animation.endOffset
        }

        fun setAlpha(animation: AnimationState) {

        }

        fun setFraction(animation: AnimationState) {
            animatedFraction = animation.animatedFraction
            interpolatedFraction = animation.interpolatedFraction
            currentAnchorY = startAnchorY + ((endAnchorY - startAnchorY) * interpolatedFraction).toInt()
        }
    }

    private companion object {
        const val statusBarEdgeToEdge = true
        const val gestureNavBarEdgeToEdge = true
    }
}