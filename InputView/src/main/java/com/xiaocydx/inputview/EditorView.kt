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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val editText = editTextRef?.get()
        editTextRef = null
        if (editText != null) {
            val window = findViewTreeWindow() ?: return
            controller = WindowInsetsControllerCompat(window, editText)
        }
    }

    fun setAdapter(adapter: EditorAdapter<*>) {
        if (childCount > 0) removeAllViews()
        this.adapter = adapter
        val checkedAdapter = checkedAdapter() ?: return
        var imeCount = 0
        adapter.editors.forEach {
            if (checkedAdapter.isIme(it)) {
                ime = it
                imeCount++
            }
        }
        require(imeCount > 0) { "editors不包含表示IME的Editor" }
        require(imeCount == 1) { "editors包${imeCount}个表示IME的Editor" }
    }

    fun setEditText(editText: EditText) {
        if (isAttachedToWindow) {
            val window = findViewTreeWindow() ?: return
            controller = WindowInsetsControllerCompat(window, editText)
        } else {
            editTextRef = WeakReference(editText)
        }
    }

    fun showChecked(editor: Editor) {
        val adapter = checkedAdapter()
        if (adapter == null || current === editor) return
        val view: View?
        val previous = current
        val previousView = views[previous]
        if (!views.contains(editor)) {
            view = when {
                editor === ime -> null
                else -> adapter.onCreateView(this, editor)
            }
            views[editor] = view
        } else {
            view = views[editor]
        }
        previousView?.let(::removeView)
        view?.let(::addView)
        if (previous === ime) {
            controlIme(isShow = false)
        } else if (editor === ime) {
            controlIme(isShow = true)
        }
        current = editor
        adapter.onVisibleChanged(previous, current)
    }

    fun hideChecked(editor: Editor) {
        val adapter = checkedAdapter()
        if (adapter != null && current === editor) {
            views[editor]?.let(::removeView)
            if (editor === ime) controlIme(isShow = false)
            current = null
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
        if (isShow) controller?.show(type) else controller?.hide(type)
    }

    @Suppress("UNCHECKED_CAST")
    private fun checkedAdapter(): EditorAdapter<Editor>? {
        return adapter as? EditorAdapter<Editor>
    }
}