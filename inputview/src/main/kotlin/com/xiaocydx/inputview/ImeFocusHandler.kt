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
import androidx.annotation.CallSuper
import com.xiaocydx.insets.OneShotHasWindowFocusListener
import java.lang.ref.WeakReference

/**
 * 显示和隐藏IME的Window焦点处理器
 *
 * @author xcc
 * @date 2024/4/7
 */
internal open class ImeFocusHandler(view: View) :
        WeakReference<View>(view), View.OnAttachStateChangeListener {
    private var isAttached = false
    private var focusAction: OneShotHasWindowFocusListener? = null
    protected var window: ViewTreeWindow? = null; private set

    fun onAttachedToHost(host: EditorHost) {
        if (isAttached) return
        isAttached = true
        get()?.addOnAttachStateChangeListener(this)
        get()?.takeIf { it.isAttachedToWindow }?.let(::onViewAttachedToWindow)
    }

    fun onDetachedFromHost(host: EditorHost) {
        if (!isAttached) return
        isAttached = false
        get()?.removeOnAttachStateChangeListener(this)
        get()?.let(::onViewDetachedFromWindow)
    }

    @CallSuper
    override fun onViewAttachedToWindow(view: View) {
        window = window ?: view.findViewTreeWindow()
    }

    @CallSuper
    override fun onViewDetachedFromWindow(view: View) {
        removePending()
    }

    fun requestFocus() {
        get()?.requestFocusCompat()
    }

    open fun requestCurrentFocus() {
        val currentFocus = window?.currentFocus
        if (currentFocus == null) get()?.requestFocusCompat()
    }

    open fun clearCurrentFocus() {
        val currentFocus = window?.currentFocus
        if (currentFocus == null) get()?.clearFocus()
    }

    fun showIme() {
        val view = (window?.currentFocus ?: get()) ?: return
        if (view === get()) view.requestFocusCompat()
        // 确保Window具有焦点后，才能调用showIme()
        if (view.hasWindowFocus()) {
            removePending()
            window?.showIme(view)
        } else if (focusAction == null) {
            focusAction = OneShotHasWindowFocusListener.add(view) {
                focusAction = null
                window?.showIme(view)
            }
        }
    }

    fun hideIme() {
        removePending()
        window?.hideIme()
    }

    private fun removePending() {
        focusAction?.removeListener()
        focusAction = null
    }

    protected fun View.requestFocusCompat() {
        if (!isFocusable) isFocusable = true
        if (!isFocusableInTouchMode) isFocusableInTouchMode = true
        if (!hasFocus()) requestFocus()
    }
}