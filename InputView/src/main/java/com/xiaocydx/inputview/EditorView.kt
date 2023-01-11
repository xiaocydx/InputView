package com.xiaocydx.inputview

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.lang.ref.WeakReference

/**
 * [InputView]的编辑区，负责管理[Editor]
 *
 * @author xcc
 * @date 2023/1/7
 */
internal class EditorView(context: Context) : FrameLayout(context) {
    private val views = mutableMapOf<Editor, View?>()
    private var editTextRef: WeakReference<EditText>? = null
    private var controller: WindowInsetsControllerCompat? = null
    var ime: Editor? = null
        private set
    var current: Editor? = null
        private set
    var adapter: EditorAdapter<*>? = null
        private set
    var changeRecord = ChangeRecord()
        private set

    var editText: EditText?
        get() = editTextRef?.get()
        set(value) {
            editTextRef = value?.let(::WeakReference)
            if (value != null && isAttachedToWindow) {
                controller = findViewTreeWindow()?.createWindowInsetsController(value)
            }
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val editText = editText
        if (controller == null && editText != null) {
            controller = findViewTreeWindow()?.createWindowInsetsController(editText)
        }
    }

    fun setAdapter(adapter: EditorAdapter<*>) {
        this.adapter = adapter
        current?.let(::hideChecked)
        if (views.isNotEmpty()) {
            removeAllViews()
            views.clear()
        }
        if (changeRecord.previousChild != null
                || changeRecord.currentChild != null) {
            changeRecord = ChangeRecord()
        }

        ime = null
        current = null
        val checkedAdapter = checkedAdapter() ?: return
        var imeCount = 0
        adapter.editors.forEach {
            if (checkedAdapter.isIme(it)) {
                ime = it
                imeCount++
            }
        }
        require(imeCount > 0) { "editors不包含表示IME的Editor" }
        require(imeCount == 1) { "editors包含${imeCount}个表示IME的Editor" }
    }

    fun showChecked(editor: Editor) {
        val adapter = checkedAdapter()
        if (adapter == null || current === editor) return
        val currentChild: View?
        val previous = current
        val previousChild = views[previous]
        if (!views.contains(editor)) {
            currentChild = when {
                editor === ime -> null
                else -> adapter.onCreateView(this, editor)
            }
            views[editor] = currentChild
        } else {
            currentChild = views[editor]
        }
        removeAllViews()
        currentChild?.let(::addView)
        if (previous === ime) {
            controlIme(isShow = false)
        } else if (editor === ime) {
            controlIme(isShow = true)
        }
        current = editor
        changeRecord = ChangeRecord(previousChild, currentChild)
        adapter.onVisibleChanged(previous, current)
    }

    fun hideChecked(editor: Editor) {
        val adapter = checkedAdapter()
        if (adapter != null && current === editor) {
            val previousChild = views[editor]
            removeAllViews()
            if (editor === ime) controlIme(isShow = false)
            current = null
            changeRecord = ChangeRecord(previousChild, currentChild = null)
            adapter.onVisibleChanged(editor, current)
        }
    }

    fun dispatchIme(isShow: Boolean) {
        val ime = ime ?: return
        when {
            current !== ime && isShow -> showChecked(ime)
            current === ime && !isShow -> hideChecked(ime)
        }
    }

    private fun controlIme(isShow: Boolean) {
        val type = WindowInsetsCompat.Type.ime()
        val editText = editTextRef?.get()
        if (isShow) {
            editText?.requestFocus()
            controller?.show(type)
        } else {
            editText?.clearFocus()
            controller?.hide(type)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun checkedAdapter(): EditorAdapter<Editor>? {
        return adapter as? EditorAdapter<Editor>
    }

    class ChangeRecord(val previousChild: View? = null, val currentChild: View? = null)
}