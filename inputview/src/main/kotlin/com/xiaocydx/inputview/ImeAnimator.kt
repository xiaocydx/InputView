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

@file:SuppressLint("ClickableViewAccessibility")

package com.xiaocydx.inputview

import android.annotation.SuppressLint
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.Window
import android.view.animation.Interpolator
import android.widget.EditText
import androidx.annotation.CheckResult
import androidx.core.view.OneShotPreDrawListener
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import com.xiaocydx.inputview.EditorAnimator.Companion.ANIMATION_DURATION_MILLIS
import com.xiaocydx.inputview.EditorAnimator.Companion.ANIMATION_INTERPOLATOR
import com.xiaocydx.inputview.compat.ReflectCompat

/**
 * 复用[InputView]的动画实现，需要先调用[init]或[initCompat]完成初始化，
 * 该函数不能跟[InputView]一直使用，两者不是共存关系，只能选择其中一个。
 *
 * ```
 * InputView.init(window)
 * val animator = InputView.animator(window, editText)
 * animator.addAnimationCallback(
 *     onPrepare = { previous, current ->
 *         // 显示和隐藏IME，处理editText的焦点，覆盖默认实现
 *     },
 *     onUpdate = { state ->
 *         // 显示和隐藏IME，运行动画平移contentView
 *         contentView.translationY = -state.currentOffset.toFloat()
 *     }
 * )
 * ```
 *
 * @param editText 首次按[durationMillis]和[interpolator]创建[EditorAnimator]，
 * 后续获取的是首次创建的[EditorAnimator]，显示IME会获得焦点，隐藏IME会清除焦点。
 * @param durationMillis 参数含义跟[EditorAnimator.durationMillis]一致
 * @param interpolator   参数含义跟[EditorAnimator.interpolator]一致
 */
@CheckResult
fun InputView.Companion.animator(
    window: Window,
    editText: EditText,
    durationMillis: Long = ANIMATION_DURATION_MILLIS,
    interpolator: Interpolator = ANIMATION_INTERPOLATOR
): ImeAnimator {
    val key = R.id.tag_view_ime_animator
    val viewTreeWindow = window.decorView.requireViewTreeWindow()
    var animator = editText.getTag(key) as? ImeAnimator
    if (animator == null) {
        animator = ImeAnimator(viewTreeWindow, editText, durationMillis, interpolator)
        editText.setTag(key, animator)
    }
    return animator
}

class ImeAnimator internal constructor(
    private val window: ViewTreeWindow,
    private val editText: EditText,
    durationMillis: Long,
    interpolator: Interpolator
) : EditorAnimator(durationMillis, interpolator) {
    private val host = EditorHostImpl()
    private val holder = EditTextHolder(editText)

    init {
        setAnimationCallback(object : AnimationCallback {
            override fun onAnimationUpdate(state: AnimationState) {
                state.updateEditorOffset(state.currentOffset)
            }
        })
        val listener = object : OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                host.onAttachedToWindow()
                holder.onAttachedToHost(host)
                this@ImeAnimator.onAttachedToHost(host)
            }

            override fun onViewDetachedFromWindow(v: View) {
                host.onDetachedFromWindow()
                holder.onDetachedFromHost(host)
                this@ImeAnimator.onDetachedFromHost(host)
            }
        }
        editText.addOnAttachStateChangeListener(listener)
        editText.takeIf { it.isAttachedToWindow }?.let(listener::onViewAttachedToWindow)
    }

    fun showIme() {
        holder.showIme()
    }

    fun hideIme() {
        holder.hideIme()
    }

    private inner class EditorHostImpl : EditorHost {
        override val WindowInsetsCompat.imeHeight: Int
            get() = window.run { imeHeight }
        override val WindowInsetsCompat.imeOffset: Int
            get() = window.run { imeOffset }
        override var editorOffset = 0
        override var navBarOffset = 0
        override val ime = Ime
        override var current: Ime? = null
        override val container = null
        override val previousView = null
        override val currentView = null

        fun onAttachedToWindow() {
            window.register(this)
            window.addEditText(editText)
        }

        fun onDetachedFromWindow() {
            window.unregister(this)
            window.removeEditText(editText)
        }

        override fun updateEditorOffset(offset: Int) {
            editorOffset = offset
        }

        override fun dispatchImeShown(shown: Boolean) {
            val previous = current
            current = if (shown) ime else null
            if (shown) holder.requestCurrentFocus() else holder.clearCurrentFocus()
            this@ImeAnimator.onPendingChanged(previous, current)
        }

        override fun addAnimationCallback(callback: AnimationCallback) {
            this@ImeAnimator.addAnimationCallback(callback)
        }

        override fun removeAnimationCallback(callback: AnimationCallback) {
            this@ImeAnimator.removeAnimationCallback(callback)
        }

        override fun addPreDrawAction(action: () -> Unit): OneShotPreDrawListener {
            return OneShotPreDrawListener.add(editText, action)
        }

        override fun setOnApplyWindowInsetsListener(listener: OnApplyWindowInsetsListenerCompat?) {
            val wrapper = if (listener == null) null else OnApplyWindowInsetsListenerCompat { v, insets ->
                navBarOffset = window.run { insets.navigationBarOffset }
                listener.onApplyWindowInsets(v, insets)
            }
            if (wrapper == null) navBarOffset = 0
            ReflectCompat { editText.setOnApplyWindowInsetsListenerImmutable(wrapper) }
        }

        override fun setWindowInsetsAnimationCallback(
            durationMillis: Long,
            interpolator: Interpolator,
            callback: WindowInsetsAnimationCompat.Callback?
        ) {
            if (callback == null) {
                window.restoreImeAnimation()
            } else {
                window.modifyImeAnimation(durationMillis, interpolator)
            }
            ReflectCompat { editText.setWindowInsetsAnimationCallbackImmutable(callback) }
        }

        override fun removeEditorView(view: View) = Unit
        override fun showChecked(editor: Editor) = Unit
        override fun hideChecked(editor: Editor) = Unit
        override fun addEditorChangedListener(listener: EditorChangedListener<Editor>) = Unit
        override fun removeEditorChangedListener(listener: EditorChangedListener<Editor>) = Unit
    }
}