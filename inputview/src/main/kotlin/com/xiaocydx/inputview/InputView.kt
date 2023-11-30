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

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.makeMeasureSpec
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.view.animation.Interpolator
import android.widget.EditText
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import androidx.core.view.OneShotPreDrawListener
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import com.xiaocydx.inputview.compat.ReflectCompat
import com.xiaocydx.inputview.compat.toCompat

/**
 * 输入控件
 *
 * 1. 调用`InputView.init()`初始化[InputView]所需的配置。
 * 2. [InputView]初始化时只能有一个子View，该子View作为[contentView]。
 * 3. [editText]用于兼容Android各版本显示和隐藏IME。
 * 4. [editorAdapter]支持[Editor]的视图创建和显示。
 * 5. [editorAnimator]支持[Editor]的切换过渡动画。
 *
 * [contentView]的初始布局位置等同于[Gravity.CENTER]，其测量高度不受`layoutParams.height`影响，
 * 最大值是[InputView]的测量高度，测量宽度同理，[Editor]位于[contentView]下方，通知显示[Editor]时，
 * 会平移[contentView]，或者修改[contentView]的尺寸，具体是哪一种行为，取决于[EditorMode]。
 *
 * @author xcc
 * @date 2023/1/6
 */
class InputView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {
    private val host = EditorHostImpl()
    private val editorView = EditorContainer(context)
    private var contentView: View? = null
    private var window: ViewTreeWindow? = null
    private var editTextHolder: EditTextHolder? = null
    private var editorOffset = 0
    private var navBarOffset = 0

    /**
     * 用于兼容Android各版本显示和隐藏IME的[EditText]
     *
     * **注意**：[editText]必须是[InputView]的子View或间接子View。
     *
     * 显示IME[editText]会获得焦点，隐藏IME会清除[editText]的焦点，
     * 可以通过[EditorChangedListener]处理[editText]的焦点，例如：
     * ```
     * enum class MessageEditor : Editor {
     *     IME, VOICE, EMOJI
     * }
     *
     * inputView.editorAdapter.addEditorChangedListener { previous, current ->
     *     // 显示EMOJI，让editText获得焦点
     *     if (current === EMOJI) inputView.editText?.requestFocus()
     * }
     * ```
     */
    var editText: EditText?
        get() = editTextHolder?.value as? EditText
        set(value) {
            val previous = editTextHolder
            val current = value?.let { EditTextHolder(it, window) }
            host.onEditTextHolderChanged(previous, current)
            editTextHolder = current
        }

    /**
     * [EditorMode]默认为[EditorMode.ADJUST_RESIZE]
     *
     * 1. [EditorMode.ADJUST_PAN]，显示[Editor]时平移[contentView]。
     * 2. [EditorMode.ADJUST_RESIZE]，显示[Editor]时修改[contentView]的尺寸。
     */
    var editorMode: EditorMode = EditorMode.ADJUST_RESIZE
        set(value) {
            if (field === value) return
            field = value
            requestLayout()
        }

    /**
     * [editorAdapter]默认为[ImeAdapter]
     *
     * [ImeAdapter]用于只需要IME的场景，若需要显示多种[Editor]，
     * 则继承并实现[EditorAdapter]，其注释介绍了如何实现以及注意点。
     *
     * [EditorAdapter.onCreateView]创建的视图可能需要实现手势导航栏EdgeToEdge，
     * [EdgeToEdgeHelper]提供了实现手势导航栏EdgeToEdge的函数。
     */
    var editorAdapter: EditorAdapter<*>
        get() = editorView.adapter
        set(value) {
            val previous = editorView.adapter
            if (previous === value) return
            host.onEditorAdapterChanged(previous, value)
            editorView.setAdapter(value)
            editorOffset = 0
            requestLayout()
        }

