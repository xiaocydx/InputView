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

@file:Suppress("PackageDirectoryMismatch")

package com.xiaocydx.inputview.transform

import android.view.View
import androidx.annotation.VisibleForTesting

/**
 * [Transformer]所有者
 *
 * @author xcc
 * @date 2024/7/24
 */
interface TransformerOwner {

    /**
     * 是否包含[transformer]
     */
    fun has(transformer: Transformer): Boolean

    /**
     * 添加[Transformer]
     */
    fun add(transformer: Transformer)

    /**
     * 移除[add]添加的[transformer]
     */
    fun remove(transformer: Transformer)

    /**
     * 请求重新分发调用[transformer]的函数
     */
    fun requestTransform(transformer: Transformer)
}

internal fun View.getTransformerHost(): TransformerOwner? {
    return getTag(R.id.tag_transform_view_transformer_host) as? TransformerOwner
}

internal fun View.setTransformerHost(host: TransformerOwner) {
    setTag(R.id.tag_transform_view_transformer_host, host)
}

internal fun View.viewTransform(): ViewTransformerOwner {
    var owner = getTag(R.id.tag_transform_view_transformer_owner) as? ViewTransformerOwner
    if (owner == null) {
        owner = ViewTransformerOwner(this)
        setTag(R.id.tag_transform_view_transformer_owner, owner)
    }
    return owner
}

internal class ViewTransformerOwner(
    private val view: View
) : TransformerOwner, View.OnAttachStateChangeListener {
    private val transformers = mutableListOf<Transformer>()
    private var host: TransformerOwner? = null

    init {
        view.addOnAttachStateChangeListener(this)
        if (view.isAttachedToWindow) onViewAttachedToWindow(view)
    }

    @VisibleForTesting
    fun setHost(host: TransformerOwner) {
        require(this.host == null)
        this.host = host
    }

    override fun onViewAttachedToWindow(v: View) {
        // attach在onPrepare()执行，此时transformers的函数还未分发调用
        if (host == null) host = findHost()
        val host = host ?: return
        for (i in transformers.indices) host.add(transformers[i])
    }

    override fun onViewDetachedFromWindow(v: View) {
        // detach在onPrepare()或onEnd()执行，此时transformers的函数分发调用完毕
        val host = host ?: return
        for (i in transformers.indices) host.remove(transformers[i])
    }

    override fun has(transformer: Transformer): Boolean {
        return transformers.contains(transformer)
    }

    override fun add(transformer: Transformer) {
        if (has(transformer)) return
        transformers.add(transformer)
        if (view.isAttachedToWindow) host?.add(transformer)
    }

    override fun remove(transformer: Transformer) {
        transformers.remove(transformer)
        host?.remove(transformer)
    }

    override fun requestTransform(transformer: Transformer) {
        host?.requestTransform(transformer)
    }

    private fun findHost(): TransformerOwner? {
        var parent = view.parent as? View
        while (parent != null) {
            val host = parent.getTransformerHost()
            if (host != null) break
            parent = parent.parent as? View
        }
        return parent?.getTransformerHost()
    }
}