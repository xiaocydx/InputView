package com.xiaocydx.inputview

@Suppress("UNCHECKED_CAST")
val <T : EditorType> EditorAdapter<T>.currentType: T?
    get() = editorView?.currentType as? T

fun <T : EditorType> EditorAdapter<T>.notifyShow(type: T) {
    editorView?.showChecked(type)
}

fun <T : EditorType> EditorAdapter<T>.notifyHide(type: T) {
    editorView?.hideChecked(type)
}

fun <T : EditorType> EditorAdapter<T>.notifyHideCurrent() {
    val editorView = editorView ?: return
    editorView.currentType?.let(editorView::hideChecked)
}

fun <T : EditorType> EditorAdapter<T>.notifyToggle(type: T) {
    val editorView = editorView ?: return
    if (editorView.currentType != type) {
        editorView.showChecked(type)
    } else {
        notifyShowIme()
    }
}

fun EditorAdapter<*>.notifyShowIme() {
    val editorView = editorView ?: return
    editorView.imeType?.let(editorView::showChecked)
}

fun EditorAdapter<*>.notifyHideIme() {
    val editorView = editorView ?: return
    editorView.imeType?.let(editorView::hideChecked)
}