package com.xiaocydx.inputview

import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper

/**
 * [InputView]编辑区的[Editor]适配器，负责创建和通知显示[Editor]的视图
 *
 * ```
 * enum class MessageEditor : Editor {
 *     IME, VOICE, EMOJI
 * }
 *
 * class MessageEditorAdapter : EditorAdapter<MessageEditor>() {
 *     override val ime: MessageEditor = IME
 *
 *     override fun onCreateView(parent: ViewGroup, editor: MessageEditor): View? {
 *         return when(editor) {
 *             IME -> null
 *             VOICE -> VoiceView(parent.context)
 *             EMOJI -> EmojiView(parent.context)
 *         }
 *     }
 * }
 * ```
 *
 * @author xcc
 * @date 2023/1/8
 */
abstract class EditorAdapter<T : Editor> {
    private val listeners = ArrayList<EditorChangedListener<T>>(2)
    internal var host: EditorHost? = null
        private set

    /**
     * 表示IME的`editor`
     */
    abstract val ime: T

    /**
     * 创建[editor]的视图，返回`null`表示不需要视图，当[editor]表示IME时，该函数不会被调用
     */
    abstract fun onCreateView(parent: ViewGroup, editor: T): View?

    /**
     * 添加[EditorChangedListener]
     *
     * 在[EditorChangedListener.onEditorChanged]可以调用[removeEditorChangedListener]。
     */
    fun addEditorChangedListener(listener: EditorChangedListener<T>) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    /**
     * 移除[EditorChangedListener]
     */
    fun removeEditorChangedListener(listener: EditorChangedListener<T>) {
        listeners.remove(listener)
    }

    @CallSuper
    internal open fun onAttachToEditorHost(host: EditorHost) {
        this.host = host
    }

    @CallSuper
    internal open fun onDetachFromEditorHost(host: EditorHost) {
        this.host = null
    }

    @Suppress("UNCHECKED_CAST")
    internal fun forEachListener(action: (EditorChangedListener<Editor>) -> Unit) {
        listeners.forEach { action(it as EditorChangedListener<Editor>) }
    }

    internal fun onEditorChanged(previous: T?, current: T?) {
        for (index in listeners.indices.reversed()) {
            listeners[index].onEditorChanged(previous, current)
        }
    }
}

/**
 * [InputView]编辑区的编辑器
 *
 * 推荐用`enum class`或`sealed class`实现[Editor]，例如：
 * ```
 * enum class MessageEditor : Editor {
 *     IME, VOICE, EMOJI
 * }
 * ```
 *
 * 用`enum class`或`sealed class`能更好的进行模式匹配，
 * 也正是因为如此，IME没有作为内部实现，而是开放了出来。
 */
interface Editor

/**
 * [InputView]编辑区更改[Editor]的监听
 */
fun interface EditorChangedListener<in T : Editor> {

    /**
     * 显示的[Editor]已更改
     *
     * @param previous 之前的[Editor]，`null`表示之前没有[Editor]
     * @param current  当前的[Editor]，`null`表示当前没有[Editor]
     */
    fun onEditorChanged(previous: T?, current: T?)
}