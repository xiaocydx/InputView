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

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "PackageDirectoryMismatch")

package com.xiaocydx.inputview.transform

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.Window
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.xiaocydx.inputview.AnimationCallback
import com.xiaocydx.inputview.AnimationState
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.EditorAdapter
import com.xiaocydx.inputview.EditorChangedListener
import com.xiaocydx.inputview.EditorMode
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.current
import com.xiaocydx.inputview.disableGestureNavBarOffset
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.initCompat
import com.xiaocydx.inputview.isVisible
import com.xiaocydx.inputview.notifyHideCurrent
import com.xiaocydx.inputview.notifyShow
import com.xiaocydx.inputview.transform.Overlay.Companion.ROOT_PARENT_ID

/**
 * @author xcc
 * @date 2024/7/23
 */
internal class OverlayImpl<C : Content, E : Editor>(
    override val lifecycleOwner: LifecycleOwner,
    private val contentAdapter: ContentAdapter<C>,
    private val editorAdapter: EditorAdapter<E>,
) : Overlay<C, E> {
    private val transformState = TransformStateImpl()
    private val transformerEnforcer = TransformerEnforcer()
    private var sceneChangedListener: SceneChangedListener<C, E>? = null
    private var sceneEditorConverter: SceneEditorConverter<C, E> = defaultEditorConverter()
    private var backPressedCallback: OnBackPressedCallback? = null
    private var isInitialized = false
    private var isActiveGoing = false

    override var previous: Scene<C, E>? = null; private set
    override var current: Scene<C, E>? = null; private set

    override fun attach(
        window: Window,
        initCompat: Boolean,
        rootParent: ViewGroup?,
        initializer: ((inputView: InputView) -> Unit)?
    ): Boolean {
        if (initCompat) {
            InputView.initCompat(window, gestureNavBarEdgeToEdge = true)
        } else {
            InputView.init(window, statusBarEdgeToEdge = true, gestureNavBarEdgeToEdge = true)
        }
        if (isInitialized) return false
        isInitialized = true

        transformState.apply {
            initialize(window)
            initializer?.invoke(inputView)
            val editorAnimator = inputView.editorAnimator
            inputView.disableGestureNavBarOffset()
            inputView.editorMode = EditorMode.ADJUST_PAN
            inputView.editorAdapter = editorAdapter
            editorAdapter.addEditorChangedListener(SceneEditorConverterCaller())
            editorAnimator.addAnimationCallback(transformerEnforcer)
            // TODO: rootView从window分离时，需要移除transformerEnforcer
            rootView.viewTreeObserver.addOnPreDrawListener(transformerEnforcer)

            contentView.setAdapter(contentAdapter)
            contentView.setRemovePreviousImmediately(!editorAnimator.canRunAnimation)
            contentAdapter.onAttachedToHost(ContentHostImpl())
        }

        transformState.rootView.setTransformerHost(this)
        if (rootParent != null && rootParent.id != ROOT_PARENT_ID) {
            rootParent.addView(transformState.rootView)
        } else {
            lifecycleOwner.lifecycle.doOnCreated {
                window.rootParent().addView(transformState.rootView)
            }
        }
        return true
    }

    override fun go(scene: Scene<C, E>?): Boolean {
        if (!isInitialized) return false
        isActiveGoing = true

        // editor的显示和隐藏允许被拦截
        val isSameEditor = editorAdapter.current === scene?.editor
        val editorChanged = if (scene == null) {
            editorAdapter.notifyHideCurrent()
        } else {
            editorAdapter.notifyShow(scene.editor)
        }

        // 1. 当editor没有改变时，忽略被拦截，允许通知content。
        // 2. 当editor的显示和隐藏被拦截时，不允许通知content。
        val editorSucceed = isSameEditor || editorChanged
        when {
            editorSucceed && scene == null -> contentAdapter.notifyHideCurrent()
            editorSucceed && scene != null -> contentAdapter.notifyShow(scene.content)
        }

        if (editorSucceed) {
            val sceneChanged = current !== scene
            previous = current
            current = scene
            backPressedCallback?.isEnabled = current != null
            if (sceneChanged) transformState.invalidate()
            if (sceneChanged) sceneChangedListener?.onChanged(previous, current)
        }

        isActiveGoing = false
        return editorSucceed
    }

    override fun setSceneChangedListener(listener: SceneChangedListener<C, E>) {
        sceneChangedListener = listener
    }

    override fun setSceneEditorConverter(converter: SceneEditorConverter<C, E>) {
        sceneEditorConverter = converter
    }

    override fun addToOnBackPressedDispatcher(dispatcher: OnBackPressedDispatcher): Boolean {
        if (backPressedCallback != null) return false
        backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                go(scene = null)
            }
        }
        dispatcher.addCallback(lifecycleOwner, backPressedCallback!!)
        return true
    }

    override fun hasTransformer(transformer: Transformer): Boolean {
        return transformerEnforcer.has(transformer)
    }

    override fun addTransformer(transformer: Transformer) {
        transformerEnforcer.add(transformer)
    }

    override fun removeTransformer(transformer: Transformer) {
        transformerEnforcer.remove(transformer)
    }

    override fun requestTransform(transformer: Transformer) {
        transformerEnforcer.request(transformer)
    }

    private fun Window.rootParent(): ViewGroup {
        return findViewById(ROOT_PARENT_ID)
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
            this@OverlayImpl.addTransformer(transformer)
        }

        override fun removeTransformer(transformer: Transformer) {
            this@OverlayImpl.removeTransformer(transformer)
        }
    }

    private inner class SceneEditorConverterCaller : EditorChangedListener<E> {
        private var isSkipChanged = false
        private var skipPrevious: E? = null
        private var skipCurrent: E? = null
        private val currentScene get() = this@OverlayImpl.current

        override fun onEditorChanged(previous: E?, current: E?) {
            if (isActiveGoing) return
            if (consumeSkipChanged(previous, current)) return
            assert(currentScene?.editor === previous)
            val nextScene = sceneEditorConverter.nextScene(currentScene, current)
            checkNextScene(previous, current, currentScene, nextScene)
            prepareSkipChanged(current, nextScene?.editor)
            go(nextScene)
        }

        private fun checkNextScene(
            previous: E?, current: E?,
            currentScene: Scene<C, E>?,
            nextScene: Scene<C, E>?
        ) = check(currentScene !== nextScene) {
            """没有通过Overlay.go()更改Editor
               |    (previousEditor = ${previous}, currentEditor = $current)
               |    请调用Overlay.setConverter()设置${SceneEditorConverter::class.java.simpleName}，
               |    完成currentEditor = ${current}映射为Scene的逻辑
            """.trimMargin()
        }

        private fun prepareSkipChanged(previous: E?, current: E?) {
            isSkipChanged = true
            skipPrevious = previous
            skipCurrent = current
        }

        private fun consumeSkipChanged(previous: E?, current: E?): Boolean {
            if (isSkipChanged && skipPrevious == previous && skipCurrent == current) {
                isSkipChanged = false
                return true
            }
            return false
        }
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private inner class TransformerEnforcer : AnimationCallback, OnPreDrawListener {
        private val transformers = mutableListOf<Transformer>()
        private val matchTransformers = mutableListOf<Transformer>()
        private val sequenceComparator = compareBy<Transformer> { it.sequence }
        private val owner = this@OverlayImpl
        private val previousScene get() = this@OverlayImpl.previous
        private val currentScene get() = this@OverlayImpl.current

        override fun onAnimationPrepare(previous: Editor?, current: Editor?) {
            transformState.contentView.consumePendingChange()
            transformState.rootView.isVisible = true
            transformState.setScene(previousScene, currentScene)
            transformState.setTransformViews()
            transformState.isDispatching = true
            findMatchTransformers()
            dispatchMatchTransformer { onPrepare(transformState) }
        }

        override fun onAnimationStart(animation: AnimationState) {
            transformState.setOffset(animation)
            dispatchMatchTransformer { onStart(transformState) }
        }

        override fun onAnimationUpdate(animation: AnimationState) {
            transformState.setOffset(animation)
            transformState.setAlpha(animation)
            transformState.setFraction(animation)
            dispatchMatchTransformer { onUpdate(transformState) }
        }

        override fun onAnimationEnd(animation: AnimationState) {
            dispatchMatchTransformer { onEnd(transformState) }
            transformState.isDispatching = false
            transformState.contentView.removeChangeRecordPrevious()
            transformState.rootView.isVisible = transformState.current != null
            if (transformState.current == null) clearMatchTransformers()
            transformState.prepareForRequest()
        }

        override fun onPreDraw(): Boolean {
            if (!transformState.isInvalidated
                    && !transformState.isDispatching
                    && matchTransformers.isNotEmpty()) {
                dispatchMatchTransformer { onPreDraw(transformState) }
            }
            return true
        }

        fun has(transformer: Transformer): Boolean {
            return transformers.contains(transformer)
        }

        fun add(transformer: Transformer) {
            if (hasTransformer(transformer)) return
            transformers.add(transformer)
            transformer.onAttachedToOwner(owner)
        }

        fun remove(transformer: Transformer) {
            transformers.remove(transformer)
            transformer.onDetachedFromOwner(owner)
        }

        fun request(transformer: Transformer) {
            if (transformer.owner === owner
                    && !transformState.isInvalidated
                    && !transformState.isDispatching
                    && matchTransformers.contains(transformer)) {
                transformState.isDispatching = true
                transformer.onPrepare(transformState)
                transformer.onStart(transformState)
                transformer.onUpdate(transformState)
                transformer.onEnd(transformState)
                transformState.isDispatching = false
            }
        }

        private fun request() {
            // TODO: 全局数值变更，进行全量分发
            if (!transformState.isInvalidated
                    && !transformState.isDispatching
                    && matchTransformers.isNotEmpty()) {
                transformState.isDispatching = true
                dispatchMatchTransformer { onPrepare(transformState) }
                dispatchMatchTransformer { onStart(transformState) }
                dispatchMatchTransformer { onUpdate(transformState) }
                dispatchMatchTransformer { onEnd(transformState) }
                transformState.isDispatching = false
            }
        }

        private fun findMatchTransformers() {
            clearMatchTransformers()
            val tempTransformers = ArrayList<Transformer>(transformers)
            tempTransformers.takeIf { it.size > 1 }?.sortWith(sequenceComparator)
            for (i in tempTransformers.indices) {
                if (!tempTransformers[i].match(transformState)) continue
                matchTransformers.add(tempTransformers[i])
            }
        }

        private fun clearMatchTransformers() {
            matchTransformers.takeIf { it.isNotEmpty() }?.clear()
        }

        private inline fun dispatchMatchTransformer(action: Transformer.() -> Unit) {
            for (i in matchTransformers.indices) matchTransformers[i].action()
        }
    }

    private class TransformViewsImpl : TransformViews {
        override var content: View? = null
        override var editor: View? = null
        override var alpha = 1f

        fun applyAlpha(alpha: Float) {
            content?.alpha = alpha
            editor?.alpha = alpha
        }

        fun reset() {
            content = null
            editor = null
            alpha = 1f
        }
    }

    private inner class TransformStateImpl : TransformState {
        override lateinit var rootView: FrameLayout; private set
        override lateinit var backgroundView: View; private set
        override lateinit var contentView: ContentContainer; private set
        override lateinit var inputView: InputView; private set
        override var previous: Scene<C, E>? = null; private set
        override var current: Scene<C, E>? = null; private set
        override var startOffset = 0; private set
        override var endOffset = 0; private set
        override var currentOffset = 0; private set
        override var animatedFraction = 0f; private set
        override var interpolatedFraction = 0f; private set
        override val startViews = TransformViewsImpl()
        override val endViews = TransformViewsImpl()
        private val candidateAnimator by lazy { FadeEditorAnimator() }

        var isInvalidated = false; private set
        var isDispatching = false

        fun initialize(window: Window) {
            val context = window.rootParent().context
            rootView = FrameLayout(context)
            backgroundView = View(context)
            contentView = ContentContainer(context)
            inputView = InputView(context)
            rootView.isVisible = false
            rootView.addView(backgroundView, MATCH_PARENT, MATCH_PARENT)
            rootView.addView(contentView, MATCH_PARENT, MATCH_PARENT)
            rootView.addView(inputView, MATCH_PARENT, MATCH_PARENT)
        }

        fun invalidate() {
            isInvalidated = true
        }

        fun setScene(previous: Scene<C, E>?, current: Scene<C, E>?) {
            this.previous = previous
            this.current = current
            isInvalidated = false
        }

        fun setTransformViews() {
            @SuppressLint("VisibleForTests")
            val host = inputView.getEditorHost()
            val record = contentView.changeRecord
            startViews.apply {
                content = record.previousChild
                editor = host.previousView
                alpha = 1f
                applyAlpha(alpha = 1f)
            }
            endViews.apply {
                content = record.currentChild
                editor = host.currentView
                alpha = 1f
                applyAlpha(alpha = 1f)
            }
        }

        fun setOffset(animation: AnimationState) {
            startOffset = animation.startOffset
            endOffset = animation.endOffset
            currentOffset = animation.currentOffset
        }

        fun setAlpha(animation: AnimationState) {
            val animator = (inputView.editorAnimator as? FadeEditorAnimator) ?: candidateAnimator
            startViews.alpha = animator.calculateAlpha(animation, matchNull = false, start = true)
            startViews.applyAlpha(alpha = 1f)
            endViews.alpha = animator.calculateAlpha(animation, matchNull = false, start = false)
            endViews.applyAlpha(alpha = 1f)
        }

        fun setFraction(animation: AnimationState) {
            animatedFraction = animation.animatedFraction
            interpolatedFraction = animation.interpolatedFraction
        }

        fun prepareForRequest() {
            previous = current
            startOffset = endOffset
            currentOffset = endOffset
            animatedFraction = 1f
            interpolatedFraction = 1f
            startViews.reset()
            if (current == null) endViews.reset()
        }
    }
}