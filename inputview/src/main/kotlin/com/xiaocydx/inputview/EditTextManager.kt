package com.xiaocydx.inputview

import android.graphics.Matrix
import android.view.MotionEvent
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.Window
import android.widget.EditText
import androidx.core.view.isVisible

/**
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

    fun register(host: EditorHost) {
        host.addAnimationCallback(callback)
    }

    fun unregister(host: EditorHost) {
        host.removeAnimationCallback(callback)
    }

    fun addHandle(editText: EditText): Boolean {
        if (indexOf(editText) >= 0) return false
        EditTextHandle(editText).onViewAttachedToWindow(editText)
        return true
    }

    fun removeHandle(editText: EditText): Boolean {
        val handle = indexOf(editText).let(handles::getOrNull) ?: return false
        handle.onViewDetachedFromWindow(editText)
        return true
    }

    private fun indexOf(editText: EditText): Int {
        return handles.indexOfFirst { it.editText === editText }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            touchedHandle = findTouchedHandle(ev)
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

    private inline fun forEachHandle(action: (EditTextHandle) -> Unit) {
        for (i in handles.indices.reversed()) action(handles[i])
    }

    private fun findTouchedHandle(ev: MotionEvent): EditTextHandle? {
        forEachHandle { handle -> if (handle.editText.isTouched(ev)) return handle }
        return null
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
        override fun onAnimationStart(state: AnimationState) = forEachHandle { handle ->
            handle.takeIf { it.hasTextSelectHandleLeftRight() }
                ?.hideTextSelectHandle(keepFocus = state.isIme(state.current))
        }
    }

    private inner class EditTextHandle(val editText: EditText) : OnAttachStateChangeListener {

        override fun onViewAttachedToWindow(v: View) {
            if (indexOf(editText) < 0) {
                handles.add(this)
                editText.addOnAttachStateChangeListener(this)
            }
        }

        override fun onViewDetachedFromWindow(v: View) {
            if (handles.remove(this)) {
                editText.removeOnAttachStateChangeListener(this)
            }
        }

        fun imeShown(): Boolean {
            val root = window.run { getRootWindowInsets() }
            return if (root == null) false else window.run { root.imeHeight > 0 }
        }

        /**
         * 重置[afterTouchEvent]的处理
         */
        fun beforeTouchEvent(ev: MotionEvent) {
            if (ev.action == MotionEvent.ACTION_DOWN) {
                // 按下时，将EditText重置为获得焦点显示IME
                editText.showSoftInputOnFocus = true
            }
        }

        /**
         * 点击[EditText]显示IME，需要隐藏水滴状指示器，避免动画运行时不断跨进程通信，进而造成卡顿
         */
        fun afterTouchEvent(ev: MotionEvent) {
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
            if (!editText.hasFocus()) return
            editText.clearFocus()
            if (keepFocus) editText.requestFocus()
        }

        /**
         * 由于`textSelectHandleLeft`和`textSelectHandleRight`是Android 10才有的属性，
         * 因此用`selectionStart != selectionEnd`为`true`表示当前有左右水滴状指示器。
         */
        fun hasTextSelectHandleLeftRight(): Boolean {
            return editText.selectionStart != editText.selectionEnd
        }
    }
}