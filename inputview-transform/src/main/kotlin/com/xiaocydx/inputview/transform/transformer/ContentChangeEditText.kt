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
 * @author xcc
 * @date 2024/8/13
 */
class ContentChangeEditText(
    editText: EditText,
    private val content: Content
) : Transformer() {
    private val ref = WeakReference(editText)

    override fun match(state: ImperfectState) = with(state) {
        isPrevious(content) || isCurrent(content)
    }

    override fun onPrepare(state: ImperfectState): Unit = with(state) {
        val ime = inputView.editorAdapter.ime
        val editText = ref.get()
        when {
            isCurrent(ime) -> inputView.editText = editText
            !isCurrent(content) && inputView.editText == editText -> inputView.editText = null
        }
        if (isCurrent(ime)) editText?.requestFocus() else editText?.clearFocus()
    }
}