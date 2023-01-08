package com.xiaocydx.inputview

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.lang.ref.WeakReference

/**
 * @author xcc
 * @date 2023/1/7
 */
internal class EditorView(context: Context) : FrameLayout(context) {
    private val views = mutableMapOf<EditorType, View?>()
    private var editTextRef: WeakReference<EditText>? = null
    private var controller: WindowInsetsControllerCompat? = null
    var imeType: EditorType? = null
        private set
    var currentType: EditorType? = null
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
        imeType = adapter.types.firstOrNull {
            checkedAdapter()?.isImeType(it) == true
        }
    }

    fun setEditText(editText: EditText) {
        if (isAttachedToWindow) {
            val window = findViewTreeWindow() ?: return
            controller = WindowInsetsControllerCompat(window, editText)
        } else {
            editTextRef = WeakReference(editText)
        }
    }

    fun dispatchIme(show: Boolean) {
        val imeType = imeType ?: return
        when {
            currentType != imeType && show -> showChecked(imeType)
            currentType === imeType && !show -> hideChecked(imeType)
        }
    }

    fun showChecked(type: EditorType) {
        val adapter = checkedAdapter()
        if (adapter == null || currentType == type) return
        val view: View?
        val previousType = currentType
        val previousView = views[previousType]
        if (!views.contains(type)) {
            view = when (type) {
                imeType -> null
                else -> adapter.onCreateView(this, type)
            }
            views[type] = view
        } else {
            view = views[type]
        }
        previousView?.let(::removeView)
        view?.let(::addView)
        currentType = type
        if (previousType == imeType) {
            controlIme(show = false)
        } else if (type == imeType) {
            controlIme(show = true)
        }
        adapter.dispatchVisible(previousType, currentType)
    }

    fun hideChecked(type: EditorType) {
        val adapter = checkedAdapter()
        if (adapter != null && currentType == type) {
            views[type]?.let(::removeView)
            currentType = null
            if (type == imeType) controlIme(show = false)
            adapter.dispatchVisible(type, null)
        }
    }

    private fun controlIme(show: Boolean) {
        val type = WindowInsetsCompat.Type.ime()
        if (show) controller?.show(type) else controller?.hide(type)
    }

    @Suppress("UNCHECKED_CAST")
    private fun checkedAdapter(): EditorAdapter<EditorType>? {
        return adapter as? EditorAdapter<EditorType>
    }
}