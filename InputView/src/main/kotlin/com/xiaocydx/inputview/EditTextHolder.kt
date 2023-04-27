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
import android.view.ViewParent
import android.widget.EditText
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import java.lang.ref.WeakReference

/**
 * [EditText]的持有类，负责处理焦点和指示器
 *
 * @author xcc
 * @date 2023/1/18
 */
internal class EditTextHolder(editText: EditText, window: ViewTreeWindow?) {
    private val location = IntArray(2)
    private val editTextRef = WeakReference(editText)
    private val callback = HideTextSelectHandleWhileAnimationStart()
    private var host: EditorHost? = null
    private var controller: WindowInsetsControllerCompat? = null
    private val editText: EditText?
        get() {
            val editText = editTextRef.get()
            if (editText == null) controller = null
            return editText
        }

    /**
     * 不对外直接暴露[editText]
     */
    val value: Any?
        get() = editText

    init {
        checkEditTextParent()
        controller = window?.createWindowInsetsController(editText)
    }

    private fun checkEditTextParent() {
        val editText = editText ?: return
        var parent: ViewParent? = editText.parent
        while (parent != null && parent !is InputView) {
            parent = parent.parent
        }
        check(parent != null) { "EditText必须是InputView的子View或间接子View" }
    }

    fun onAttachedToWindow(window: ViewTreeWindow) {
        val editText = editText
        if (controller == null && editText != null) {
            controller = window.createWindowInsetsController(editText)
        }
    }

    fun onAttachToEditorHost(host: EditorHost) {
        this.host = host
        host.addAnimationCallback(callback)
    }

    fun onDetachFromEditorHost(host: EditorHost) {
        this.host = null
        host.removeAnimationCallback(callback)
    }

    fun hasFocus(): Boolean {
        return editText?.hasFocus() ?: false
    }

    fun requestFocus() {
        editText?.requestFocus()
    }

    fun clearFocus() {
        editText?.clearFocus()
    }

    fun showIme() {
        controller?.show(WindowInsetsCompat.Type.ime())
    }

    fun hideIme() {
        controller?.hide(WindowInsetsCompat.Type.ime())
    }

    /**
     * 重置[afterDispatchTouchEvent]的处理
     */
    fun beforeDispatchTouchEvent(ev: MotionEvent) {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            // 按下时，将EditText重置为获得焦点显示IME
            editText?.showSoftInputOnFocus = true
        }
    }

    /**
     * 点击[EditText]显示IME，需要隐藏水滴状指示器，避免动画运行时不断跨进程通信，进而造成卡顿。
     */
    fun afterDispatchTouchEvent(ev: MotionEvent) {
        val editText = editText ?: return
        val ime = host?.ime ?: return
        val current = host?.current
        if (ev.action == MotionEvent.ACTION_UP && current !== ime
                && editText.showSoftInputOnFocus && editText.isTouched(ev)) {
            // 点击EditText显示IME，手指抬起时隐藏水滴状指示器
            hideTextSelectHandle()
        }
        if (ev.action != MotionEvent.ACTION_DOWN) {
            // 若EditText有左右水滴状指示器，则表示文本被选中，此时不显示IME
            editText.showSoftInputOnFocus = !editText.hasTextSelectHandleLeftRight()
        }
    }

    /**
     * 由于`textSelectHandleXXX`是Android 10才有的属性，即[EditText]的水滴状指示器，
     * 因此通过[clearFocus]隐藏水滴状指示器，若[keepFocus]为`true`，则重新获得焦点。
     */
    private fun hideTextSelectHandle(keepFocus: Boolean = true) {
        if (!hasFocus()) return
        clearFocus()
        if (keepFocus) requestFocus()
    }

    /**
     * 由于`textSelectHandleLeft`和`textSelectHandleRight`是Android 10才有的属性，
     * 因此用`selectionStart != selectionEnd`为`true`表示当前有左右水滴状指示器。
     */
    private fun EditText.hasTextSelectHandleLeftRight(): Boolean {
        return selectionStart != selectionEnd
    }

    private fun EditText.isTouched(ev: MotionEvent): Boolean {
        if (!isVisible) return false
        getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + width
        val bottom = top + height
        val isContainX = ev.rawX in left.toFloat()..right.toFloat()
        val isContainY = ev.rawY in top.toFloat()..bottom.toFloat()
        return isContainX && isContainY
    }

    /**
     * 在实际场景中，交互可能是先选中[EditText]的文本内容，再点击其它地方切换[Editor]，
     * 当动画开始时，隐藏左右水滴状指示器，避免动画运行时不断跨进程通信，进而造成卡顿。
     */
    private inner class HideTextSelectHandleWhileAnimationStart : ReplicableAnimationCallback {

        override fun onAnimationStart(state: AnimationState) {
            val editText = editText ?: return
            if (editText.selectionStart != editText.selectionEnd) {
                hideTextSelectHandle(keepFocus = state.isIme(state.current))
            }
        }
    }
}