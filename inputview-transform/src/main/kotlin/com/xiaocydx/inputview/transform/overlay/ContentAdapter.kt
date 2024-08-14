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
import android.view.ViewGroup
import androidx.annotation.CallSuper

/**
 * [Content]的适配器，负责创建和通知显示[Content]的视图
 *
 * ```
 * enum class VideoContent : Content {
 *     TEXT, TITLE
 * }
 *
 * class VideoEditorAdapter : ContentAdapter<VideoContent>() {
 *
 *     override fun onCreateView(parent: ViewGroup, content: VideoContent): View {
 *         return when(content) {
 *             VideoContent.TEXT -> TextView()
 *             VideoContent.TITLE -> TitleView()
 *         }
 *     }
 * }
 * ```
 *
 * @author xcc
 * @date 2024/7/22
 */
abstract class ContentAdapter<T : Content> {
    internal var host: ContentHost? = null
        private set

    /**
     * 创建[content]的视图，返回`null`表示不需要视图
     */
    abstract fun onCreateView(parent: ViewGroup, content: T): View?

    @CallSuper
    internal open fun onAttachedToHost(host: ContentHost) {
        check(this.host == null) { "ContentAdapter已关联ContentHost" }
        this.host = host
    }

    @CallSuper
    internal open fun onDetachedFromHost(host: ContentHost) {
        this.host = null
    }
}

/**
 * [Overlay]内容区的内容
 *
 * 推荐用`enum class`或`sealed class`实现[Content]，例如：
 * ```
 * enum class VideoContent : Content {
 *     TEXT, TITLE
 * }
 * ```
 *
 * 用`enum class`或`sealed class`能更好的进行模式匹配。
 */
interface Content

@Suppress("UNCHECKED_CAST")
internal val <T : Content> ContentAdapter<T>.current: T?
    get() = host?.current as? T

internal fun <T : Content> ContentAdapter<T>.notifyShow(content: T): Boolean {
    return host?.showChecked(content) ?: false
}

internal fun <T : Content> ContentAdapter<T>.notifyHide(content: T): Boolean {
    return host?.hideChecked(content) ?: false
}

internal fun <T : Content> ContentAdapter<T>.notifyHideCurrent(): Boolean {
    return current?.let(::notifyHide) ?: false
}

internal interface ContentHost {
    val current: Content?
    val container: ViewGroup?
    fun showChecked(content: Content): Boolean
    fun hideChecked(content: Content): Boolean
    fun addTransformer(transformer: Transformer)
    fun removeTransformer(transformer: Transformer)
}

internal class EmptyAdapter : ContentAdapter<Content>() {
    override fun onCreateView(parent: ViewGroup, content: Content) = null
}