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
import com.xiaocydx.inputview.Editor

/**
 * 对匹配的[Editor]视图进行变换，用于简化模板代码
 *
 * 可调用[setMatch]设置匹配条件，当满足匹配条件时，才进行变换。
 *
 * @author xcc
 * @date 2024/7/25
 */
abstract class EditorTransformer : Transformer() {
    private var match: EditorMatch? = null
    private var matchStart = false
    private var matchEnd = false

    fun setMatch(match: EditorMatch?) {
        this.match = match
    }

    final override fun match(state: ImperfectState): Boolean {
        matchStart = match?.match(start = true, state.previous?.editor) ?: true
        matchEnd = match?.match(start = false, state.current?.editor) ?: true
        return onMatch(state)
    }

    protected abstract fun onMatch(state: ImperfectState): Boolean

    protected fun ImperfectState.startView(): View? {
        return startViews.editor?.takeIf { matchStart }
    }

    protected fun ImperfectState.endView(): View? {
        return endViews.editor?.takeIf { matchEnd }
    }
}

/**
 * [EditorTransformer.setMatch]的简化函数，当匹配[matchEditor]时，才进行变换
 */
fun EditorTransformer.setMatchEditor(matchEditor: Editor) {
    setMatch { _, editor -> editor === matchEditor }
}

/**
 * [Editor]的匹配条件
 */
fun interface EditorMatch {

    /**
     * 是否匹配
     *
     * @param start  `true`-匹配起始视图，`false`-匹配结束视图
     * @param editor `true`-`state.previous?.editor`，`false`-`state.current?.editor`
     */
    fun match(start: Boolean, editor: Editor?): Boolean
}