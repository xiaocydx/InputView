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
import android.view.View.OnAttachStateChangeListener
import android.widget.EditText
import androidx.core.view.OneShotPreDrawListener
import java.lang.ref.WeakReference

/**
 * [EditText]的持有类，负责处理焦点和显示IME
 *
 * @author xcc
 * @date 2023/1/18
 */
internal class EditTextHolder(editText: EditText) :
        WeakReference<EditText>(editText), OnAttachStateChangeListener {
    private var host: EditorHost? = null
    private var window: ViewTreeWindow? = null
    private var pendingShowIme = false
    private var preDrawAction: OneShotPreDrawListener? = null

    fun onAttachedToHost(host: EditorHost) {
        this.host = host
        get()?.addOnAttachStateChangeListener(this)
        get()?.takeIf { it.isAttachedToWindow }?.let(::onViewAttachedToWindow)
    }

    fun onDetachedFromHost(host: EditorHost) {
        this.host = null
        get()?.removeOnAttachStateChangeListener(this)
        get()?.let(::onViewDetachedFromWindow)
    }

    override fun onViewAttachedToWindow(v: View) {
        window = v.requireViewTreeWindow()
        if (!pendingShowIme) return
        // 兼容IME未跟ViewRootImpl的属性动画同步的问题
        preDrawAction = host?.addPreDrawAction {
            preDrawAction = null
            if (pendingShowIme) showIme()
        }
    }

    override fun onViewDetachedFromWindow(v: View) = removePending()

    fun requestCurrentFocus() {
        window?.currentFocus?.requestFocus() ?: get()?.requestFocus()
    }

    fun clearCurrentFocus() {
        window?.currentFocus?.clearFocus() ?: get()?.clearFocus()
    }

    fun showIme() {
        val editText = get() ?: return
        pendingShowIme = !editText.isAttachedToWindow
        if (!pendingShowIme) window?.showIme(editText)
    }

    fun hideIme() {
        removePending()
        window?.hideIme()
    }

    private fun removePending() {
        pendingShowIme = false
        preDrawAction?.removeListener()
        preDrawAction = null
    }
}