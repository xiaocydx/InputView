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

package com.xiaocydx.inputview

import android.view.View
import android.widget.EditText
import java.lang.ref.WeakReference

/**
 * [EditText]的持有类，负责处理`editText`的焦点和对[window]添加[EditText]
 *
 * @author xcc
 * @date 2023/1/18
 */
internal class EditTextHolder(editText: EditText) : ImeFocusHandler(editText) {
    private var savedCurrentFocus: WeakReference<EditText>? = null

    override fun onViewAttachedToWindow(view: View) {
        super.onViewAttachedToWindow(view)
        (view as? EditText)?.let { window?.addEditText(it) }
    }

    override fun onViewDetachedFromWindow(view: View) {
        super.onViewDetachedFromWindow(view)
        (view as? EditText)?.let { window?.removeEditText(it) }
    }

    override fun requestCurrentFocus() {
        if (restoreCurrentFocus()) return
        val currentFocus = window?.currentFocus
        if (currentFocus == null) {
            get()?.requestFocusCompat()
        } else {
            (currentFocus as? EditText)?.requestFocusCompat()
        }
    }

    override fun clearCurrentFocus() {
        saveCurrentFocus()
        val currentFocus = window?.currentFocus
        if (currentFocus == null) {
            get()?.clearFocus()
        } else {
            (currentFocus as? EditText)?.clearFocus()
        }
    }

    private fun restoreCurrentFocus(): Boolean {
        var saved: EditText? = null
        if (window?.currentFocus == null) {
            saved = savedCurrentFocus?.get()
                ?.takeIf { it.isAttachedToWindow }
        }
        savedCurrentFocus = null
        saved?.requestFocusCompat()
        return saved != null
    }

    private fun saveCurrentFocus() {
        // 显示IME期间：
        // 1. 切换到另一个第三方输入法。
        // 2. 输入密码切换到安全输入法。
        // 这些切换情况会先隐藏IME，再重新显示IME，
        // 记录currentFocus，显示IME重新获得焦点。
        val currentFocus = window?.currentFocus
        savedCurrentFocus = when (currentFocus) {
            get() -> null
            savedCurrentFocus?.get() -> savedCurrentFocus
            is EditText -> WeakReference(currentFocus)
            else -> null
        }
    }
}