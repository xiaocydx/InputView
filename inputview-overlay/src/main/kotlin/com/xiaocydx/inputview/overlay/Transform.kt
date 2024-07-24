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

import android.view.View
import android.view.ViewGroup
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import com.xiaocydx.inputview.AnimationState
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.overlay.Overlay.Scene

interface Transformer {

    fun match(state: PrepareState): Boolean

    fun onPrepare(state: PrepareState) = Unit

    fun onStart(state: TransformState) = Unit

    fun onUpdate(state: TransformState) = Unit

    fun onEnd(state: TransformState) = Unit

    interface Owner {

        /**
         * 是否包含[transformer]
         */
        fun hasTransformer(transformer: Transformer): Boolean

        /**
         * 添加[Transformer]
         */
        fun addTransformer(transformer: Transformer)

        /**
         * 移除[addTransformer]添加的[transformer]
         */
        fun removeTransformer(transformer: Transformer)

        /**
         * 请求重新分发调用[Transformer]的函数
         */
        fun requestTransform()
    }
}

/**
 * [Transformer]的准备状态
 */
interface PrepareState {

    val rootView: ViewGroup

    val inputView: InputView

    val contentView: ViewGroup

    val previous: Scene<*, *>?

    val current: Scene<*, *>?

    /**
     * 动画起始视图组，其中[TransformViews.editor]等同于[AnimationState.startView]
     */
    val startViews: TransformViews

    /**
     * 动画结束视图组，其中[TransformViews.editor]等同于[AnimationState.endView]
     */
    val endViews: TransformViews
}

fun PrepareState.isPrevious(scene: Scene<*, *>?) = previous === scene

fun PrepareState.isCurrent(scene: Scene<*, *>?) = current === scene

fun PrepareState.isPrevious(content: Content?) = previous?.content === content

fun PrepareState.isCurrent(content: Content?) = current?.content === content

fun PrepareState.isPrevious(editor: Editor?) = previous?.editor === editor

fun PrepareState.isCurrent(editor: Editor?) = current?.editor === editor

fun PrepareState.view(content: Content) = when {
    isPrevious(content) -> startViews.content
    isCurrent(content) -> endViews.content
    else -> null
}

fun PrepareState.view(editor: Editor?) = when {
    isPrevious(editor) -> startViews.editor
    isCurrent(editor) -> endViews.editor
    else -> null
}

fun PrepareState.alpha(content: Content) = when {
    isPrevious(content) -> startViews.alpha
    isCurrent(content) -> endViews.alpha
    else -> 1f
}

fun PrepareState.alpha(editor: Editor?) = when {
    isPrevious(editor) -> startViews.alpha
    isCurrent(editor) -> endViews.alpha
    else -> 1f
}

/**
 * [Transformer]的动画状态
 */
interface TransformState : PrepareState {

    /**
     * 动画起始偏移值，等同于[AnimationState.startOffset]
     */
    @get:IntRange(from = 0)
    val startOffset: Int

    /**
     * 动画结束偏移值，等同于[AnimationState.endOffset]
     */
    @get:IntRange(from = 0)
    val endOffset: Int

    /**
     * 基于[interpolatedFraction]计算的动画当前偏移值，等同于[AnimationState.currentOffset]
     */
    @get:IntRange(from = 0)
    val currentOffset: Int

    /**
     * 动画起始状态和结束状态之间的原始分数进度，等同于[AnimationState.animatedFraction]
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    val animatedFraction: Float

    /**
     * 动画起始状态和结束状态之间的插值器分数进度，等同于[AnimationState.interpolatedFraction]
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    val interpolatedFraction: Float
}

/**
 * [Transformer]的动画视图组
 */
interface TransformViews {
    /**
     * [Scene.content]的视图
     */
    val content: View?

    /**
     * [Scene.editor]的视图
     */
    val editor: View?

    @get:FloatRange(from = 0.0, to = 1.0)
    val alpha: Float

    fun applyAlpha(alpha: Float = this.alpha) {
        content?.alpha = alpha
        editor?.alpha = alpha
    }
}

internal fun View.getTransformerHost(): Transformer.Owner? {
    return getTag(R.id.tag_overlay_view_transformer_host) as? Transformer.Owner
}

internal fun View.setTransformerHost(host: Transformer.Owner) {
    setTag(R.id.tag_overlay_view_transformer_host, host)
}

internal fun View.viewTransform(): Transformer.Owner {
    var owner = getTag(R.id.tag_overlay_view_transformer_owner) as? ViewTransformerOwner
    if (owner == null) {
        owner = ViewTransformerOwner(this)
        setTag(R.id.tag_overlay_view_transformer_owner, owner)
    }
    return owner
}

private class ViewTransformerOwner(
    private val view: View
) : Transformer.Owner, View.OnAttachStateChangeListener {
    private val transformers = mutableListOf<Transformer>()
    private var host: Transformer.Owner? = null

    init {
        view.addOnAttachStateChangeListener(this)
        if (view.isAttachedToWindow) onViewAttachedToWindow(view)
    }

    override fun onViewAttachedToWindow(v: View) {
        // attach在onPrepare()执行，此时transformers的函数还未分发调用
        if (host == null) host = findHost()
        val host = host ?: return
        for (i in transformers.indices) host.addTransformer(transformers[i])
    }

    override fun onViewDetachedFromWindow(v: View) {
        // detach在onPrepare()或onEnd()执行，此时transformers的函数分发调用完毕
        val host = host ?: return
        for (i in transformers.indices) host.removeTransformer(transformers[i])
    }

    override fun hasTransformer(transformer: Transformer): Boolean {
        return transformers.contains(transformer)
    }

    override fun addTransformer(transformer: Transformer) {
        if (hasTransformer(transformer)) return
        transformers.add(transformer)
        if (view.isAttachedToWindow) host?.addTransformer(transformer)
    }

    override fun removeTransformer(transformer: Transformer) {
        transformers.remove(transformer)
        host?.removeTransformer(transformer)
    }

    override fun requestTransform() {
        host?.requestTransform()
    }

    private fun findHost(): Transformer.Owner? {
        var parent = view.parent as? View
        while (parent != null) {
            val host = parent.getTransformerHost()
            if (host != null) break
            parent = parent.parent as? View
        }
        return parent?.getTransformerHost()
    }
}