package com.xiaocydx.inputview

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.addListener
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.doOnPreDraw

/**
 * [InputView]编辑区的[Editor]切换动画
 *
 * 1. [onVisibleChanged]处理`previous`和`current`不是IME的动画。
 * 2. [onImeAnimationStart]、[onImeAnimationUpdate]、[onImeAnimationEnd]，
 * 处理`previous`和`current`其中一个是IME的动画。
 *
 * @author xcc
 * @date 2023/1/8
 */
abstract class EditorAnimator(
    private val canRunAnimation: Boolean = true
) : EditorVisibleListener<Editor> {
    private var previous: Editor? = null
    private var current: Editor? = null
    private var imeStartOffset = NO_OFFSET
    private var imeEndOffset = NO_OFFSET
    private var willRunImeAnimation = false
    private var preDrawRunAnimation = false
    private var editorAnimation: Animator? = null
    private var editorAdapter: EditorAdapter<*>? = null
    protected val inputView: InputView?
        get() = editorAdapter?.inputView

    /**
     * 动画开始
     *
     * @param startOffset   编辑区的起始偏移值
     * @param endOffset     编辑区的结束偏移值
     */
    protected open fun onAnimationStart(startOffset: Int, endOffset: Int) = Unit

    /**
     * 动画更新
     *
     * @param startOffset   编辑区的起始偏移值
     * @param endOffset     编辑区的结束偏移值
     * @param currentOffset 编辑区的当前偏移值
     */
    protected open fun onAnimationUpdate(startOffset: Int, endOffset: Int, currentOffset: Int) = Unit

    /**
     * 动画结束
     *
     * @param startOffset   编辑区的起始偏移值
     * @param endOffset     编辑区的结束偏移值
     */
    protected open fun onAnimationEnd(startOffset: Int, endOffset: Int) = Unit

    /**
     * 当前[EditorAnimator]添加到[adapter]
     */
    protected open fun onAttachToEditorAdapter(adapter: EditorAdapter<*>) = Unit

    /**
     * 当前[EditorAnimator]从[adapter]移除
     */
    protected open fun onDetachFromEditorAdapter(adapter: EditorAdapter<*>) = Unit

    private fun isIme(editor: Editor?): Boolean {
        val editorView = editorAdapter?.editorView
        if (editor == null || editorView == null) return false
        return editorView.ime == editor
    }

    private fun getEditorEndOffset(): Int {
        return editorAdapter?.editorView?.height ?: NO_OFFSET
    }

    private inline fun runEditorAnimationIfNecessary(
        startOffset: Int,
        endOffset: Int,
        block: ValueAnimator.() -> Unit = {}
    ) {
        editorAnimation?.takeIf { it.isRunning }?.end()
        if (startOffset == endOffset) return
        if (!canRunAnimation) {
            onAnimationStart(startOffset, endOffset)
            onAnimationUpdate(startOffset, endOffset, endOffset)
            onAnimationEnd(startOffset, endOffset)
            return
        }

        editorAnimation = ValueAnimator.ofInt(startOffset, endOffset).apply {
            addListener(
                onStart = {
                    onAnimationStart(startOffset, endOffset)
                },
                onCancel = {
                    editorAnimation = null
                    onAnimationUpdate(startOffset, endOffset, endOffset)
                    onAnimationEnd(startOffset, endOffset)
                },
                onEnd = {
                    editorAnimation = null
                    onAnimationEnd(startOffset, endOffset)
                }
            )
            addUpdateListener {
                val currentOffset = it.animatedValue as Int
                onAnimationUpdate(startOffset, endOffset, currentOffset)
            }
            duration = ANIMATION_DURATION
            interpolator = ANIMATION_INTERPOLATOR
            this.block()
            start()
        }
    }

    final override fun onVisibleChanged(previous: Editor?, current: Editor?) {
        this.previous = previous
        this.current = current
        editorAnimation?.takeIf { it.isRunning }?.end()
        willRunImeAnimation = isIme(previous) || isIme(current)
        if (willRunImeAnimation) return

        val inputView = editorAdapter?.inputView
        if (inputView == null || preDrawRunAnimation) return
        preDrawRunAnimation = true
        val startOffset = inputView.editorOffset
        inputView.doOnPreDraw {
            preDrawRunAnimation = false
            runEditorAnimationIfNecessary(startOffset, getEditorEndOffset())
        }
    }

    /**
     * IME动画开始
     *
     * @param endValue  IME动画结束时的偏移值
     * @param animation IME动画
     */
    internal fun onImeAnimationStart(endValue: Int, animation: WindowInsetsAnimationCompat) {
        val inputView = editorAdapter?.inputView
        if (!willRunImeAnimation || inputView == null) return

        imeStartOffset = inputView.editorOffset
        imeEndOffset = if (isIme(current)) endValue else getEditorEndOffset()
        val imeToOther = isIme(previous) && current != null && !isIme(current)
        val otherToIme = previous != null && !isIme(previous) && isIme(current)
        when {
            imeStartOffset == imeEndOffset -> resetImeOffset()
            !canRunAnimation || imeToOther || otherToIme -> {
                // imeToOther的animation是0到imeEndOffset，
                // otherToIme的animation是imeStartOffset到0，
                // 这两种情况依靠animation进行更新，不好实现流畅的过渡效果，
                // 因此用animation的duration和interpolator运行动画进行更新。
                runEditorAnimationIfNecessary(imeStartOffset, imeEndOffset) {
                    duration = animation.durationMillis
                    interpolator = animation.interpolator ?: ANIMATION_INTERPOLATOR
                }
                resetImeOffset()
            }
            else -> onAnimationStart(imeStartOffset, imeEndOffset)
        }
    }

    /**
     * IME动画更新
     *
     * @param currentValue IME动画运行中的偏移值
     * @param animation    IME动画
     */
    internal fun onImeAnimationUpdate(currentValue: Int, animation: WindowInsetsAnimationCompat) {
        if (imeStartOffset == NO_OFFSET || imeEndOffset == NO_OFFSET) return
        onAnimationUpdate(imeStartOffset, imeEndOffset, currentValue)
    }

    /**
     * IME动画结束
     *
     * @param animation IME动画
     */
    internal fun onImeAnimationEnd(animation: WindowInsetsAnimationCompat) {
        if (imeStartOffset == NO_OFFSET || imeEndOffset == NO_OFFSET) return
        if (inputView != null && inputView!!.editorOffset != imeEndOffset) {
            // 兼容应用退至后台隐藏IME，onImeAnimationUpdate()执行不完整的情况
            onAnimationUpdate(imeStartOffset, imeEndOffset, imeEndOffset)
        }
        onAnimationEnd(imeStartOffset, imeEndOffset)
        resetImeOffset()
    }

    private fun resetImeOffset() {
        imeStartOffset = NO_OFFSET
        imeEndOffset = NO_OFFSET
    }

    internal fun attach(adapter: EditorAdapter<*>) {
        editorAdapter = adapter
        adapter.addEditorVisibleListener(this)
        onAttachToEditorAdapter(adapter)
    }

    internal fun detach(adapter: EditorAdapter<*>) {
        assert(editorAdapter === adapter) { "EditorAdapter不相同" }
        editorAdapter = null
        adapter.removeEditorVisibleListener(this)
        onDetachFromEditorAdapter(adapter)
    }

    companion object {
        private const val NO_OFFSET = -1
        private const val ANIMATION_DURATION = 250L
        private val ANIMATION_INTERPOLATOR = AccelerateDecelerateInterpolator()
    }
}