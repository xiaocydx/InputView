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
import android.view.ViewTreeObserver
import androidx.annotation.CallSuper
import androidx.core.view.OneShotPreDrawListener
import java.lang.ref.WeakReference

/**
 * 显示和隐藏IME的Window焦点逻辑
 *
 * @author xcc
 * @date 2024/4/7
 */
internal open class ImeWindowFocus<V : View>(view: V) :
        WeakReference<V>(view), View.OnAttachStateChangeListener {
    private var host: EditorHost? = null
    private var hasFocusAction: OneShotHasFocusListener? = null
    protected var window: ViewTreeWindow? = null; private set

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

    @CallSuper
    override fun onViewAttachedToWindow(view: View) {
        window = window ?: view.requireViewTreeWindow()
    }

    @CallSuper
    override fun onViewDetachedFromWindow(view: View) {
        removePending()
    }

    open fun requestCurrentFocus() {
        val currentFocus = window?.currentFocus
        if (currentFocus == null) get()?.requestFocus()
    }

    open fun clearCurrentFocus() {
        val currentFocus = window?.currentFocus
        if (currentFocus == null) get()?.clearFocus()
    }

    fun showIme() {
        val view = get() ?: return
        // 确保Window具有焦点后，才能调用showIme()
        if (view.hasWindowFocus()) {
            removePending()
            view.ensureCanShowIme()
            window?.showIme(view)
        } else if (hasFocusAction == null) {
            hasFocusAction = OneShotHasFocusListener.add(view) {
                hasFocusAction = null
                view.ensureCanShowIme()
                window?.showIme(view)
            }
        }
    }

    fun hideIme() {
        removePending()
        window?.hideIme()
    }

    private fun removePending() {
        hasFocusAction?.removeListener()
        hasFocusAction = null
    }

    private fun View.ensureCanShowIme() {
        if (!isFocusable) isFocusable = true
        if (!isFocusableInTouchMode) isFocusableInTouchMode = true
    }
}

/**
 * 实现逻辑参考自[OneShotPreDrawListener]
 */
private class OneShotHasFocusListener private constructor(
    private val view: View,
    private val runnable: Runnable
) : ViewTreeObserver.OnWindowFocusChangeListener, View.OnAttachStateChangeListener {
    private var viewTreeObserver = view.viewTreeObserver

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            removeListener()
            runnable.run()
        }
    }

    fun removeListener() {
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.removeOnWindowFocusChangeListener(this)
        } else {
            view.viewTreeObserver.removeOnWindowFocusChangeListener(this)
        }
        view.removeOnAttachStateChangeListener(this)
    }

    override fun onViewAttachedToWindow(v: View) {
        viewTreeObserver = view.viewTreeObserver
    }

    override fun onViewDetachedFromWindow(v: View) {
        removeListener()
    }

    companion object {

        fun add(view: View, runnable: Runnable): OneShotHasFocusListener {
            val listener = OneShotHasFocusListener(view, runnable)
            view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
            view.addOnAttachStateChangeListener(listener)
            return listener
        }
    }
}