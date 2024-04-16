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

/**
 * [EditText]的持有类，负责处理`editText`的焦点和对[window]添加[EditText]
 *
 * @author xcc
 * @date 2023/1/18
 */
internal class EditTextHolder(editText: EditText) : ImeFocusHandler(editText) {

    override fun onViewAttachedToWindow(view: View) {
        super.onViewAttachedToWindow(view)
        (view as? EditText)?.let { window?.addEditText(it) }
    }

    override fun onViewDetachedFromWindow(view: View) {
        super.onViewDetachedFromWindow(view)
        (view as? EditText)?.let { window?.removeEditText(it) }
    }

    override fun requestCurrentFocus() {
        val currentFocus = window?.currentFocus
        if (currentFocus == null) {
            get()?.requestFocusCompat()
        } else {
            (currentFocus as? EditText)?.requestFocusCompat()
        }
    }

    override fun clearCurrentFocus() {
        val currentFocus = window?.currentFocus
        if (currentFocus == null) {
            get()?.clearFocus()
        } else {
            (currentFocus as? EditText)?.clearFocus()
        }
    }
}