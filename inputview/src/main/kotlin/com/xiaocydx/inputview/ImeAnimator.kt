package com.xiaocydx.inputview

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
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
 * 复用[InputView]的动画实现，调用该函数之前，需要先调用`InputView.init()`
 *
 * ```
 * InputView.init(window)
 * val animator = InputView.animator(view)
 * // 显示和隐藏IME，运行动画平移contentView
 * animator.addAnimationCallback(onUpdate = { state ->
 *     contentView.translationY = -state.currentOffset.toFloat()
 * })
 * ```
 *
 * @param view 首次传入，按[durationMillis]和[interpolator]创建[EditorAnimator]，
 * 后续传入，获取首次创建的[EditorAnimator]，若[view]的类型是[EditText]，则利用
 * [EditText.setOnTouchListener]解决水滴状指示器导致动画卡顿的问题。
 * @param durationMillis 含义等同于[EditorAnimator.durationMillis]
 * @param interpolator   含义等同于[EditorAnimator.interpolator]
 */
@CheckResult
fun InputView.Companion.animator(
    view: View,
    durationMillis: Long = ANIMATION_DURATION_MILLIS,
    interpolator: Interpolator = ANIMATION_INTERPOLATOR
): EditorAnimator {
    val key = R.id.tag_view_ime_animator
    var animator = view.getTag(key) as? ImeAnimator
    if (animator == null) {
        animator = ImeAnimator(view, durationMillis, interpolator)
        view.setTag(key, animator)
    }
    return animator
}

private class ImeAnimator(
    private val view: View,
    durationMillis: Long,
    interpolator: Interpolator
) : EditorAnimator(durationMillis, interpolator) {
    private val host = EditorHostImpl()

    init {
        setAnimationCallback(object : AnimationCallback {
            override fun onAnimationUpdate(state: AnimationState) {
                state.updateEditorOffset(state.currentOffset)
            }
        })
        var holder: EditTextHolder? = null
        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                require(view.parent is ViewGroup) {
                    "InputView.animator()不能传入Window.decorView"
                }
                val window = requireNotNull(view.findViewTreeWindow()) {
                    "需要调用InputView.init()初始化InputView.animator()所需的配置"
                }
                if (holder == null && view is EditText) {
                    holder = createEditTextHolder(view, window)
                }
                host.onAttachedToWindow(window)
                holder?.onAttachToEditorHost(host)
                this@ImeAnimator.onAttachToEditorHost(host)
            }

            override fun onViewDetachedFromWindow(v: View) {
                holder?.onDetachFromEditorHost(host)
                this@ImeAnimator.onDetachFromEditorHost(host)
            }
        }
        if (view.isAttachedToWindow) listener.onViewAttachedToWindow(view)
        view.addOnAttachStateChangeListener(listener)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createEditTextHolder(editText: EditText, window: ViewTreeWindow): EditTextHolder {
        val holder = EditTextHolder(editText, window)
        editText.setOnTouchListener { _, event ->
            holder.beforeTouchEvent(event)
            val consumed = editText.onTouchEvent(event)
            holder.afterTouchEvent(event)
            consumed
        }
        return holder
    }

    private inner class EditorHostImpl : EditorHost {
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