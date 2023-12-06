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
 * 通知显示[editor]，若当前[EditorAdapter]未关联[InputView]，则调用无效，
 * 多个[EditText]的焦点处理逻辑，详细解释可以看[InputView.editText]的注释。
 */
fun <T : Editor> EditorAdapter<T>.notifyShow(editor: T) {
    host?.showChecked(editor)
}

/**
 * 通知隐藏[editor]，若当前[EditorAdapter]未关联[InputView]，则调用无效，
 * 多个[EditText]的焦点处理逻辑，详细解释可以看[InputView.editText]的注释。
 */
fun <T : Editor> EditorAdapter<T>.notifyHide(editor: T) {
    host?.hideChecked(editor)
}

/**
 * 通知隐藏当前[Editor]，若当前[EditorAdapter]未关联[InputView]，则调用无效，
 * 多个[EditText]的焦点处理逻辑，详细解释可以看[InputView.editText]的注释。
 */
fun <T : Editor> EditorAdapter<T>.notifyHideCurrent() {
    current?.let(::notifyHide)
}

/**
 * 通知显示IME，若当前[EditorAdapter]未关联[InputView]，则调用无效，
 * 多个[EditText]的焦点处理逻辑，详细解释可以看[InputView.editText]的注释。
 */
fun EditorAdapter<*>.notifyShowIme() {
    host?.apply { ime?.let(::showChecked) }
}

/**
 * 通知隐藏IME，若当前[EditorAdapter]未关联[InputView]，则调用无效，
 * 多个[EditText]的焦点处理逻辑，详细解释可以看[InputView.editText]的注释。
 */
fun EditorAdapter<*>.notifyHideIme() {
    host?.apply { ime?.let(::hideChecked) }
}

/**
 * 通知切换显示[Editor]，若当前[EditorAdapter]未关联[InputView]，则调用无效，
 * 多个[EditText]的焦点处理逻辑，详细解释可以看[InputView.editText]的注释。
 *
 * 1. 当前未显示[editor]，则显示[editor]。
 * 2. 当前已显示[editor]，则显示IME。
 */
fun <T : Editor> EditorAdapter<T>.notifyToggle(editor: T) {
    if (current !== editor) notifyShow(editor) else notifyShowIme()
}