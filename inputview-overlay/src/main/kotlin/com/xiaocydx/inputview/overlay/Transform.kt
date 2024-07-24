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
import android.widget.FrameLayout
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.overlay.Overlay.Scene

interface Transformer {

    fun match(state: PrepareState): Boolean

    fun onPrepare(state: PrepareState) = Unit

    fun onStart(state: TransformState) = Unit

    fun onUpdate(state: TransformState) = Unit

    fun onEnd(state: TransformState) = Unit

    fun View.updateLayoutGravity(gravity: Int) {
        val lp = layoutParams as? FrameLayout.LayoutParams
        if (lp == null || lp.gravity == gravity) return
        lp.gravity = gravity
        layoutParams = lp
    }

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

interface PrepareState {
    val inputView: InputView

    val contentView: ViewGroup

    val previous: Scene<*, *>?

    val current: Scene<*, *>?
}

interface TransformState : PrepareState {
    /**
     * 相对于Window的初始锚点Y
     */
    @get:IntRange(from = 0)
    val initialAnchorY: Int

    /**
     * 相对于Window的动画起始锚点Y
     */
    @get:IntRange(from = 0)
    val startAnchorY: Int

    /**
     * 相对于Window的动画结束锚点Y
     */
    @get:IntRange(from = 0)
    val endAnchorY: Int

    /**
     * 对于Window的动画当前锚点Y
     */
    @get:IntRange(from = 0)
    val currentAnchorY: Int

    /**
     * 动画起始状态和结束状态之间的原始分数进度
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    val animatedFraction: Float

    /**
     * 动画起始状态和结束状态之间的插值器分数进度
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    val interpolatedFraction: Float
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