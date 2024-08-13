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

import android.widget.EditText
import java.lang.ref.WeakReference

/**
 * 当之前的[Content]或当前的[Content]匹配[matchContent]时：
 * 1. 若显示IME，则对`state.inputView`设置`editText`并请求焦点。
 * 2. 若隐藏IME，则清除焦点，在必要时移除第1步设置的`editText`。
 *
 * @author xcc
 * @date 2024/8/13
 */
class ContentChangeEditText(
    editText: EditText,
    private val matchContent: Content
) : Transformer() {
    private val ref = WeakReference(editText)

    override fun match(state: ImperfectState) = with(state) {
        isPrevious(matchContent) || isCurrent(matchContent)
    }

    override fun onPrepare(state: ImperfectState): Unit = with(state) {
        val ime = inputView.editorAdapter.ime
        val editText = ref.get()
        when {
            isCurrent(ime) -> inputView.editText = editText
            !isCurrent(matchContent) && inputView.editText == editText -> inputView.editText = null
        }
        if (isCurrent(ime)) editText?.requestFocus() else editText?.clearFocus()
    }
}