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

package com.xiaocydx.inputview.compat

import android.view.View
import android.view.Window
import androidx.core.view.ViewCompat

/**
 * 提供[attach]和[detach]的基类
 *
 * @author xcc
 * @date 2023/5/2
 */
internal abstract class WindowAttachCompat(protected val window: Window) {
    private var isAttached = false
    private var listener: View.OnAttachStateChangeListener? = null
    protected val decorView = window.decorView

    fun attach() {
        if (isAttached) return
        isAttached = true
        doOnAttach { onAttach() }
    }

    fun detach() {
        if (!isAttached) return
        isAttached = false
        removeListener()
        onDetach()
    }

    fun reattach() {
        if (listener != null) return
        isAttached = true
        doOnAttach { onAttach() }
    }

    protected open fun onAttach() = Unit

    protected open fun onDetach() = Unit

    private inline fun doOnAttach(crossinline action: () -> Unit) {
        if (ViewCompat.isAttachedToWindow(decorView)) {
            action()
        } else {
            listener = object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) {
                    removeListener()
                    action()
                }

                override fun onViewDetachedFromWindow(view: View) = Unit
            }
            listener?.let(decorView::addOnAttachStateChangeListener)
        }
    }

    private fun removeListener() {
        listener?.let(decorView::removeOnAttachStateChangeListener)
        listener = null
    }
}