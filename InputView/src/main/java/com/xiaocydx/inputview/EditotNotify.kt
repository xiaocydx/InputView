package com.xiaocydx.inputview

/**
 * 当前显示的[Editor]，若为`null`，则当前是初始化状态
 */
@Suppress("UNCHECKED_CAST")
val <T : Editor> EditorAdapter<T>.current: T?
    get() = editorView?.current as? T

/**
 * 当前显示的[Editor]是否为[editor]
 */
fun <T : Editor> EditorAdapter<T>.isShowing(editor: T): Boolean = current === editor

/**
 * 通知显示[editor]，若未对[InputView]设置当前[EditorAdapter]，则调用无效
 */
fun <T : Editor> EditorAdapter<T>.notifyShow(editor: T) {
    editorView?.showChecked(editor)
}

/**
 * 通知隐藏[editor]，若未对[InputView]设置当前[EditorAdapter]，则调用无效
 */
fun <T : Editor> EditorAdapter<T>.notifyHide(editor: T) {
    editorView?.hideChecked(editor)
}

/**
 * 通知隐藏当前[Editor]，若未对[InputView]设置当前[EditorAdapter]，则调用无效
 */
fun <T : Editor> EditorAdapter<T>.notifyHideCurrent() {
    current?.let(::notifyHide)
}

/**
 * 通知显示IME，若未对[InputView]设置当前[EditorAdapter]，则调用无效
 */
fun EditorAdapter<*>.notifyShowIme() {
    editorView?.apply { ime?.let(::showChecked) }
}

/**
 * 通知隐藏IME，若未对[InputView]设置当前[EditorAdapter]，则调用无效
 */
fun EditorAdapter<*>.notifyHideIme() {
    editorView?.apply { ime?.let(::hideChecked) }
}

/**
 * 通知切换显示[Editor]，若未对[InputView]设置当前[EditorAdapter]，则调用无效
 *
 * 1. 当前未显示[editor]，则显示[editor]。
 * 2. 当前已显示[editor]，则显示IME。
 */
fun <T : Editor> EditorAdapter<T>.notifyToggle(editor: T) {
    if (current !== editor) notifyShow(editor) else notifyShowIme()
}