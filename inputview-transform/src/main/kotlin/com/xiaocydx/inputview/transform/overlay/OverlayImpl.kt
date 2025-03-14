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
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.Window
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.savedstate.SavedStateRegistry.SavedStateProvider
import androidx.savedstate.SavedStateRegistryOwner
import com.xiaocydx.inputview.AnimationCallback
import com.xiaocydx.inputview.AnimationInterceptor
import com.xiaocydx.inputview.AnimationState
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.EditorAdapter
import com.xiaocydx.inputview.EditorAnimator
import com.xiaocydx.inputview.EditorChangedListener
import com.xiaocydx.inputview.EditorMode
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.ViewTreeWindow
import com.xiaocydx.inputview.current
import com.xiaocydx.inputview.disableGestureNavBarOffset
import com.xiaocydx.inputview.notifyHideCurrent
import com.xiaocydx.inputview.notifyShow
import com.xiaocydx.inputview.requireViewTreeWindow
import com.xiaocydx.inputview.transform.Overlay.Companion.ROOT_PARENT_ID
import com.xiaocydx.insets.consumeInsets
import com.xiaocydx.insets.navigationBarHeight
import com.xiaocydx.insets.navigationBars
import com.xiaocydx.insets.setOnApplyWindowInsetsListenerCompat
import com.xiaocydx.insets.updateMargins

/**
 * [Overlay]的实现类
 *
 * @author xcc
 * @date 2024/7/23
 */
