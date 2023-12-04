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

import android.view.MotionEvent
import android.widget.EditText
import java.lang.ref.WeakReference

/**
 * [EditText]的引用类，负责处理焦点和指示器
 *
 * @author xcc
 * @date 2023/1/18
 */
internal class EditTextWeakRef(editText: EditText) : WeakReference<EditText>(editText) {

    fun hasFocus(): Boolean {
        return get()?.hasFocus() ?: false
    }

    fun requestFocus() {
        get()?.requestFocus()
    }

    fun clearFocus() {
        get()?.clearFocus()
    }

    /**
     * 重置[afterTouchEvent]的处理
     */
    fun beforeTouchEvent(ev: MotionEvent) {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            // 按下时，将EditText重置为获得焦点显示IME
            get()?.showSoftInputOnFocus = true
        }
    }

    /**
     * 点击[EditText]显示IME，需要隐藏水滴状指示器，避免动画运行时不断跨进程通信，进而造成卡顿
     */
    fun afterTouchEvent(ev: MotionEvent, canHide: Boolean) {
        val editText = get() ?: return
        if (ev.action == MotionEvent.ACTION_UP && editText.showSoftInputOnFocus) {
            // 点击EditText显示IME，手指抬起时隐藏水滴状指示器
            if (canHide) hideTextSelectHandle()
        }
        if (ev.action != MotionEvent.ACTION_DOWN) {
            // 若EditText有左右水滴状指示器，则表示文本被选中，此时不显示IME
            editText.showSoftInputOnFocus = !hasTextSelectHandleLeftRight()
        }
    }

    /**
     * 由于`textSelectHandleXXX`是Android 10才有的属性，即[EditText]的水滴状指示器，
     * 因此通过`clearFocus`隐藏水滴状指示器，若[keepFocus]为`true`，则重新获得焦点。
     */
    fun hideTextSelectHandle(keepFocus: Boolean = true) {
        val editText = get() ?: return
        if (!editText.hasFocus()) return
        editText.clearFocus()
        if (keepFocus) editText.requestFocus()
    }

    /**
     * 由于`textSelectHandleLeft`和`textSelectHandleRight`是Android 10才有的属性，
     * 因此用`selectionStart != selectionEnd`为`true`表示当前有左右水滴状指示器。
     */
    fun hasTextSelectHandleLeftRight(): Boolean {
        val editText = get() ?: return false
        return editText.selectionStart != editText.selectionEnd
    }
}