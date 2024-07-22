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
import androidx.annotation.CallSuper

/**
 * @author xcc
 * @date 2024/7/22
 */
abstract class ContentAdapter<T : Content> {
    internal var host: ContentHost? = null
        private set

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

interface Content

internal interface ContentHost {

    val current: Content?

    val container: ViewGroup?

    fun addTransformer(transformer: OverlayTransformer)

    fun removeTransformer(transformer: OverlayTransformer)
}

internal class EmptyAdapter : ContentAdapter<Content>() {
    override fun onCreateView(parent: ViewGroup, content: Content) = null
}