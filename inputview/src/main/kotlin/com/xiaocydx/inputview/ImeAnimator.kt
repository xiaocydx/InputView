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
 * 复用[InputView]的动画实现，需要调用`InputView.init()`完成初始化
 *
 * ```
 * InputView.init(window)
 * val animator = InputView.animator(editText)
 * animator.addAnimationCallback(
 *     onPrepare = { previous, current ->
 *         // 显示和隐藏IME，处理editText的焦点，覆盖默认实现
 *     }
 *     onUpdate = { state ->
 *         // 显示和隐藏IME，运行动画平移contentView
 *         contentView.translationY = -state.currentOffset.toFloat()
 *     }
 * )
 * ```
 *
 * @param editText 首次按[durationMillis]和[interpolator]创建[EditorAnimator]，
 * 后续获取的是首次创建的[EditorAnimator]，显示IME会获得焦点，隐藏IME会清除焦点，
 * 利用[EditText.setOnTouchListener]，解决水滴状指示器导致动画卡顿的问题。
 * @param durationMillis 含义等同于[EditorAnimator.durationMillis]
 * @param interpolator   含义等同于[EditorAnimator.interpolator]
 */
@CheckResult
fun InputView.Companion.animator(
    editText: EditText,
    durationMillis: Long = ANIMATION_DURATION_MILLIS,
    interpolator: Interpolator = ANIMATION_INTERPOLATOR
): ImeAnimator {
    val key = R.id.tag_view_ime_animator
    var animator = editText.getTag(key) as? ImeAnimator
    if (animator == null) {
        animator = ImeAnimator(editText, durationMillis, interpolator)
        editText.setTag(key, animator)
    }
    return animator
}

class ImeAnimator internal constructor(
    editText: EditText,
    durationMillis: Long,
    interpolator: Interpolator
) : EditorAnimator(durationMillis, interpolator) {
    private val host = EditorHostImpl(editText)
    private val holder = EditTextHolder(editText)

    init {
        setAnimationCallback(EditorOffsetUpdater())
        editText.setOnTouchListener { _, event ->
            holder.beforeTouchEvent(event)
            val consumed = editText.onTouchEvent(event)
            holder.afterTouchEvent(event)
            consumed
        }
        val listener = AttachStateChangeListenerImpl()
        editText.addOnAttachStateChangeListener(listener)
        editText.takeIf { it.isAttachedToWindow }?.let(listener::onViewAttachedToWindow)
    }

    fun showIme() {
        holder.showIme()
    }

    fun hideIme() {
        holder.hideIme()
    }

    private inner class EditorOffsetUpdater : AnimationCallback {
        override fun onAnimationPrepare(previous: Editor?, current: Editor?) {
            if (current != null) holder.requestFocus() else holder.clearFocus()
        }

        override fun onAnimationUpdate(state: AnimationState) {
            state.apply { updateEditorOffset(currentOffset) }
        }
    }

    private inner class AttachStateChangeListenerImpl : OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            val window = v.requireViewTreeWindow()
            host.onAttachedToWindow(window)
            holder.onAttachToEditorHost(host)
            this@ImeAnimator.onAttachToEditorHost(host)
        }

        override fun onViewDetachedFromWindow(v: View) {
            holder.onDetachFromEditorHost(host)
            this@ImeAnimator.onDetachFromEditorHost(host)
        }
    }

    private inner class EditorHostImpl(private val view: View) : EditorHost {
        private var window: ViewTreeWindow? = null
        override val WindowInsetsCompat.imeHeight: Int
            get() = window?.run { imeHeight } ?: NO_VALUE
        override val WindowInsetsCompat.imeOffset: Int
            get() = window?.run { imeOffset } ?: NO_VALUE
        override var editorOffset = 0
        override var navBarOffset = 0
        override val ime = Ime
        override var current: Ime? = null
        override val container = null
        override val previousView = null
        override val currentView = null

        fun onAttachedToWindow(window: ViewTreeWindow) {
            this.window = window
        }

        override fun updateEditorOffset(offset: Int) {
            editorOffset = offset
        }

        override fun dispatchImeShown(shown: Boolean) {
            val previous = current
            current = if (shown) ime else null
            this@ImeAnimator.onPendingChanged(previous, current)
        }

        override fun addAnimationCallback(callback: AnimationCallback) {
            this@ImeAnimator.addAnimationCallback(callback)
        }

        override fun removeAnimationCallback(callback: AnimationCallback) {
            this@ImeAnimator.removeAnimationCallback(callback)
        }

        override fun addPreDrawAction(action: () -> Unit): OneShotPreDrawListener {
            return OneShotPreDrawListener.add(view, action)
        }

        override fun setOnApplyWindowInsetsListener(listener: OnApplyWindowInsetsListenerCompat?) {
            val wrapper = if (listener == null) null else OnApplyWindowInsetsListenerCompat { v, insets ->
                navBarOffset = window?.run { insets.navigationBarOffset } ?: 0
                listener.onApplyWindowInsets(v, insets)
            }
            if (wrapper == null) navBarOffset = 0
            ReflectCompat { view.setOnApplyWindowInsetsListenerImmutable(wrapper) }
        }

        override fun setWindowInsetsAnimationCallback(
            durationMillis: Long,
            interpolator: Interpolator,
            callback: WindowInsetsAnimationCompat.Callback?
        ) {
            val window = window ?: return
            if (callback == null) {
                window.restoreImeAnimation()
            } else {
                window.modifyImeAnimation(durationMillis, interpolator)
            }
            ReflectCompat { view.setWindowInsetsAnimationCallbackImmutable(callback) }
        }

        override fun removeEditorView(view: View) = Unit
        override fun showChecked(editor: Editor) = Unit
        override fun hideChecked(editor: Editor) = Unit
        override fun addEditorChangedListener(listener: EditorChangedListener<Editor>) = Unit
        override fun removeEditorChangedListener(listener: EditorChangedListener<Editor>) = Unit
    }
}