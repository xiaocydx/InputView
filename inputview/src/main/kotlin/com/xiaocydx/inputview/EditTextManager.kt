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

import android.graphics.Matrix
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import androidx.annotation.VisibleForTesting
import com.xiaocydx.insets.imeHeight
import java.lang.ref.WeakReference

/**
 * [EditText]的管理器，负责处理[EditText]的水滴状指示器
 *
 * @author xcc
 * @date 2023/12/4
 */
internal class EditTextManager(
    private val window: ViewTreeWindow,
    private val delegate: Window.Callback
) : Window.Callback by delegate {
    private val handles = mutableListOf<EditTextHandle>()
    private val callback = HideTextSelectHandleOnStart()
    private val point = FloatArray(2)
    private val location = IntArray(2)
    private var inverseMatrix: Matrix? = null
    private var touchedHandle: EditTextHandle? = null

    /**
     * 设置焦点的兼容处理
     *
     * Android 9.0以下，让[EditText]的parent获得焦点，才能清除[EditText]的焦点，
     * 实现不区分Android版本和有无[InputView]，统一处理为`contentRoot`获得焦点。
     */
    fun setFocusableCompat(decorView: ViewGroup) {
        val contentRoot = getFocusableView(decorView)
        contentRoot?.isFocusable = true
        contentRoot?.isFocusableInTouchMode = true
    }

    fun getFocusableView(decorView: ViewGroup): View? {
        return decorView.children.firstOrNull { it is ViewGroup }
    }

    fun registerHost(host: EditorHost) {
        host.addAnimationCallback(callback)
    }

    fun unregisterHost(host: EditorHost) {
        host.removeAnimationCallback(callback)
    }

    fun addEditText(editText: EditText): Boolean {
        val handle = findHandle { it === editText }
        return if (handle == null) handles.add(EditTextHandle(editText)) else false
    }

    fun removeEditText(editText: EditText): Boolean {
        val handle = findHandle { it === editText }
        return if (handle != null) handles.remove(handle) else false
    }

    @VisibleForTesting
    fun isHostRegistered(host: EditorHost): Boolean {
        return host.containsAnimationCallback(callback)
    }

    @VisibleForTesting
    fun isEditTextAdded(editText: EditText): Boolean {
        return findHandle { it === editText } != null
    }

    @VisibleForTesting
    fun peekEditTextHandleSize(): Int {
        return handles.size
    }

    @VisibleForTesting
    fun getEditTextHandleSize(): Int {
        forEach {}
        return peekEditTextHandleSize()
    }

    private inline fun findHandle(predicate: (EditText) -> Boolean): EditTextHandle? {
        forEach action@{ handle ->
            val editText = handle.get() ?: return@action
            if (predicate(editText)) return handle
        }
        return null
    }

    private inline fun forEach(action: (EditTextHandle) -> Unit) {
        for (i in handles.indices.reversed()) {
            val handle = handles[i]
            if (handle.get() != null) action(handle) else handles.removeAt(i)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            touchedHandle = findHandle { it.isTouched(ev) }
            touchedHandle?.beforeTouchEvent(ev)
        }
        val consumed = delegate.dispatchTouchEvent(ev)
        touchedHandle?.afterTouchEvent(ev)
        if (ev.action == MotionEvent.ACTION_UP
                || ev.action == MotionEvent.ACTION_CANCEL) {
            touchedHandle = null
        }
        return consumed
    }

    private fun EditText.isTouched(ev: MotionEvent): Boolean {
        if (!isAttachedToWindow || !isLaidOut || !isVisible) return false
        getLocationOnScreen(location)
        point[0] = ev.rawX - location[0]
        point[1] = ev.rawY - location[1]
        if (!matrix.isIdentity) {
            if (inverseMatrix == null) {
                inverseMatrix = Matrix()
            }
            matrix.invert(inverseMatrix)
            inverseMatrix!!.mapPoints(point)
        }
        return point[0] >= 0 && point[0] < right - left
                && point[1] >= 0 && point[1] < bottom - top
    }

    /**
     * 在实际场景中，交互可能是先选中[EditText]的文本内容，再点击其它地方切换[Editor]，
     * 当动画开始时，隐藏左右水滴状指示器，避免动画运行时不断跨进程通信，进而造成卡顿。
     */
    private inner class HideTextSelectHandleOnStart : ReplicableAnimationCallback {
        override fun onAnimationStart(state: AnimationState) = forEach { handle ->
            handle.takeIf { it.hasTextSelectHandleLeftRight() }
                ?.hideTextSelectHandle(keepFocus = state.isIme(state.current))
        }
    }

    private inner class EditTextHandle(editText: EditText) : WeakReference<EditText>(editText) {

        fun imeShown(): Boolean {
            val root = window.getRootWindowInsets()
            return if (root == null) false else root.imeHeight > 0
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
        fun afterTouchEvent(ev: MotionEvent) {
            val editText = get() ?: return
            if (ev.action == MotionEvent.ACTION_UP && editText.showSoftInputOnFocus) {
                // 点击EditText显示IME，手指抬起时隐藏水滴状指示器，
                // 注意：此时隐藏，能确保手指抬起后完全看不到指示器。
                if (!imeShown()) hideTextSelectHandle()
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
}