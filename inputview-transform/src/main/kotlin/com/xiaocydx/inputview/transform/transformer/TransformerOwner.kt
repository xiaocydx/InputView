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

/**
 * @author xcc
 * @date 2024/7/24
 */
interface TransformerOwner {

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

internal fun View.getTransformerHost(): TransformerOwner? {
    return getTag(R.id.tag_transform_view_transformer_host) as? TransformerOwner
}

internal fun View.setTransformerHost(host: TransformerOwner) {
    setTag(R.id.tag_transform_view_transformer_host, host)
}

internal fun View.viewTransform(): TransformerOwner {
    var owner = getTag(R.id.tag_transform_view_transformer_owner) as? ViewTransformerOwner
    if (owner == null) {
        owner = ViewTransformerOwner(this)
        setTag(R.id.tag_transform_view_transformer_owner, owner)
    }
    return owner
}

private class ViewTransformerOwner(
    private val view: View
) : TransformerOwner, View.OnAttachStateChangeListener {
    private val transformers = mutableListOf<Transformer>()
    private var host: TransformerOwner? = null

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