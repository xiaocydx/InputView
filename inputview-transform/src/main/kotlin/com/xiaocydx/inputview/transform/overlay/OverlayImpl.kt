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

/**
 * @author xcc
 * @date 2024/7/23
 */
internal class OverlayImpl<C : Content, E : Editor>(
    private val window: Window,
    private val lifecycleOwner: LifecycleOwner,
    private val contentAdapter: ContentAdapter<C>,
    private val editorAdapter: EditorAdapter<E>,
) : Overlay<C, E> {
    private val transformState = TransformStateImpl()
    private val transformerEnforcer = TransformerEnforcer()
    private var sceneChangedListener: SceneChangedListener<C, E>? = null
    private var sceneEditorConverter: SceneEditorConverter<C, E> = defaultEditorConverter()
    private var backPressedCallback: OnBackPressedCallback? = null

    override val currentScene: Scene<C, E>?
        get() = transformState.current

    override fun setSceneChangedListener(listener: SceneChangedListener<C, E>) {
        sceneChangedListener = listener
    }

    override fun setSceneEditorConverter(converter: SceneEditorConverter<C, E>) {
        sceneEditorConverter = converter
    }

    override fun addToOnBackPressedDispatcher(dispatcher: OnBackPressedDispatcher) {
        require(backPressedCallback == null) { "已添加到OnBackPressedDispatcher" }
        backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                go(scene = null)
            }
        }
        dispatcher.addCallback(lifecycleOwner, backPressedCallback!!)
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
        editorAnimator.addAnimationCallback(transformerEnforcer)
        editorAdapter.addEditorChangedListener(SceneEditorConverterCaller())

        contentView.setAdapter(contentAdapter)
        contentView.setRemovePreviousImmediately(!editorAnimator.canRunAnimation)
        contentAdapter.onAttachedToHost(ContentHostImpl())
        return true
    }

    override fun go(scene: Scene<C, E>?): Boolean {
        if (!transformState.isInitialized) return false
        transformState.isActiveGoing = true
        val isSameEditor = editorAdapter.current === scene?.editor

        // editor的显示和隐藏允许被拦截
        val editorChanged = if (scene == null) {
            editorAdapter.notifyHideCurrent()
        } else {
            editorAdapter.notifyShow(scene.editor)
        }

        // 1. 当editor没有改变时，忽略被拦截，允许通知content。
        // 2. 当editor的显示和隐藏被拦截时，不允许通知content。
        val succeed = isSameEditor || editorChanged
        when {
            succeed && scene == null -> contentAdapter.notifyHideCurrent()
            succeed && scene != null -> contentAdapter.notifyShow(scene.content)
        }
        if (succeed) transformState.setCurrentScene(scene)
        transformState.isActiveGoing = false
        return succeed
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

    override fun requestTransform() {
        transformerEnforcer.request()
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

        override fun onEditorChanged(previous: E?, current: E?) {
            if (transformState.isActiveGoing) return
            if (consumeSkipChanged(previous, current)) return
            assert(transformState.current?.editor === previous)
            val nextScene = sceneEditorConverter.nextScene(transformState.current, current)
            checkNextScene(previous, current, transformState.current, nextScene)
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
    private inner class TransformerEnforcer : AnimationCallback {
        private val transformers = mutableListOf<Transformer>()
        private val dispatchingTransformers = mutableListOf<Transformer>()
        private val sequenceComparator = compareBy<Transformer> { it.sequence }

        fun has(transformer: Transformer): Boolean {
            return transformers.contains(transformer)
        }

        fun add(transformer: Transformer) {
            if (hasTransformer(transformer)) return
            transformers.add(transformer)
        }

        fun remove(transformer: Transformer) {
            transformers.remove(transformer)
        }

        override fun onAnimationPrepare(previous: Editor?, current: Editor?) {
            assert(transformState.previous?.editor === previous)
            assert(transformState.current?.editor === current)
            transformState.contentView.consumePendingChange()
            transformState.rootView.isVisible = true
            transformState.isDispatching = true
            transformState.setTransformViews()
            matchDispatchingTransformers()
            dispatchTransformer { onPrepare(transformState) }
        }

        override fun onAnimationStart(animation: AnimationState) {
            transformState.setOffset(animation)
            dispatchTransformer { onStart(transformState) }
        }

        override fun onAnimationUpdate(animation: AnimationState) {
            transformState.setOffset(animation)
            transformState.setAlpha(animation)
            transformState.setFraction(animation)
            dispatchTransformer { onUpdate(transformState) }
        }

        override fun onAnimationEnd(animation: AnimationState) {
            dispatchTransformer { onEnd(transformState) }
            transformState.contentView.removeChangeRecordPrevious()
            transformState.rootView.isVisible = transformState.current != null
            transformState.isDispatching = false
        }

        fun request() {
            // TODO: 校验state的有效性，处理重复分发
            // TODO: 全量copy遍历的必要性不大
            if (!transformState.isInitialized) return
            if (transformState.isDispatching) return
            transformState.isDispatching = true
            matchDispatchingTransformers()
            dispatchTransformer { onPrepare(transformState) }
            dispatchTransformer { onStart(transformState) }
            dispatchTransformer { onUpdate(transformState) }
            dispatchTransformer { onEnd(transformState) }
            transformState.isDispatching = false
        }

        private fun matchDispatchingTransformers() {
            dispatchingTransformers.takeIf { it.isNotEmpty() }?.clear()
            val tempTransformers = ArrayList<Transformer>(transformers)
            tempTransformers.takeIf { it.size > 1 }?.sortWith(sequenceComparator)
            for (i in tempTransformers.indices) {
                if (!tempTransformers[i].match(transformState)) continue
                dispatchingTransformers.add(tempTransformers[i])
            }
        }

        private inline fun dispatchTransformer(action: Transformer.() -> Unit) {
            for (i in dispatchingTransformers.indices) dispatchingTransformers[i].action()
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
    }

    private inner class TransformStateImpl : TransformState {
        override lateinit var rootView: FrameLayout; private set
        override lateinit var inputView: InputView; private set
        override lateinit var contentView: ContentContainer; private set
        override lateinit var backgroundView: View; private set
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

        var isInitialized = false; private set
        var isActiveGoing = false
        var isDispatching = false

        fun attachToParent(rootParent: ViewGroup?) {
            isInitialized = true
            val context = window.rootParent().context
            rootView = FrameLayout(context)
            inputView = InputView(context)
            contentView = ContentContainer(context)
            backgroundView = View(context)
            rootView.isVisible = false
            rootView.addView(backgroundView, MATCH_PARENT, MATCH_PARENT)
            rootView.addView(contentView, MATCH_PARENT, MATCH_PARENT)
            rootView.addView(inputView, MATCH_PARENT, MATCH_PARENT)
            rootView.setTransformerHost(this@OverlayImpl)
            if (rootParent == null || rootParent.id == windowRootParentId()) {
                lifecycleOwner.lifecycle.doOnCreated { window.rootParent().addView(rootView) }
            } else {
                rootParent.addView(rootView)
            }
        }

        fun setCurrentScene(scene: Scene<C, E>?) {
            val changed = current !== scene
            previous = current
            current = scene
            if (changed) sceneChangedListener?.onChanged(previous, current)
            if (changed) backPressedCallback?.isEnabled = current != null
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
    }

    @Suppress("ConstPropertyName")
    private companion object {
        const val statusBarEdgeToEdge = true
        const val gestureNavBarEdgeToEdge = true
    }
}