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
import com.xiaocydx.inputview.isVisible
import com.xiaocydx.inputview.notifyHideCurrent
import com.xiaocydx.inputview.notifyShow

fun <C : Content, E : Editor> InputView.Companion.createOverlay(
    window: Window,
    lifecycle: Lifecycle,
    contentAdapter: ContentAdapter<C>,
    editorAdapter: EditorAdapter<E>,
): Overlay<C, E> = Overlay(window, lifecycle, contentAdapter, editorAdapter)

class Overlay<C : Content, E : Editor> internal constructor(
    private val window: Window,
    private val lifecycle: Lifecycle,
    private val contentAdapter: ContentAdapter<C>,
    private val editorAdapter: EditorAdapter<E>,
) : TransformerOwner.Host {
    private val transformState = TransformStateImpl()
    private val transformers = mutableListOf<OverlayTransformer>()

    init {
        transformState.root.setTransformerOwner(this)
    }

    override fun hasTransformer(transformer: OverlayTransformer): Boolean {
        return transformers.contains(transformer)
    }

    override fun addTransformer(transformer: OverlayTransformer) {
        if (hasTransformer(transformer)) return
        transformers.add(transformer)
    }

    override fun removeTransformer(transformer: OverlayTransformer) {
        transformers.remove(transformer)
    }

    override fun requestTransform() {
        // TODO: 检验state的有效性，处理重复分发
        if (transformState.isDispatching) return
        transformState.isDispatching = true
        dispatchTransformer { onPrepare(transformState) }
        dispatchTransformer { onStart(transformState) }
        dispatchTransformer { onUpdate(transformState) }
        dispatchTransformer { onEnd(transformState) }
        transformState.isDispatching = false
    }

    fun notify(scene: OverlayScene<C, E>?) {
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

    fun attachToWindow(
        initCompat: Boolean,
        rootParent: ViewGroup = window.rootParent(),
        initializer: ((inputView: InputView) -> Unit)? = null
    ): Boolean = with(transformState) {
        val first = if (initCompat) {
            InputView.initCompat(window, gestureNavBarEdgeToEdge)
        } else {
            InputView.init(window, statusBarEdgeToEdge, gestureNavBarEdgeToEdge)
        }
        if (!first) return false

        initializer?.invoke(inputView)
        val editorAnimator = inputView.editorAnimator
        inputView.disableGestureNavBarOffset()
        inputView.editorMode = EditorMode.ADJUST_PAN
        inputView.editorAdapter = editorAdapter
        editorAnimator.addAnimationCallback(AnimationCallbackImpl())

        contentView.setAdapter(contentAdapter)
        contentView.setRemovePreviousImmediately(!editorAnimator.canRunAnimation)
        contentAdapter.onAttachedToHost(ContentHostImpl())

        if (rootParent.id == windowRootParentId()) {
            lifecycle.doOnCreated { window.rootParent().addView(root) }
        } else {
            rootParent.addView(root)
        }
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
                if (!source.lifecycle.currentState.isAtLeast(CREATED)) return
                source.lifecycle.removeObserver(this)
                action()
            }
        })
    }

    private inline fun dispatchTransformer(action: OverlayTransformer.() -> Unit) {
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

        override fun addTransformer(transformer: OverlayTransformer) {
            this@Overlay.addTransformer(transformer)
        }

        override fun removeTransformer(transformer: OverlayTransformer) {
            this@Overlay.removeTransformer(transformer)
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
        override val inputView = InputView(window.decorView.context)
        override val contentView = ContentContainer(window.decorView.context)
        override var previous: OverlayScene<C, E>? = null; private set
        override var current: OverlayScene<C, E>? = null; private set
        override var initialAnchorY = 0; private set
        override var startAnchorY = 0; private set
        override var endAnchorY = 0; private set
        override var currentAnchorY = 0; private set
        override var animatedFraction = 0f; private set
        override var interpolatedFraction = 0f; private set

        private val point = IntArray(2)
        val root = FrameLayout(window.decorView.context)
        var isDispatching = false

        init {
            root.isVisible = false
            root.addView(contentView, MATCH_PARENT, MATCH_PARENT)
            root.addView(inputView, MATCH_PARENT, MATCH_PARENT)
        }

        fun isSameScene(scene: OverlayScene<C, E>?): Boolean {
            return current?.content === scene?.content
                    && current?.editor === scene?.editor
        }

        fun setCurrentScene(scene: OverlayScene<C, E>?) {
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