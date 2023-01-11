package com.xiaocydx.inputview

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import androidx.core.animation.addListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw

/**
 * [InputView]编辑区的[Editor]过渡动画
 *
 * @author xcc
 * @date 2023/1/8
 */
abstract class EditorAnimator : EditorVisibleListener<Editor> {
    private var animationRecord: AnimationRecord? = null
    private var editorAdapter: EditorAdapter<*>? = null
    private val insetsHandler = InsetsHandler()
    private val editorView: EditorView?
        get() = editorAdapter?.editorView
    private val inputView: InputView?
        get() = editorAdapter?.inputView
    internal open val canRunAnimation: Boolean = true

    /**
     * 动画是否运行中
     */
    val isRunning: Boolean
        get() = animationRecord != null

    /**
     * 动画开始
     *
     * @param startView   编辑区的起始视图
     * @param endView     编辑区的结束视图
     * @param startOffset 编辑区的起始偏移值
     * @param endOffset   编辑区的结束偏移值
     */
    protected open fun onAnimationStart(
        startView: View?, endView: View?,
        startOffset: Int, endOffset: Int
    ) = Unit

    /**
     * 动画更新
     *
     * @param startView     编辑区的起始视图
     * @param endView       编辑区的结束视图
     * @param startOffset   编辑区的起始偏移值
     * @param endOffset     编辑区的结束偏移值
     * @param currentOffset 编辑区的当前偏移值
     */
    protected open fun onAnimationUpdate(
        startView: View?, endView: View?,
        startOffset: Int, endOffset: Int, currentOffset: Int
    ) = Unit

    /**
     * 动画结束
     *
     * @param startView   编辑区的起始视图
     * @param endView     编辑区的结束视图
     * @param startOffset 编辑区的起始偏移值
     * @param endOffset   编辑区的结束偏移值
     *
     * **注意**：动画结束时，应当将[startView]和[endView]恢复为初始状态。
     */
    protected open fun onAnimationEnd(
        startView: View?, endView: View?,
        startOffset: Int, endOffset: Int
    ) = Unit

    /**
     * 获取动画时长，单位：ms
     *
     * @param startView   编辑区的起始视图
     * @param endView     编辑区的结束视图
     * @param startOffset 编辑区的起始偏移值
     * @param endOffset   编辑区的结束偏移值
     *
     * **注意**：显示或隐藏IME时，内部实现会配合Insets动画，因此不会调用该函数。
     */
    protected open fun getAnimationDuration(
        startView: View?, endView: View?,
        startOffset: Int, endOffset: Int
    ): Long = ANIMATION_DURATION

    /**
     * 获取动画时长，单位：ms
     *
     * @param startView   编辑区的起始视图
     * @param endView     编辑区的结束视图
     * @param startOffset 编辑区的起始偏移值
     * @param endOffset   编辑区的结束偏移值
     *
     * **注意**：显示或隐藏IME时，内部实现会配合Insets动画，因此不会调用该函数。
     */
    protected open fun getAnimationInterpolator(
        startView: View?, endView: View?,
        startOffset: Int, endOffset: Int
    ): Interpolator = ANIMATION_INTERPOLATOR

    /**
     * 更新编辑区的偏移值
     *
     * **注意**：只能在[onAnimationStart]、[onAnimationUpdate]、[onAnimationEnd]调用该函数。
     */
    protected fun updateEditorOffset(offset: Int) {
        inputView?.updateEditorOffset(offset, resizeInNextLayout = canRunAnimation)
    }

    /**
     * 当前[EditorAnimator]添加到[adapter]
     */
    protected open fun onAttachToEditorAdapter(adapter: EditorAdapter<*>) = Unit

    /**
     * 当前[EditorAnimator]从[adapter]移除
     */
    protected open fun onDetachFromEditorAdapter(adapter: EditorAdapter<*>) = Unit

    /**
     * 1. [EditorView]更改[Editor]时会先移除全部子View，再添加当前[Editor]的子View，
     * 运行动画之前调用[AnimationRecord.addStartViewIfNecessary]将移除的子View重新添加回来，
     * 参与动画更新过程，动画结束时调用[AnimationRecord.removeStartViewIfNecessary]移除子View。
     *
     * 2. 若[previous]和[current]不是IME，则调用[runSimpleAnimationIfNecessary]运行简单动画，
     * 若[previous]和[current]其中一个是IME，则运行Insets动画，在[InsetsHandler]做进一步处理。
     */
    final override fun onVisibleChanged(previous: Editor?, current: Editor?) {
        val inputView = inputView ?: return
        // 若还未到下一帧运行动画，则不会置空animationRecord，而是更新记录的属性
        animationRecord?.endAnimation()
        if (animationRecord == null) {
            animationRecord = AnimationRecord()
            inputView.doOnPreDraw {
                val record = animationRecord
                if (record == null || record.willRunInsetsAnimation) return@doOnPreDraw
                record.setAnimationOffset(inputView.editorOffset, getEditorEndOffset())
                runSimpleAnimationIfNecessary(record)
            }
        }
        animationRecord!!.apply {
            // 在下一帧运行动画之前，更新记录的属性
            updateStartViewAndEndView()
            updateRunAnimationType(previous, current)
        }
    }

