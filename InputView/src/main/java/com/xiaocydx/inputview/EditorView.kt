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
    var ime: Editor? = null; private set
    var current: Editor? = null; private set
    var adapter: EditorAdapter<*>? = null; private set
    var changeRecord = ChangeRecord(); private set

    var editText: EditText?
        get() = editTextRef?.get()
        set(value) {
            controller = null
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

    fun showChecked(editor: Editor, controlIme: Boolean = true): Boolean {
        val adapter = checkedAdapter()
        if (adapter == null || current === editor) return false
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
            handleImeShown(shown = false, controlIme)
        } else if (editor === ime) {
            handleImeShown(shown = true, controlIme)
        }
        current = editor
        changeRecord = ChangeRecord(previousChild, currentChild)
        adapter.onEditorChanged(previous, current)
        return true
    }

    fun hideChecked(editor: Editor, controlIme: Boolean = true): Boolean {
        val adapter = checkedAdapter()
        if (adapter != null && current === editor) {
            val previousChild = views[editor]
            removeAllViews()
            if (editor === ime) handleImeShown(shown = false, controlIme)
            current = null
            changeRecord = ChangeRecord(previousChild, currentChild = null)
            adapter.onEditorChanged(editor, current)
            return true
        }
        return false
    }

    fun dispatchImeShown(shown: Boolean): Boolean {
        val ime = ime ?: return false
        return when {
            current !== ime && shown -> showChecked(ime, controlIme = false)
            current === ime && !shown -> hideChecked(ime, controlIme = false)
            else -> false
        }
    }

    private fun handleImeShown(shown: Boolean, controlIme: Boolean) {
        val editText = editText
        if (editText == null) {
            controller = null
            return
        }
        val type = WindowInsetsCompat.Type.ime()
        if (shown) {
            editText.requestFocus()
            if (controlIme) controller?.show(type)
        } else {
            editText.clearFocus()
            if (controlIme) controller?.hide(type)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun checkedAdapter(): EditorAdapter<Editor>? {
        return adapter as? EditorAdapter<Editor>
    }

    class ChangeRecord(val previousChild: View? = null, val currentChild: View? = null)
}