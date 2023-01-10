package com.xiaocydx.inputview

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.animation.addListener
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.doOnPreDraw

/**
 * [InputView]编辑区的[Editor]切换动画
 *
 * @author xcc
 * @date 2023/1/8
 */
abstract class EditorAnimator : EditorVisibleListener<Editor> {
    private var previous: Editor? = null
    private var current: Editor? = null
    private var insetsStartOffset = NO_VALUE
    private var insetsEndOffset = NO_VALUE
    private var willRunInsetsAnimation = false
    private var willRunSimpleAnimation = false
    private var simpleAnimation: Animator? = null
    private var editorAdapter: EditorAdapter<*>? = null
    private val editorView: EditorView?
        get() = editorAdapter?.editorView
    private val inputView: InputView?
        get() = editorAdapter?.inputView
    internal open val canRunAnimation: Boolean = true

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
     * 运行动画之前调用[addStartViewBeforeRunAnimation]将移除的子View重新添加回来，
     * 参与动画的更新过程，动画结束时调用[removeStartViewAfterAnimationEnd]移除子View。
     *
     * 2. 若[previous]和[current]不是IME，则调用[runSimpleAnimationIfNecessary]运行简单动画，
     * 若[previous]和[current]其中一个是IME，则运行insets动画，在[onWindowInsetsAnimationStart]、
     * [onWindowInsetsAnimationProgress]、[onWindowInsetsAnimationEnd]做进一步处理。
     */
    final override fun onVisibleChanged(previous: Editor?, current: Editor?) {
        this.previous = previous
        this.current = current
        simpleAnimation?.end()
        addStartViewBeforeRunAnimation()
        willRunInsetsAnimation = isIme(previous) || isIme(current)
        if (willRunInsetsAnimation) return

        val inputView = inputView
        if (inputView == null || willRunSimpleAnimation) return
        willRunSimpleAnimation = true
        val startOffset = inputView.editorOffset
        inputView.doOnPreDraw {
            willRunSimpleAnimation = false
            if (willRunInsetsAnimation) return@doOnPreDraw
            val endOffset = getEditorEndOffset()
            runSimpleAnimationIfNecessary(startOffset, endOffset) run@{
                if (startOffset == 0 || endOffset == 0) return@run
                // previous和current不是IME，匀速的过渡效果更流畅
                interpolator = LinearInterpolator()
            }
        }
    }

    /**
     * [WindowInsetsAnimationCompat]动画开始
     *
     * @param endValue [animation]动画结束时的偏移值
     */
    internal fun onWindowInsetsAnimationStart(endValue: Int, animation: WindowInsetsAnimationCompat) {
        if (!willRunInsetsAnimation) return
        insetsStartOffset = inputView?.editorOffset ?: NO_VALUE
        insetsEndOffset = if (isIme(current)) endValue else getEditorEndOffset()
        val imeToOther = isIme(previous) && current != null && !isIme(current)
        val otherToIme = previous != null && !isIme(previous) && isIme(current)
        val startView = editorView?.changeRecord?.previousChild
        val endView = editorView?.changeRecord?.currentChild
        when {
            insetsStartOffset == insetsEndOffset -> {
                dispatchAnimationEnd(startView, endView, insetsStartOffset, insetsEndOffset)
            }
            !canRunAnimation || imeToOther || otherToIme -> {
                // imeToOther的animation是0到imeEndOffset，
                // otherToIme的animation是imeStartOffset到0，
                // 这两种情况依靠animation进行更新，实现的过渡效果并不流畅，
                // 因此用animation的duration和interpolator运行动画进行更新。
                runSimpleAnimationIfNecessary(insetsStartOffset, insetsEndOffset) {
                    duration = animation.durationMillis
                    interpolator = animation.interpolator ?: ANIMATION_INTERPOLATOR
                }
                insetsStartOffset = NO_VALUE
                insetsEndOffset = NO_VALUE
            }
            else -> onAnimationStart(startView, endView, insetsStartOffset, insetsEndOffset)
        }
    }

    /**
     * [WindowInsetsAnimationCompat]动画更新
     *
     * @param currentValue [animation]动画运行中的偏移值
     */
    internal fun onWindowInsetsAnimationProgress(currentValue: Int, animation: WindowInsetsAnimationCompat) {
        if (insetsStartOffset == NO_VALUE || insetsEndOffset == NO_VALUE) return
        val startView = editorView?.changeRecord?.previousChild
        val endView = editorView?.changeRecord?.currentChild
        onAnimationUpdate(startView, endView, insetsStartOffset, insetsEndOffset, currentValue)
    }

