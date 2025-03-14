/*
 * Copyright 2023 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaocydx.inputview

import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.CallSuper

/**
 * [Editor]的适配器，负责创建和通知显示[Editor]的视图
 *
 * ```
 * enum class MessageEditor : Editor {
 *     Ime, Voice, Emoji
 * }
 *
 * class MessageEditorAdapter : EditorAdapter<MessageEditor>() {
 *     override val ime = MessageEditor.Ime
 *
 *     override fun onCreateView(parent: ViewGroup, editor: MessageEditor): View? {
 *         return when(editor) {
 *             MessageEditor.Ime -> null
 *             MessageEditor.Voice -> VoiceView(parent.context)
 *             MessageEditor.Emoji -> EmojiView(parent.context)
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
     * 在保存和恢复显示的[Editor]时，会调用该函数获取可保存显示状态的[Editor]集合，
     * 恢复显示的[Editor]不会运行动画，仅记录动画状态，分发动画回调，在恢复的过程中，
     * 会调用[EditorChangedListener]、[AnimationCallback]、[AnimationInterceptor]。
     *
     * **注意**：
     * 1. 重写该函数只会恢复显示的[Editor]，不会恢复[Editor]视图的状态，
     * 若需要恢复[Editor]视图的状态，则可以使用[FragmentEditorAdapter]。
     * 2. 即使不重写该函数，在页面重建期间通知显示[Editor]，也不会运行动画，
     * 可以类比Fragment首次创建有过渡动画，重建的Fragment不会运行过渡动画。
     */
    open fun getStatefulEditorList(): List<T> = emptyList()

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
    internal open fun onAttachedToHost(host: EditorHost) {
        check(this.host == null) { "EditorAdapter已关联EditorHost" }
        this.host = host
    }

    @CallSuper
    internal open fun onDetachedFromHost(host: EditorHost) {
        this.host = null
    }

    @Suppress("UNCHECKED_CAST")
    internal fun forEachListener(action: (EditorChangedListener<Editor>) -> Unit) {
        listeners.forEach { action(it as EditorChangedListener<Editor>) }
    }

    internal fun dispatchChanged(previous: T?, current: T?, invalidated: DispatchInvalidated) {
        for (index in listeners.indices.reversed()) {
            if (invalidated()) break
            listeners[index].onEditorChanged(previous, current)
        }
    }
}

internal typealias DispatchInvalidated = () -> Boolean

/**
 * [InputView]编辑区的编辑器
 *
 * 推荐用`enum class`或`sealed class`实现[Editor]，例如：
 * ```
 * enum class MessageEditor : Editor {
 *     Ime, Voice, Emoji
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

/**
 * 当前显示的[Editor]，若为`null`，则当前是初始化状态
 */
@Suppress("UNCHECKED_CAST")
val <T : Editor> EditorAdapter<T>.current: T?
    get() = host?.current as? T

/**
 * 当前显示的[Editor]是否为[editor]
 */
fun <T : Editor> EditorAdapter<T>.isShowing(editor: T): Boolean = current === editor

/**
 * 通知显示[editor]，多个[EditText]的焦点处理逻辑，详细解释可以看[InputView.editText]的注释
 *
 * @return `true`-显示[editor]成功，`false`-未关联[InputView]、已显示[editor]、显示[editor]被拦截。
 */
fun <T : Editor> EditorAdapter<T>.notifyShow(editor: T): Boolean {
    return host?.showChecked(editor) ?: false
}

/**
 * 通知隐藏[editor]，多个[EditText]的焦点处理逻辑，详细解释可以看[InputView.editText]的注释
 *
 * @return `true`-隐藏[editor]成功，`false`-未关联[InputView]、已隐藏[editor]、隐藏[editor]被拦截。
 */
fun <T : Editor> EditorAdapter<T>.notifyHide(editor: T): Boolean {
    return host?.hideChecked(editor) ?: false
}

/**
 * 通知隐藏当前[Editor]，多个[EditText]的焦点处理逻辑，详细解释可以看[InputView.editText]的注释
 *
 * @return `true`-隐藏[current]成功，`false`-未关联[InputView]、已隐藏[current]、隐藏[current]被拦截。
 */
fun <T : Editor> EditorAdapter<T>.notifyHideCurrent(): Boolean {
    return current?.let(::notifyHide) ?: false
}

/**
 * 通知显示IME，多个[EditText]的焦点处理逻辑，详细解释可以看[InputView.editText]的注释
 *
 * @return `true`-显示IME成功，`false`-未关联[InputView]、已显示IME、显示IME被拦截。
 */
fun EditorAdapter<*>.notifyShowIme(): Boolean {
    return host?.run { ime?.let(::showChecked) } ?: false
}

/**
 * 通知隐藏IME，多个[EditText]的焦点处理逻辑，详细解释可以看[InputView.editText]的注释
 *
 * @return `true`-隐藏IME成功，`false`-未关联[InputView]、已隐藏IME、隐藏IME被拦截。
 */
fun EditorAdapter<*>.notifyHideIme(): Boolean {
    return host?.run { ime?.let(::hideChecked) } ?: false
}

/**
 * 通知切换显示[Editor]，多个[EditText]的焦点处理逻辑，详细解释可以看[InputView.editText]的注释
 *
 * 1. 当前未显示[editor]，则显示[editor]。
 * 2. 当前已显示[editor]，则显示IME。
 *
 * @return `true`-切换显示[editor]成功，`false`-未关联[InputView]、切换显示[editor]被拦截。
 */
fun <T : Editor> EditorAdapter<T>.notifyToggle(editor: T): Boolean {
    return if (current !== editor) notifyShow(editor) else notifyShowIme()
}