@PublishedApi
internal class OverlayImpl<S : Scene<C, E>, C : Content, E : Editor>(
    override val sceneList: List<S>,
    override val lifecycleOwner: SavedStateRegistryOwner,
    private val contentAdapter: ContentAdapter<C>,
    private val editorAdapter: EditorAdapter<E>,
    private val editorAnimator: EditorAnimator,
    private val isStatefulSceneEnabled: Boolean
) : Overlay<S> {
    private val transformState = TransformStateImpl()
    private val transformerDispatcher = TransformerDispatcher()
    private val statefulSceneProvider = StatefulSceneProvider()
    private var backPressedCallback: OnBackPressedCallback? = null
    private var isInitialized = false
    private var isActiveGoing = false

    override var previous: S? = null; private set
    override var current: S? = null; private set
    override var sceneChangedListener: SceneChangedListener<S>? = null
    override var sceneEditorConverter: SceneEditorConverter<S>? = null

    override fun attach(window: Window, rootParent: ViewGroup?): Boolean {
        if (isInitialized) return false
        isInitialized = true

        transformState.apply {
            initialize(window)
            rootView.setTransformerHost(this@OverlayImpl)

            inputView.editorAdapter = editorAdapter
            inputView.editorAnimator = editorAnimator
            inputView.editorMode = EditorMode.ADJUST_PAN
            inputView.disableGestureNavBarOffset()
            editorAdapter.addEditorChangedListener(SceneEditorConverterCaller())

            contentView.setAdapter(contentAdapter)
            contentView.setRemovePreviousImmediately(!editorAnimator.canRunAnimation)
            contentAdapter.onAttachedToHost(ContentHostImpl())

            // attachedToWindow -> add transformerDispatcher
            // detachedFromWindow -> remove transformerDispatcher
            transformerDispatcher.initialize(rootView, editorAnimator)
        }

        // 兼容insets-systembar的层级关系，补充未消费的导航栏间距
        var viewTreeWindow: ViewTreeWindow? = null
        transformState.rootView.setOnApplyWindowInsetsListenerCompat { v, insets ->
            viewTreeWindow = viewTreeWindow ?: v.requireViewTreeWindow()
            val isSupport = viewTreeWindow!!.run { insets.supportGestureNavBarEdgeToEdge }
            v.updateMargins(bottom = if (isSupport) 0 else insets.navigationBarHeight)
            if (isSupport) insets else insets.consumeInsets(navigationBars())
        }

        if (rootParent != null && rootParent.id != ROOT_PARENT_ID) {
            rootParent.addView(transformState.rootView)
        } else {
            // window.rootParent()可能被替换，在替换完成后添加rootView
            lifecycleOwner.lifecycle.doOnCreated {
                window.rootParent().addView(transformState.rootView)
            }
        }

        // 移除rootView，通过detachedFromWindow移除transformerDispatcher
        lifecycleOwner.lifecycle.doOnDestroyed {
            val parent = transformState.rootView.parent as? ViewGroup
            parent?.removeView(transformState.rootView)
        }

        statefulSceneProvider.initialize()
        return true
    }

    override fun go(scene: S?): Boolean {
        require(isInitialized) { "未调用Overlay.attach()" }
        require(scene == null || sceneList.contains(scene)) { "Overlay.sceneList不包含${scene}" }
        isActiveGoing = true
        statefulSceneProvider.clearPendingScene()

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

        val sceneChanged = current !== scene
        if (editorSucceed && sceneChanged) {
            previous = current
            current = scene
            transformState.invalidate()
            sceneChangedListener?.onChanged(previous, current)
            // editor一致，content不同，忽略editor请求运行动画
            if (isSameEditor) editorAnimator.requestSimpleAnimation()
        }
        backPressedCallback?.isEnabled = current != null

        isActiveGoing = false
        return editorSucceed && sceneChanged
    }

    override fun addToOnBackPressedDispatcher(dispatcher: OnBackPressedDispatcher): Boolean {
        if (backPressedCallback != null) return false
        backPressedCallback = object : OnBackPressedCallback(current != null) {
            override fun handleOnBackPressed() {
                go(scene = null)
            }
        }
        dispatcher.addCallback(lifecycleOwner, backPressedCallback!!)
        return true
    }

    override fun setAnimationInterceptor(interceptor: AnimationInterceptor) {
        editorAnimator.setAnimationInterceptor(interceptor)
    }

    override fun has(transformer: Transformer): Boolean {
        return transformerDispatcher.has(transformer)
    }

    override fun add(transformer: Transformer) {
        transformerDispatcher.add(transformer)
    }

    override fun remove(transformer: Transformer) {
        transformerDispatcher.remove(transformer)
    }

    override fun requestTransform(transformer: Transformer) {
        transformerDispatcher.request(transformer)
    }

    private fun Window.rootParent(): ViewGroup {
        return findViewById(ROOT_PARENT_ID)
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

    private inline fun Lifecycle.doOnDestroyed(crossinline action: () -> Unit) {
        addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (source.lifecycle.currentState == DESTROYED) action()
            }
        })
    }

    private inner class StatefulSceneProvider : SavedStateProvider {
        private var pendingScene: S? = null

        fun initialize() {
            val registry = lifecycleOwner.savedStateRegistry
            registry.registerSavedStateProvider(KEY_SCENE, this)
            lifecycleOwner.lifecycle.doOnDestroyed {
                registry.unregisterSavedStateProvider(KEY_SCENE)
                if (!canSaveScene()) go(scene = null)
            }

            val bundle = registry.consumeRestoredStateForKey(KEY_SCENE)
            val index = bundle?.getInt(KEY_SCENE, NO_INDEX) ?: NO_INDEX
            pendingScene = sceneList.getOrNull(index) ?: return
            // 在初始化阶段之后，InputView消费pendingSavedState之前，设置pendingScene
            lifecycleOwner.lifecycle.doOnCreated { pendingScene?.let(::go) }
        }

        fun clearPendingScene() {
            pendingScene = null
        }

        override fun saveState(): Bundle {
            val bundle = Bundle(1)
            bundle.putInt(KEY_SCENE, currentSaveIndex())
            return bundle
        }

        private fun canSaveScene(): Boolean {
            return currentSaveIndex() != NO_INDEX
        }

        private fun currentSaveIndex(): Int {
            if (!isStatefulSceneEnabled) return NO_INDEX
            return sceneList.indexOfFirst { it === current }
        }
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
            this@OverlayImpl.add(transformer)
        }

        override fun removeTransformer(transformer: Transformer) {
            this@OverlayImpl.remove(transformer)
        }
    }

    private inner class SceneEditorConverterCaller : EditorChangedListener<E> {
        private var isSkipChanged = false
        private var skipPrevious: E? = null
        private var skipCurrent: E? = null
        private val previousScene get() = this@OverlayImpl.previous
        private val currentScene get() = this@OverlayImpl.current

        override fun onEditorChanged(previous: E?, current: E?) {
            if (isActiveGoing) return
            if (consumeSkipChanged(previous, current)) return
            assert(currentScene?.editor === previous)

            val nextEditor = current
            var nextScene = currentScene
            sceneEditorConverter?.let {
                nextScene = it.nextScene(previousScene, currentScene, nextEditor)
            }

            var matchCount = 0
            if (nextScene === currentScene) {
                if (nextEditor == null) {
                    nextScene = null
                } else {
                    for (i in sceneList.indices) {
                        val scene = sceneList[i]
                        if (scene.editor !== nextEditor) continue
                        matchCount++
                        nextScene = scene
                        if (scene.content === currentScene?.content) {
                            matchCount = 1
                            break
                        }
                    }
                }
            }

            require(matchCount <= 1 && nextScene !== currentScene) {
                var matchError = ""
                if (matchCount > 1) {
                    val matchScene = sceneList.filter { it.editor === nextEditor }
                    matchError = "\n|    Overlay.sceneList中${matchScene}的Scene.editor都等于nextEditor，"
                }
                """没有通过Overlay.go()更改Editor${matchError}
                |    请设置Overlay.sceneEditorConverter, 完成nextEditor = ${nextEditor}转换为Scene的逻辑
                 """.trimMargin()
            }

            prepareSkipChanged(current, nextScene?.editor)
            go(nextScene)
        }

        private fun prepareSkipChanged(previous: E?, current: E?) {
            isSkipChanged = true
            skipPrevious = previous
            skipCurrent = current
        }

        private fun consumeSkipChanged(previous: E?, current: E?): Boolean {
            if (isSkipChanged && skipPrevious === previous && skipCurrent === current) {
                isSkipChanged = false
                return true
            }
            return false
        }
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private inner class TransformerDispatcher : View.OnAttachStateChangeListener,
            AnimationCallback, OnPreDrawListener {
        private val transformers = mutableListOf<Transformer>()
        private val matchTransformers = mutableListOf<Transformer>()
        private val sequenceComparator = compareBy<Transformer> { it.sequence }
        private val owner = this@OverlayImpl
        private val previousScene get() = this@OverlayImpl.previous
        private val currentScene get() = this@OverlayImpl.current

        fun initialize(view: View, animator: EditorAnimator) {
            view.addOnAttachStateChangeListener(this)
            view.takeIf { it.isAttachedToWindow }?.let(::onViewAttachedToWindow)
            animator.addAnimationCallback(this)
        }

        override fun onViewAttachedToWindow(v: View) {
            v.viewTreeObserver.addOnPreDrawListener(this)
        }

        override fun onViewDetachedFromWindow(v: View) {
            v.viewTreeObserver.removeOnPreDrawListener(this)
        }

        override fun onAnimationPrepare(previous: Editor?, current: Editor?) {
            transformState.isDispatching = true
            transformState.prepare(previousScene, currentScene)
            findMatchTransformers(checkState = false)
            dispatchMatchTransformers { onPrepare(transformState) }
        }

        override fun onAnimationStart(animation: AnimationState) {
            transformState.preStart(animation)
            dispatchMatchTransformers { onStart(transformState) }
        }

        override fun onAnimationUpdate(animation: AnimationState) {
            transformState.preUpdate(animation)
            dispatchMatchTransformers { onUpdate(transformState) }
        }

        override fun onAnimationEnd(animation: AnimationState) {
            dispatchMatchTransformers { onEnd(transformState) }
            transformState.postEnd(animation)
            // 更改transformState后，再次查找matchTransformers
            findMatchTransformers(checkState = true)
            transformState.isDispatching = false
        }

        override fun onPreDraw(): Boolean {
            dispatchMatchTransformersOnPreDraw()
            return true
        }

        fun has(transformer: Transformer): Boolean {
            return transformers.contains(transformer)
        }

        fun add(transformer: Transformer) {
            if (has(transformer)) return
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

        private fun findMatchTransformers(checkState: Boolean) {
            matchTransformers.takeIf { it.isNotEmpty() }?.clear()
            if (checkState && transformState.current == null) return
            // 按add顺序匹配matchTransformers
            for (i in transformers.indices) {
                if (!transformers[i].match(transformState)) continue
                matchTransformers.add(transformers[i])
            }
            // 按sequence顺序分发matchTransformers
            matchTransformers.takeIf { it.size > 1 }?.sortWith(sequenceComparator)
        }

        private fun dispatchMatchTransformersOnPreDraw() {
            if (!transformState.isInvalidated
                    && !transformState.isDispatching
                    && matchTransformers.isNotEmpty()) {
                if (transformState.consumeOffsetChange()) {
                    // editorOffset更改，重新分发matchTransformers执行变换逻辑，
                    // 先将isDispatching设为true，避免onPreDraw()执行request()。
                    transformState.isDispatching = true
                    dispatchMatchTransformers { onPreDraw(transformState) }
                    dispatchMatchTransformers { onPrepare(transformState) }
                    dispatchMatchTransformers { onStart(transformState) }
                    dispatchMatchTransformers { onUpdate(transformState) }
                    dispatchMatchTransformers { onEnd(transformState) }
                    transformState.isDispatching = false
                } else {
                    dispatchMatchTransformers { onPreDraw(transformState) }
                }
            }
        }

        private inline fun dispatchMatchTransformers(action: Transformer.() -> Unit) {
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

    private class TransformRootView(context: Context) : FrameLayout(context) {
        override fun dispatchApplyWindowInsets(insets: WindowInsets): WindowInsets {
            super.dispatchApplyWindowInsets(insets)
            // 兼容到跟Android 11一样的分发效果，确保同级子View能处理已消费的Insets
            return insets
        }
    }

    private inner class TransformStateImpl : TransformState {
        override lateinit var rootView: TransformRootView; private set
        override lateinit var backgroundView: View; private set
        override lateinit var contentView: ContentContainer; private set
        override lateinit var inputView: InputView; private set
        override var previous: S? = null; private set
        override var current: S? = null; private set
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
            rootView = TransformRootView(context)
            backgroundView = View(context)
            contentView = ContentContainer(context)
            inputView = InputView(context)
            rootView.addView(backgroundView, MATCH_PARENT, MATCH_PARENT)
            rootView.addView(contentView, MATCH_PARENT, MATCH_PARENT)
            rootView.addView(inputView, MATCH_PARENT, MATCH_PARENT)
            setVisible(isVisible = false)
        }

        fun setVisible(isVisible: Boolean) {
            rootView.isEnabled = isVisible
            // 设置View.INVISIBLE是为了让rootView能布局，在执行变换操作之前rootView有尺寸。
            // 当current = null时，Content和Editor的视图都会被移除，布局流程不会有性能问题。
            val visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
            if (visibility != rootView.visibility) rootView.visibility = visibility
        }

        fun invalidate() {
            isInvalidated = true
        }

        fun prepare(previous: S?, current: S?) {
            setVisible(true)
            if (!isInvalidated) return
            isInvalidated = false

            // 消费pendingChange，构建changeRecord
            contentView.consumePendingChange()
            this.previous = previous
            this.current = current

            @SuppressLint("VisibleForTests")
            val host = inputView.getEditorHost()
            val record = contentView.changeRecord
            startViews.apply {
                content = when (previous?.content) {
                    current?.content -> record.currentChild
                    else -> record.previousChild
                }
                editor = when (previous?.editor) {
                    current?.editor -> host.currentView
                    else -> host.previousView
                }
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

        fun preStart(animation: AnimationState) {
            setOffset(animation)
        }

        fun preUpdate(animation: AnimationState) {
            setOffset(animation)
            val animator = (editorAnimator as? FadeEditorAnimator) ?: candidateAnimator
            startViews.alpha = animator.calculateAlpha(animation, matchNull = false, start = true)
            startViews.applyAlpha(alpha = 1f)
            endViews.alpha = animator.calculateAlpha(animation, matchNull = false, start = false)
            endViews.applyAlpha(alpha = 1f)
            animatedFraction = animation.animatedFraction
            interpolatedFraction = animation.interpolatedFraction
        }

        fun postEnd(animation: AnimationState) {
            setVisible(current != null)
            contentView.removeChangeRecordPrevious()
            previous = current
            setOffset(animation.endOffset)
            animatedFraction = 1f
            interpolatedFraction = 1f
            // 清除views，避免内存泄漏
            startViews.reset()
            if (current == null) endViews.reset()
        }

        fun consumeOffsetChange(): Boolean {
            val offset = inputView.editorOffset
            if (offset != endOffset) {
                setOffset(offset)
                return true
            }
            return false
        }

        private fun setOffset(animation: AnimationState) {
            startOffset = animation.startOffset
            endOffset = animation.endOffset
            currentOffset = animation.currentOffset
        }

        private fun setOffset(offset: Int) {
            startOffset = offset
            endOffset = offset
            currentOffset = offset
        }
    }

    private companion object {
        const val NO_INDEX = -1
        const val KEY_SCENE = "com.xiaocydx.inputview.transform.KEY_SCENE"
    }
}