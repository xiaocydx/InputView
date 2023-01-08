package com.xiaocydx.inputview

import android.view.View
import android.view.ViewGroup

interface EditorType

fun interface EditorVisibleListener<in T : EditorType> {
    fun onVisibleChanged(previous: T?, current: T?)
}

abstract class EditorAdapter<T : EditorType> {
    private var listeners = ArrayList<EditorVisibleListener<T>>(2)
    abstract val types: List<T>
    internal var inputView: InputView? = null
        private set
    internal var editorView: EditorView? = null
        private set

    internal fun attach(inputView: InputView, editorView: EditorView) {
        this.inputView = inputView
        this.editorView = editorView
    }

    internal fun detach(inputView: InputView, editorView: EditorView) {
        require(this.inputView === inputView)
        require(this.editorView === editorView)
        this.inputView = null
        this.editorView = null
    }

    abstract fun isImeType(type: T): Boolean

    abstract fun onCreateView(parent: ViewGroup, type: T): View?

    fun addEditorVisibleListener(listener: EditorVisibleListener<T>) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeEditorVisibleListener(listener: EditorVisibleListener<T>) {
        listeners.remove(listener)
    }

    internal fun dispatchVisible(previous: T?, current: T?) {
        for (index in listeners.indices.reversed()) {
            listeners[index].onVisibleChanged(previous, current)
        }
    }
}