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
import android.view.Window
import android.view.animation.Interpolator
import android.widget.EditText
import androidx.annotation.CheckResult
import androidx.annotation.VisibleForTesting
import androidx.core.view.OneShotPreDrawListener
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import com.xiaocydx.inputview.EditorAnimator.Companion.ANIMATION_DURATION_MILLIS
import com.xiaocydx.inputview.EditorAnimator.Companion.ANIMATION_INTERPOLATOR
import com.xiaocydx.inputview.compat.ReflectCompat

/**
 * 复用[InputView]的动画实现，需要先调用[init]或[initCompat]完成初始化，
 * 该函数不能跟[InputView]一起使用，两者不是共存关系，只能选择其中一个。
 *
 * 多个[EditText]的焦点处理逻辑：
 * 1. 调用[ImeAnimator.showIme]显示IME，会让[editText]获得焦点。
 * 2. 调用[ImeAnimator.hideIme]隐藏IME，或者通过其它方式隐藏IME，
 * 会清除`currentFocus`的焦点，`currentFocus`不一定是[editText]。
 *
 * 首次调用按[durationMillis]和[interpolator]创建[ImeAnimator]，
 * 后续调用获取的是首次调用创建的[ImeAnimator]：
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
 * @param editText       用于兼容Android各版本显示IME的[EditText]
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
        if (canChangeEditor(host.current, host.ime)) holder.showIme()
    }

    fun hideIme() {
        if (canChangeEditor(host.current, null)) holder.hideIme()
    }

    private inner class EditorHostImpl : EditorHost {
        override val WindowInsetsCompat.imeOffset: Int
            get() = window.run { imeOffset }
        override val hasWindowFocus: Boolean
            get() = window.hasWindowFocus
        override var editorOffset = 0
        override var navBarOffset = 0
        override val ime = Ime
        override var current: Ime? = null
        override val container = null
        override val previousView = null
        override val currentView = null

        fun onAttachedToWindow() {
            window.registerHost(this)
        }

        fun onDetachedFromWindow() {
            window.unregisterHost(this)
        }

        override fun updateEditorOffset(offset: Int) {
            editorOffset = offset
        }

        override fun dispatchImeShown(shown: Boolean): Boolean {
            val next = if (shown) ime else null
            if (!canChangeEditor(current, next)) return false
            val previous = current
            current = next
            if (shown) holder.requestCurrentFocus() else holder.clearCurrentFocus()
            this@ImeAnimator.onPendingChanged(previous, current, immediately = false)
            return true
        }

        override fun addAnimationCallback(callback: AnimationCallback) {
            this@ImeAnimator.addAnimationCallback(callback)
        }

        override fun removeAnimationCallback(callback: AnimationCallback) {
            this@ImeAnimator.removeAnimationCallback(callback)
        }

        @VisibleForTesting
        override fun containsAnimationCallback(callback: AnimationCallback): Boolean {
            return this@ImeAnimator.containsCallback(callback)
        }

        override fun addPreDrawAction(action: () -> Unit): OneShotPreDrawListener {
            return OneShotPreDrawListener.add(editText, action)
        }

        override fun setOnApplyWindowInsetsListener(listener: OnApplyWindowInsetsListenerCompat?) {
            val wrapper = if (listener == null) null else OnApplyWindowInsetsListenerCompat { v, insets ->
                navBarOffset = window.run { insets.navBarOffset }
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
        override fun showChecked(editor: Editor) = false
        override fun hideChecked(editor: Editor) = false
    }
}