    /**
     * [WindowInsetsAnimationCompat]动画结束
     */
    internal fun onWindowInsetsAnimationEnd(animation: WindowInsetsAnimationCompat) {
        val startView = editorView?.changeRecord?.previousChild
        val endView = editorView?.changeRecord?.currentChild
        dispatchAnimationEnd(startView, endView, insetsStartOffset, insetsEndOffset)
    }

    private fun isIme(editor: Editor?): Boolean {
        return editor != null && editorView != null && editorView!!.ime === editor
    }

    private fun getEditorEndOffset(): Int {
        val editorView = editorView ?: return NO_VALUE
        return editorView.changeRecord.currentChild?.height ?: 0
    }

    private inline fun runSimpleAnimationIfNecessary(
        startOffset: Int,
        endOffset: Int,
        block: ValueAnimator.() -> Unit = {}
    ) {
        simpleAnimation?.end()
        val startView = editorView?.changeRecord?.previousChild
        val endView = editorView?.changeRecord?.currentChild
        if (!canRunAnimation || startOffset == endOffset) {
            onAnimationStart(startView, endView, startOffset, endOffset)
            onAnimationUpdate(startView, endView, startOffset, endOffset, endOffset)
            dispatchAnimationEnd(startView, endView, startOffset, endOffset)
            return
        }

        simpleAnimation = ValueAnimator.ofInt(startOffset, endOffset).apply {
            addListener(
                onStart = { onAnimationStart(startView, endView, startOffset, endOffset) },
                onCancel = { dispatchAnimationEnd(startView, endView, startOffset, endOffset) },
                onEnd = { dispatchAnimationEnd(startView, endView, startOffset, endOffset) }
            )
            addUpdateListener {
                val currentOffset = it.animatedValue as Int
                onAnimationUpdate(startView, endView, startOffset, endOffset, currentOffset)
            }
            duration = ANIMATION_DURATION
            interpolator = ANIMATION_INTERPOLATOR
            block(this)
            start()
        }
    }

    private fun dispatchAnimationEnd(
        startView: View?, endView: View?,
        startOffset: Int, endOffset: Int
    ) {
        if (endOffset != NO_VALUE && inputView != null && inputView!!.editorOffset != endOffset) {
            // 兼容应用退至后台隐藏IME，动画更新不完整的情况
            onAnimationUpdate(startView, endView, startOffset, endOffset, endOffset)
        }
        if (startOffset != NO_VALUE && endOffset != NO_VALUE) {
            onAnimationEnd(startView, endView, startOffset, endOffset)
        }
        removeStartViewAfterAnimationEnd()
        simpleAnimation = null
        willRunInsetsAnimation = false
        willRunSimpleAnimation = false
        insetsStartOffset = NO_VALUE
        insetsEndOffset = NO_VALUE
    }

    private fun addStartViewBeforeRunAnimation() {
        val editorView = editorView ?: return
        val startView = editorView.changeRecord.previousChild
        if (canRunAnimation && startView != null && startView.parent == null) {
            editorView.addView(startView)
        }
    }

    private fun removeStartViewAfterAnimationEnd() {
        val editorView = editorView ?: return
        val startView = editorView.changeRecord.previousChild
        if (canRunAnimation && startView != null && startView.parent === editorView) {
            editorView.removeView(startView)
        }
    }

    private fun endAnimation() {
        val startView = editorView?.changeRecord?.previousChild
        val endView = editorView?.changeRecord?.currentChild
        simpleAnimation?.end()
        if (willRunInsetsAnimation) {
            dispatchAnimationEnd(startView, endView, insetsStartOffset, insetsEndOffset)
        }
    }

    internal fun attach(adapter: EditorAdapter<*>) {
        editorAdapter = adapter
        adapter.addEditorVisibleListener(this)
        onAttachToEditorAdapter(adapter)
    }

    internal fun detach(adapter: EditorAdapter<*>) {
        assert(editorAdapter === adapter) { "EditorAdapter不相同" }
        endAnimation()
        editorAdapter = null
        adapter.removeEditorVisibleListener(this)
        onDetachFromEditorAdapter(adapter)
    }

    companion object {
        private const val NO_VALUE = -1
        private const val ANIMATION_DURATION = 250L
        private val ANIMATION_INTERPOLATOR = DecelerateInterpolator()
    }
}