package com.xiaocydx.inputview

import android.view.View
import android.view.ViewGroup

/**
 * [InputView]编辑区的编辑器适配器，负责创建和通知显示[Editor]的视图
 *
 * ```
 * enum class MessageEditor : Editor {
 *     IME, EDITOR_A, EDITOR_B
 * }
 *
 * class MessageEditorAdapter : EditorAdapter<MessageEditor>() {
 *     override val editors: List<MessageEditor> = listOf(
 *         MessageEditor.IME, MessageEditor.EDITOR_A, MessageEditor.EDITOR_B
 *     )
 *
 *     override fun isIme(editor: MessageEditor): Boolean {
 *         return editor == MessageEditor.IME
 *     }
 *
 *     override fun onCreateView(parent: ViewGroup, editor: MessageEditor): View? {
 *         return when(editor) {
 *             MessageEditor.IME -> null
 *             MessageEditor.EDITOR_A -> View(parent.content)
 *             MessageEditor.EDITOR_B -> View(parent.content)
 *         }
 *     }
 * }
 * ```
 *
 * @author xcc
 * @date 2023/1/8
 */
abstract class EditorAdapter<T : Editor> {
    private var listeners = ArrayList<EditorVisibleListener<T>>(2)
    internal var inputView: InputView? = null
        private set
    internal var editorView: EditorView? = null
        private set

    /**
     * [InputView]编辑区的编辑器集合
     */
    abstract val editors: List<T>

    /**
     * [editor]是否为IME
     *
     * **注意**：[editors]只能有一个[Editor]表示IME，否则初始化时抛出异常。
     */
    abstract fun isIme(editor: T): Boolean

    /**
     * 创建[editor]的视图，返回`null`表示不需要视图，例如IME或者语音消息
     */
    abstract fun onCreateView(parent: ViewGroup, editor: T): View?

    /**
     * 当前[EditorAdapter]添加到[inputView]
     */
    protected fun onAttachToInputView(inputView: InputView) = Unit

    /**
     * 当前[EditorAdapter]从[inputView]移除
     */
    protected fun onDetachFromInputView(inputView: InputView) = Unit

    /**
     * 添加[EditorVisibleListener]
     *
     * 在[EditorVisibleListener.onVisibleChanged]可以调用[removeEditorVisibleListener]。
     */
    fun addEditorVisibleListener(listener: EditorVisibleListener<T>) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    /**
     * 移除[EditorVisibleListener]
     */
    fun removeEditorVisibleListener(listener: EditorVisibleListener<T>) {
        listeners.remove(listener)
    }

    internal fun attach(inputView: InputView, editorView: EditorView) {
        this.inputView = inputView
        this.editorView = editorView
        onAttachToInputView(inputView)
    }

    internal fun detach(inputView: InputView, editorView: EditorView) {
        assert(this.inputView === inputView) { "InputView不相同" }
        assert(this.editorView === editorView) { "EditorView不相同" }
        this.inputView = null
        this.editorView = null
        onDetachFromInputView(inputView)
    }

    internal fun onVisibleChanged(previous: T?, current: T?) {
        for (index in listeners.indices.reversed()) {
            listeners[index].onVisibleChanged(previous, current)
        }
    }
}

/**
 * [InputView]编辑区的编辑器
 *
 * 推荐用`enum class`或者`sealed class `实现[Editor]，例如：
 * ```
 * enum class MessageEditor : Editor {
 *     IME, EDITOR_A, EDITOR_B
 * }
 * ```
 */
interface Editor

/**
 * [InputView]编辑区显示的[Editor]更改监听
 */
fun interface EditorVisibleListener<in T : Editor> {

    /**
     * 显示的[Editor]已更改
     *
     * @param previous 之前显示的[Editor]，`null`表示之前没有显示[Editor]
     * @param current  当前显示的[Editor]，`null`表示当前没有显示[Editor]
     */
    fun onVisibleChanged(previous: T?, current: T?)
}