    private fun getEditorEndOffset(): Int {
        val editorView = editorView ?: return NO_VALUE
        return editorView.changeRecord.currentChild?.height ?: 0
    }

    private inline fun runSimpleAnimationIfNecessary(
        record: AnimationRecord,
        block: ValueAnimator.() -> Boolean = { false }
    ) {
        if (!canRunAnimation || record.startOffset == record.endOffset) {
            dispatchAnimationStart(record)
            dispatchAnimationEnd(record)
            return
        }

        ValueAnimator.ofInt(record.startOffset, record.endOffset).apply {
            addListener(
                onStart = { dispatchAnimationStart(record) },
                onCancel = { dispatchAnimationEnd(record) },
                onEnd = { dispatchAnimationEnd(record) }
            )
            addUpdateListener { dispatchAnimationUpdate(record, it.animatedValue as Int) }
            if (block(this)) return@apply start()

            duration = record.run {
                getAnimationDuration(startView, endView, startOffset, endOffset)
            }
            interpolator = record.run {
                getAnimationInterpolator(startView, endView, startOffset, endOffset)
            }
            start()
        }.also(record::setSimpleAnimation)
    }

    private fun dispatchAnimationStart(record: AnimationRecord) = with(record) {
        if (startOffset == NO_VALUE || endOffset == NO_VALUE) return@with
        onAnimationStart(startView, endView, startOffset, endOffset)
    }

    private fun dispatchAnimationUpdate(record: AnimationRecord, currentOffset: Int) = with(record) {
        if (startOffset == NO_VALUE || endOffset == NO_VALUE) return@with
        onAnimationUpdate(startView, endView, startOffset, endOffset, currentOffset)
    }

    private fun dispatchAnimationEnd(record: AnimationRecord) = with(record) {
        if (startOffset != NO_VALUE && endOffset != NO_VALUE) {
            if (inputView != null && inputView!!.editorOffset != endOffset) {
                dispatchAnimationUpdate(record, endOffset)
            }
            onAnimationEnd(startView, endView, startOffset, endOffset)
        }
        removeStartViewIfNecessary()
        animationRecord = null
    }

    internal fun attach(adapter: EditorAdapter<*>) {
        editorAdapter = adapter
        insetsHandler.reset()
        editorView?.let { ViewCompat.setWindowInsetsAnimationCallback(it, insetsHandler) }
        adapter.addEditorVisibleListener(this)
        onAttachToEditorAdapter(adapter)
    }

    internal fun detach(adapter: EditorAdapter<*>) {
        assert(editorAdapter === adapter) { "EditorAdapter不相同" }
        animationRecord?.endAnimation()
        editorView?.let { ViewCompat.setWindowInsetsAnimationCallback(it, null) }
        editorAdapter = null
        adapter.removeEditorVisibleListener(this)
        onDetachFromEditorAdapter(adapter)
    }

    private inner class InsetsHandler : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
        private var typeMask = NO_VALUE
        private var window: ViewTreeWindow? = null
            get() = field ?: editorView?.findViewTreeWindow()?.also { field = it }

        fun reset() {
            typeMask = NO_VALUE
        }

        override fun onStart(
            animation: WindowInsetsAnimationCompat,
            bounds: WindowInsetsAnimationCompat.BoundsCompat
        ): WindowInsetsAnimationCompat.BoundsCompat {
            val window = window
            val insets = window?.getRootWindowInsets()
            if (window == null || insets == null || typeMask != NO_VALUE
                    || !window.containsImeType(animation.typeMask)) {
                return bounds
            }
            typeMask = animation.typeMask
            editorView?.dispatchIme(isShow = window.getImeHeight(insets) > 0)

            val record = animationRecord?.takeIf { it.willRunInsetsAnimation } ?: return bounds
            val isCurrentIme = record.isIme(record.current)
            record.setAnimationOffset(
                startOffset = inputView?.editorOffset ?: NO_VALUE,
                endOffset = if (isCurrentIme) window.getImeOffset(insets) else getEditorEndOffset()
            )
            record.setInsetsAnimation(animation)
            when {
                !canRunAnimation || (record.startOffset == record.endOffset)
                        || (record.imeToOther() || record.otherToIme()) -> {
                    // imeToOther的animation是0到imeEndOffset，
                    // otherToIme的animation是imeStartOffset到0，
                    // 这两种情况依靠animation进行更新，实现的动画效果并不理想，
                    // 因此用animation的duration和interpolator运行动画进行更新。
                    runSimpleAnimationIfNecessary(record) {
                        duration = animation.durationMillis
                        interpolator = animation.interpolator ?: ANIMATION_INTERPOLATOR
                        true
                    }
                }
                else -> dispatchAnimationStart(record)
            }
            return bounds
        }

