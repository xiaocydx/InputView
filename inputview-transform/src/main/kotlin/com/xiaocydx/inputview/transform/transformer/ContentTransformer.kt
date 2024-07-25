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
 * @date 2024/7/25
 */
abstract class ContentTransformer : Transformer() {
    private var match: ContentMatch? = null
    private var matchStart = false
    private var matchEnd = false

    fun setMatch(match: ContentMatch?) {
        this.match = match
    }

    final override fun match(state: ImperfectState): Boolean {
        matchStart = match?.match(start = true, state.previous?.content) ?: true
        matchEnd = match?.match(start = false, state.current?.content) ?: true
        return onMatch(state)
    }

    protected abstract fun onMatch(state: ImperfectState): Boolean

    protected fun ImperfectState.startView(): View? {
        return startViews.content?.takeIf { matchStart }
    }

    protected fun ImperfectState.endView(): View? {
        return endViews.content?.takeIf { matchEnd }
    }
}

fun ContentTransformer.setMatchContent(matchContent: Content) {
    setMatch { _, content -> content === matchContent }
}

fun interface ContentMatch {
    fun match(start: Boolean, content: Content?): Boolean
}