    /**
     * [editorAnimator]默认为[FadeEditorAnimator]
     *
     * 若需要调整动画的参数，则可以继承[FadeEditorAnimator]进行修改，
     * 或者参考[FadeEditorAnimator]，继承[EditorAnimator]实现动画。
     */
    var editorAnimator: EditorAnimator = FadeEditorAnimator()
        set(value) {
            val previous = field
            if (previous === value) return
            host.onEditorAnimatorChanged(previous, value)
            field = value
        }

    /**
     * [Editor]区域的[Drawable]，边界范围包含手势导航栏EdgeToEdge的偏移
     */
    var editBackground: Drawable? = null
        set(value) {
            if (field === value) return
            setWillNotDraw(value == null)
            field?.callback = null
            value?.callback = this
            field = value
            field?.invalidateSelf()
        }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        addView(editorView, MATCH_PARENT, WRAP_CONTENT)
        host.onEditorAdapterChanged(previous = null, editorAdapter)
        host.onEditorAnimatorChanged(previous = null, editorAnimator)
    }

    /**
     * 设置[Editor]区域的背景色，边界范围包含手势导航栏EdgeToEdge的偏移
     */
    fun setEditBackgroundColor(@ColorInt color: Int) {
        if (editBackground is ColorDrawable) {
            (editBackground!!.mutate() as ColorDrawable).color = color
        } else {
            editBackground = ColorDrawable(color)
        }
    }

    override fun shouldDelayChildPressedState(): Boolean = false

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (window == null) {
            window = requireNotNull(findViewTreeWindow()) {
                "需要调用InputView.init()初始化InputView所需的配置"
            }
            window?.let(host::onAttachedToWindow)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        editTextHolder?.beforeDispatchTouchEvent(ev)
        val consumed = super.dispatchTouchEvent(ev)
        editTextHolder?.afterDispatchTouchEvent(ev)
        return consumed
    }

    /**
     * [contentView]和[editorView]之间会有一个[navBarOffset]区域，
     * 当支持手势导航栏EdgeToEdge时，[navBarOffset]等于导航栏高度，此时显示[Editor]，
     * 在[editorOffset]超过[navBarOffset]后，才会更新[contentView]的尺寸或位置。
     */
    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val lastNavBarOffset = window?.run {
            insets.toCompat(this@InputView).navigationBarOffset
        } ?: 0
        if (navBarOffset != lastNavBarOffset) {
            navBarOffset = lastNavBarOffset
            requestLayout()
        }
        return super.onApplyWindowInsets(insets)
    }

    /**
     * 更新编辑区的偏移值
     *
     * 若调用该函数之前已申请重新布局，则不处理[editorView]和[contentView]的尺寸和位置，否则：
     * 1. 若[editorMode]为[EditorMode.ADJUST_PAN]，则偏移[editorView]和[contentView]。
     * 2. 若[editorMode]为[EditorMode.ADJUST_RESIZE]，则申请重新measure和layout。
     */
    @Suppress("KotlinConstantConditions", "ConvertTwoComparisonsToRangeCheck")
    private fun updateEditorOffset(offset: Int) {
        val current = offset.coerceAtLeast(0)
        if (editorOffset == current) return
        val previous = editorOffset
        editorOffset = current

        val contentView = contentView
        if (contentView == null || isLayoutRequested) {
            // isLayoutRequested = true，已经申请重新布局
            return
        }

        when (editorMode) {
            EditorMode.ADJUST_PAN -> {
                val editorDiff = previous - current
                editorView.offsetTopAndBottom(editorDiff)
                val threshold = navBarOffset
                val contentDiff = when {
                    previous >= threshold && current >= threshold -> editorDiff
                    previous < threshold && current > threshold -> threshold - current
                    previous > threshold && current < threshold -> previous - threshold
                    else -> Int.MIN_VALUE
                }
                if (contentDiff != Int.MIN_VALUE) {
                    contentView.offsetTopAndBottom(contentDiff)
                }
                updateEditBackground(top = contentView.bottom)
            }
            EditorMode.ADJUST_RESIZE -> requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        checkContentView()
        val contentView = contentView ?: return

        // 消费PendingChange，仅在measure阶段创建、添加、移除子View
        if (editorView.consumePendingChange()) {
            val (previous, current, _, _) = editorView.changeRecord
            editorAnimator.onPendingChanged(previous, current)
        }
        editorView.measure(widthMeasureSpec, measuredHeight.toAtMostMeasureSpec())

        // 若消费PendingChange失败，则表示等待条件未满足，先不尝试修正editorOffset
        if (!editorView.hasPendingChange()
                && (!editorAnimator.canRunAnimation || !editorAnimator.isActive)) {
            // 尝试修正editorOffset，例如导航栏高度改变（导航栏模式更改），
            // editorView的子View实现手势导航栏EdgeToEdge，可能会修改尺寸，
            // 此时editorOffset未修正，导致布局位置不正确。
            val offset = editorAnimator.calculateEndOffset()
            if (offset >= 0) editorOffset = offset
        }

        val horizontalMargin = contentView.let { it.marginLeft + it.marginRight }
        val verticalMargin = contentView.let { it.marginTop + it.marginBottom }
        val maxContentWidth = measuredWidth - horizontalMargin
        var maxContentHeight = measuredHeight - verticalMargin - navBarOffset
        if (editorMode === EditorMode.ADJUST_RESIZE) {
            maxContentHeight -= getLayoutOffset()
        }
        contentView.measure(
            when (val width = contentView.layoutParams.width) {
                MATCH_PARENT -> maxContentWidth.toExactlyMeasureSpec()
                WRAP_CONTENT -> maxContentWidth.toAtMostMeasureSpec()
                else -> width.coerceAtMost(maxContentWidth).toExactlyMeasureSpec()
            },
            when (val height = contentView.layoutParams.height) {
                MATCH_PARENT -> maxContentHeight.toExactlyMeasureSpec()
                WRAP_CONTENT -> maxContentHeight.toAtMostMeasureSpec()
                else -> height.coerceAtMost(maxContentHeight).toExactlyMeasureSpec()
            }
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val contentView = contentView ?: return
        // 基于inputView底部向下布局editorView，
        // 通过editorOffset向上偏移editorView。
        editorView.let {
            val left = 0
            val top = measuredHeight - editorOffset
            val right = left + it.measuredWidth
            val bottom = top + it.measuredHeight
            it.layout(left, top, right, bottom)
        }

        // 基于editorView顶部向上布局contentView
        contentView.let {
            val left = (measuredWidth - it.measuredWidth) / 2
            val bottom = height - it.marginBottom - navBarOffset - getLayoutOffset()
            val right = left + it.measuredWidth
            val top = bottom - it.measuredHeight
            it.layout(left, top, right, bottom)
        }
        updateEditBackground(top = contentView.bottom)
    }

    override fun onDraw(canvas: Canvas) {
        val contentView = contentView ?: return
        editBackground?.setBounds(0, contentView.bottom, width, height)
        editBackground?.takeIf { it.bounds.height() > 0 }?.draw(canvas)
    }

    private fun Int.toExactlyMeasureSpec() = makeMeasureSpec(this, EXACTLY)

    private fun Int.toAtMostMeasureSpec() = makeMeasureSpec(this, AT_MOST)

    private fun checkContentView() {
        if (contentView != null) return
        for (i in 0 until childCount) {
            require(contentView == null) { "InputView初始化时只能有一个子View" }
            val child = getChildAt(i)
            if (child !== editorView) contentView = child
        }
    }

    private fun getLayoutOffset(): Int {
        return (editorOffset - navBarOffset).coerceAtLeast(0)
    }

    private fun updateEditBackground(top: Int) {
        editBackground?.takeIf { it.bounds.top != top }?.invalidateSelf()
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return who === editBackground || super.verifyDrawable(who)
    }

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun getEditorHost(): EditorHost = host

    private inner class EditorHostImpl : EditorHost {
        private var pending: PendingInsetsAnimationCallback? = null
        override val WindowInsetsCompat.imeHeight: Int
            get() = window?.run { imeHeight } ?: NO_VALUE
        override val WindowInsetsCompat.imeOffset: Int
            get() = window?.run { imeOffset } ?: NO_VALUE
        override val editorOffset: Int
            get() = this@InputView.editorOffset
        override val navBarOffset: Int
            get() = this@InputView.navBarOffset
        override val ime: Editor?
            get() = editorView.ime
        override val current: Editor?
            get() = editorView.current
        override val container: ViewGroup
            get() = editorView
        override val previousView: View?
            get() = editorView.changeRecord.previousChild
        override val currentView: View?
            get() = editorView.changeRecord.currentChild

        fun onAttachedToWindow(window: ViewTreeWindow) {
            editTextHolder?.onAttachedToWindow(window)
            pending?.apply { setWindowInsetsAnimationCallback(durationMillis, interpolator, callback) }
        }

        fun onEditorAdapterChanged(previous: EditorAdapter<*>?, current: EditorAdapter<*>) {
            editorAnimator.endAnimation()
            previous?.onDetachFromEditorHost(this)
            current.onAttachToEditorHost(this)
            previous?.forEachListener { if (it is Replicable) current.addEditorChangedListener(it) }
        }

        fun onEditorAnimatorChanged(previous: EditorAnimator?, current: EditorAnimator) {
            previous?.onDetachFromEditorHost(this)
            current.onAttachToEditorHost(this)
            previous?.forEachCallback { if (it is Replicable) current.addAnimationCallback(it) }
            editorView.setRemovePreviousImmediately(!current.canRunAnimation)
        }

        fun onEditTextHolderChanged(previous: EditTextHolder?, current: EditTextHolder?) {
            previous?.onDetachFromEditorHost(this)
            current?.onAttachToEditorHost(this)
            editorView.setEditTextHolder(current)
        }

        override fun removeEditorView(view: View) {
            view.takeIf { it.parent === editorView }?.let(editorView::removeView)
        }

        override fun updateEditorOffset(offset: Int) {
            this@InputView.updateEditorOffset(offset)
        }

        override fun dispatchImeShown(shown: Boolean) {
            editorView.dispatchImeShown(shown)
        }

        override fun showChecked(editor: Editor) {
            editorView.showChecked(editor)
        }

        override fun hideChecked(editor: Editor) {
            editorView.hideChecked(editor)
        }

        override fun addAnimationCallback(callback: AnimationCallback) {
            editorAnimator.addAnimationCallback(callback)
        }

        override fun removeAnimationCallback(callback: AnimationCallback) {
            editorAnimator.removeAnimationCallback(callback)
        }

        override fun addEditorChangedListener(listener: EditorChangedListener<Editor>) {
            editorAdapter.addEditorChangedListener(listener)
        }

        override fun removeEditorChangedListener(listener: EditorChangedListener<Editor>) {
            editorAdapter.removeEditorChangedListener(listener)
        }

        override fun addPreDrawAction(action: () -> Unit): OneShotPreDrawListener {
            return OneShotPreDrawListener.add(editorView, action)
        }

        override fun setOnApplyWindowInsetsListener(listener: OnApplyWindowInsetsListenerCompat?) {
            ReflectCompat { editorView.setOnApplyWindowInsetsListenerImmutable(listener) }
        }

        override fun setWindowInsetsAnimationCallback(
            durationMillis: Long,
            interpolator: Interpolator,
            callback: WindowInsetsAnimationCompat.Callback?
        ) {
            val window = window
            pending = when {
                window != null || callback == null -> null
                else -> PendingInsetsAnimationCallback(durationMillis, interpolator, callback)
            }
            if (pending == null && window != null) {
                if (callback == null) {
                    window.restoreImeAnimation()
                } else {
                    window.modifyImeAnimation(durationMillis, interpolator)
                }
                ReflectCompat { editorView.setWindowInsetsAnimationCallbackImmutable(callback) }
            }
        }
    }

    private class PendingInsetsAnimationCallback(
        val durationMillis: Long,
        val interpolator: Interpolator,
        val callback: WindowInsetsAnimationCompat.Callback
    )

    companion object
}