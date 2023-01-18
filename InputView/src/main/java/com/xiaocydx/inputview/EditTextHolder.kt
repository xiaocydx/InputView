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
internal class EditTextHolder(
    editText: EditText,
    private var window: ViewTreeWindow?
) {
    private val location = IntArray(2)
    private val editTextRef = WeakReference(editText)
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
        this.window = window
        val editText = editText
        if (controller == null && editText != null) {
            controller = window.createWindowInsetsController(editText)
        }
    }

    var showSoftInputOnFocus: Boolean
        get() = editText?.showSoftInputOnFocus ?: false
        set(value) {
            val editText = editText ?: return
            // 虽然源码是单纯的赋值逻辑，但是出于稳妥起见，做差异对比
            if (editText.showSoftInputOnFocus != value) {
                editText.showSoftInputOnFocus = value
            }
        }

    val hasTextSelectHandleLeftToRight: Boolean
        get() {
            val editText = editText ?: return false
            return editText.selectionStart != editText.selectionEnd
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

    fun hideTextSelectHandle(keepFocus: Boolean = true) {
        val hasFocus = hasFocus()
        if (!hasFocus) return
        clearFocus()
        if (keepFocus) requestFocus()
    }

    fun isTouched(ev: MotionEvent): Boolean {
        val editText = editText
        if (editText == null || !editText.isVisible) return false
        editText.getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + editText.width
        val bottom = top + editText.height
        val isContainX = ev.rawX in left.toFloat()..right.toFloat()
        val isContainY = ev.rawY in top.toFloat()..bottom.toFloat()
        return isContainX && isContainY
    }
}