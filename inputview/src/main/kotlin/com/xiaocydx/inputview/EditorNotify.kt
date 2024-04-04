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

import android.widget.EditText

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