        override fun onProgress(
            insets: WindowInsetsCompat,
            runningAnimations: MutableList<WindowInsetsAnimationCompat>
        ): WindowInsetsCompat {
            val window = window ?: return insets
            val animation = runningAnimations.firstOrNull { it.typeMask == typeMask }
            animationRecord?.takeIf { animation != null && it.handleInsetsAnimation(animation) }
                ?.let { dispatchAnimationUpdate(it, currentOffset = window.getImeOffset(insets)) }
            return insets
        }

        override fun onEnd(animation: WindowInsetsAnimationCompat) {
            animationRecord?.takeIf { animation.typeMask == typeMask }
                ?.takeIf { it.handleInsetsAnimation(animation) }
                ?.let(::dispatchAnimationEnd)
            typeMask = NO_VALUE
        }
    }

    private inner class AnimationRecord {
        var previous: Editor? = null
            private set
        var current: Editor? = null
            private set
        var startView: View? = null
            private set
        var endView: View? = null
            private set
        var startOffset: Int = NO_VALUE
            private set
        var endOffset: Int = NO_VALUE
            private set
        var willRunInsetsAnimation = false
            private set
        var willRunSimpleAnimation = false
            private set
        var insetsAnimation: WindowInsetsAnimationCompat? = null
            private set
        var simpleAnimation: Animator? = null
            private set

        fun imeToOther() = isIme(previous) && current != null && !isIme(current)

        fun otherToIme() = previous != null && !isIme(previous) && isIme(current)

        fun isIme(editor: Editor?): Boolean {
            return editor != null && editorView != null && editorView!!.ime === editor
        }

        fun updateStartViewAndEndView() {
            addStartViewIfNecessary()
            updateEndView()
        }

        fun updateRunAnimationType(previous: Editor?, current: Editor?) {
            this.previous = previous
            this.current = current
            // 存在逻辑缺陷，insetsAnimation不可结束
            willRunInsetsAnimation = isIme(previous) || isIme(current)
            willRunSimpleAnimation = !willRunInsetsAnimation
        }

        fun addStartViewIfNecessary() {
            val editorView = editorView
            if (!canRunAnimation || editorView == null) return
            val record = editorView.changeRecord
            if (startView !== record.previousChild
                    && startView !== record.currentChild) {
                removeStartViewIfNecessary()
            }
            val child = editorView.changeRecord.previousChild
            if (child != null && child.parent == null) {
                startView = child
                editorView.addView(child)
            }
        }

        fun removeStartViewIfNecessary() {
            val editorView = editorView ?: return
            val child = startView
            if (child != null && child.parent === editorView
                    && child !== editorView.changeRecord.currentChild) {
                editorView.removeView(child)
            }
            startView = null
        }

        fun updateEndView() {
            endView = editorView?.changeRecord?.currentChild
        }

        fun setAnimationOffset(startOffset: Int, endOffset: Int) {
            this.startOffset = startOffset
            this.endOffset = endOffset
        }

        fun setInsetsAnimation(animation: WindowInsetsAnimationCompat) {
            insetsAnimation = animation
            willRunInsetsAnimation = true
        }

        fun setSimpleAnimation(animation: Animator) {
            simpleAnimation = animation
            willRunSimpleAnimation = true
        }

        fun handleInsetsAnimation(animation: WindowInsetsAnimationCompat): Boolean = when {
            willRunSimpleAnimation && simpleAnimation != null -> false
            willRunInsetsAnimation && insetsAnimation === animation -> true
            else -> false
        }

        fun endAnimation() {
            // simpleAnimation.end()会置空insetsAnimation
            simpleAnimation?.end()
            // 存在逻辑缺陷，insetsAnimation不可结束，需要主动更新为结束值
            if (insetsAnimation != null) dispatchAnimationEnd(this)
        }
    }

    companion object {
        private const val NO_VALUE = -1
        private const val ANIMATION_DURATION = 250L
        private val ANIMATION_INTERPOLATOR = DecelerateInterpolator()
    }
}