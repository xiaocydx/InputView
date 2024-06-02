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
import com.xiaocydx.insets.requestApplyInsetsCompat

/**
 * 复用[InputView]的动画实现，需要先调用[init]或[initCompat]完成初始化，
 * 该函数不能跟[InputView]一起使用，两者不是共存关系，只能使用其中一个。
 *
 * 若[view]的类型是[EditText]，则直接设置为[ImeAnimator.editText]，
 * 多个[EditText]的焦点处理逻辑可以看[ImeAnimator.editText]的注释。
 *
 * 首次调用按[durationMillis]和[interpolator]创建[ImeAnimator]，
 * 后续调用获取的是首次调用创建的[ImeAnimator]：
 * ```
 * InputView.init(window)
 * val animator = InputView.animator(view)
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
 * @param view           [ImeAnimator]的生命周期跟[view]一致
 * @param durationMillis 参数含义跟[EditorAnimator.durationMillis]一致
 * @param interpolator   参数含义跟[EditorAnimator.interpolator]一致
 */
@CheckResult
fun InputView.Companion.animator(
    view: View,
    durationMillis: Long = ANIMATION_DURATION_MILLIS,
    interpolator: Interpolator = ANIMATION_INTERPOLATOR
): ImeAnimator {
    val key = R.id.tag_view_ime_animator
    var animator = view.getTag(key) as? ImeAnimator
    if (animator == null) {
        animator = ImeAnimator(view, durationMillis, interpolator)
        view.setTag(key, animator)
    }
    return animator
}

@Deprecated(
    message = "不再需要Window参数和限制EditText类型",
    replaceWith = ReplaceWith("animator(editText, durationMillis, interpolator)")
)
@CheckResult
fun InputView.Companion.animator(
    window: Window,
    editText: EditText,
    durationMillis: Long = ANIMATION_DURATION_MILLIS,
    interpolator: Interpolator = ANIMATION_INTERPOLATOR
): ImeAnimator = animator(editText, durationMillis, interpolator)

class ImeAnimator internal constructor(
    private val view: View,
    durationMillis: Long,
    interpolator: Interpolator
) : EditorAnimator(durationMillis, interpolator) {
    private val host = EditorHostImpl()
    private var window: ViewTreeWindow? = null
    private var imeFocusHandler = when (view) {
        is EditText -> EditTextHolder(view)
        else -> ImeFocusHandler(view)
    }

    /**
     * 设置需要处理的[EditText]
     *
     * ### 多个[EditText]的焦点处理逻辑
     * 1. 调用[showIme]显示IME，会让[editText]获得焦点。
     * 2. 调用[hideIme]隐藏IME，或者通过其它方式隐藏IME，
     * 会清除`currentFocus`的焦点，`currentFocus`不一定是[editText]。
     *
     * ### 处理[editText]的水滴状指示器
     * 对[editText]设置的[EditText]会自动处理水滴状指示器导致动画卡顿的问题，
     * 若其它[EditText]也需要处理，则调用`InputView.addEditText()`完成添加。
     */
    var editText: EditText?
        get() = imeFocusHandler.get() as? EditText
        set(value) {
            val previous = imeFocusHandler
            if (previous.get() === value) return
            val current = when {
                value != null -> EditTextHolder(value)
                else -> ImeFocusHandler(view)
            }
            host.onImeFocusHandlerChanged(previous, current)
            imeFocusHandler = current
        }

    init {
        host.onImeFocusHandlerChanged(previous = null, imeFocusHandler)
        setAnimationCallback(object : AnimationCallback {
            override fun onAnimationUpdate(state: AnimationState) {
                state.updateEditorOffset(state.currentOffset)
            }
        })
        val listener = object : OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                window = window ?: view.requireViewTreeWindow()
                window?.let(host::onAttachedToWindow)
                view.requestApplyInsetsCompat()
            }

            override fun onViewDetachedFromWindow(v: View) {
                window?.let(host::onDetachedFromWindow)
            }
        }
        view.addOnAttachStateChangeListener(listener)
        view.takeIf { it.isAttachedToWindow }?.let(listener::onViewAttachedToWindow)
    }

    fun showIme() {
        host.showChecked(host.ime)
    }

    fun hideIme() {
        host.hideChecked(host.ime)
    }

    private inner class EditorHostImpl : EditorHost {
        override val WindowInsetsCompat.imeOffset: Int
            get() = window?.run { imeOffset } ?: NO_VALUE
        override var isRestored = false
        override val hasWindowFocus: Boolean
            get() = window?.hasWindowFocus ?: false
        override var editorOffset = 0
        override var navBarOffset = 0
        override val ime = Ime
        override var current: Ime? = null
        override val container = null
        override val previousView = null
        override val currentView = null
        private var pendingChange: Boolean? = null

        fun onAttachedToWindow(window: ViewTreeWindow) {
            window.registerHost(this)
            this@ImeAnimator.onAttachedToHost(this)
            imeFocusHandler.attach()
            isRestored = true
        }

        fun onDetachedFromWindow(window: ViewTreeWindow) {
            window.unregisterHost(this)
            window.restoreImeAnimation()
            this@ImeAnimator.onDetachedFromHost(this)
            imeFocusHandler.detach()
        }

        fun onImeFocusHandlerChanged(previous: ImeFocusHandler?, current: ImeFocusHandler) {
            previous?.detach()
            if (view.isAttachedToWindow) current.attach()
        }

        override fun updateEditorOffset(offset: Int) {
            editorOffset = offset
        }

        override fun dispatchImeShown(shown: Boolean): Boolean {
            pendingChange = null
            val next = if (shown) ime else null
            val changed = this@ImeAnimator.canChangeEditor(current, next) && current !== next
            if (changed) {
                val previous = current
                current = next
                if (shown) imeFocusHandler.requestCurrentFocus() else imeFocusHandler.clearCurrentFocus()
                this@ImeAnimator.onPendingChanged(previous, current, immediately = false)
            }
            return changed
        }

        override fun showChecked(editor: Editor): Boolean {
            val changed = this@ImeAnimator.canChangeEditor(current, editor)
                    && current == null && pendingChange != true
            if (changed) {
                pendingChange = true
                imeFocusHandler.requestFocus()
                imeFocusHandler.showIme()
            }
            return changed
        }

        override fun hideChecked(editor: Editor): Boolean {
            val changed = this@ImeAnimator.canChangeEditor(editor, null)
                    && current != null && pendingChange != false
            if (changed) {
                pendingChange = false
                imeFocusHandler.clearCurrentFocus()
                imeFocusHandler.hideIme()
            }
            return changed
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
            return OneShotPreDrawListener.add(view, action)
        }

        override fun modifyImeAnimation(durationMillis: Long, interpolator: Interpolator) {
            window?.modifyImeAnimation(durationMillis, interpolator)
        }

        override fun setOnApplyWindowInsetsListener(listener: OnApplyWindowInsetsListenerCompat?) {
            val wrapper = if (listener == null) null else OnApplyWindowInsetsListenerCompat { v, insets ->
                navBarOffset = window?.run { insets.navBarOffset } ?: 0
                listener.onApplyWindowInsets(v, insets)
            }
            if (wrapper == null) navBarOffset = 0
            ReflectCompat { view.setOnApplyWindowInsetsListenerImmutable(wrapper) }
        }

        override fun setWindowInsetsAnimationCallback(callback: WindowInsetsAnimationCompat.Callback?) {
            if (callback == null) window?.restoreImeAnimation()
            ReflectCompat { view.setWindowInsetsAnimationCallbackImmutable(callback) }
        }

        override fun removeEditorView(view: View) = Unit
    }
}