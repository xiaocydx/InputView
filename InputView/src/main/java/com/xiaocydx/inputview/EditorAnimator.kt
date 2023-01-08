package com.xiaocydx.inputview

import android.animation.Animator
import android.animation.ValueAnimator
import androidx.annotation.CallSuper
import androidx.core.animation.addListener
import androidx.core.view.doOnPreDraw
import kotlin.math.absoluteValue

/**
 * [InputView]编辑区的编辑器动画
 *
 * 1. [onVisibleChanged]处理`previous`和`current`不是IME的动画，
 * 2. [onImeAnimationStart]、[onImeAnimationUpdate]、[onImeAnimationEnd]，
 * 处理`previous`和`current`其中一个是IME的动画。
 *
 * @author xcc
 * @date 2023/1/8
 */
abstract class EditorAnimator : EditorVisibleListener<Editor> {
    private var previous: Editor? = null
    private var current: Editor? = null
    private var startOffset = NO_OFFSET
    private var endOffset = NO_OFFSET
    private var handleImeUpdate = false
    private var editorAnimation: Animator? = null
    private var editorAdapter: EditorAdapter<*>? = null
    protected val inputView: InputView?
        get() = editorAdapter?.inputView

    /**
     * 执行动画的变换操作
     *
     * @param startOffset   [InputView]编辑区的起始偏移值
     * @param endOffset     [InputView]编辑区的结束偏移值
     * @param currentOffset [InputView]编辑区的当前偏移值
     */
    protected abstract fun transform(startOffset: Int, endOffset: Int, currentOffset: Int)

    /**
     * [editor]是否为IME
     */
    protected fun isIme(editor: Editor?): Boolean {
        val editorView = editorAdapter?.editorView
        if (editor == null || editorView == null) return false
        return editorView.ime == editor
    }

    /**
     * 获取[InputView]编辑区的高度，可作为动画的结束偏移值
     */
    protected fun getEditorHeight(): Int {
        return editorAdapter?.editorView?.height ?: NO_OFFSET
    }

    /**
     * 当前[EditorAnimator]添加到[adapter]
     */
    protected fun onAttachToEditorAdapter(adapter: EditorAdapter<*>) = Unit

    /**
     * 当前[EditorAnimator]从[adapter]移除
     */
    protected fun onDetachFromEditorAdapter(adapter: EditorAdapter<*>) = Unit

    @CallSuper
    override fun onVisibleChanged(previous: Editor?, current: Editor?) {
        this.previous = previous
        this.current = current
        if (editorAnimation?.isRunning == true) {
            editorAnimation?.cancel()
        }
        handleImeUpdate = isIme(previous) || isIme(current)
        if (handleImeUpdate) return

        val inputView = editorAdapter?.inputView ?: return
        val startOffset = inputView.editorOffset
        inputView.doOnPreDraw {
            val endOffset = getEditorHeight()
            if (startOffset == endOffset) return@doOnPreDraw
            editorAnimation = ValueAnimator.ofInt(startOffset, endOffset).apply {
                addUpdateListener {
                    val currentOffset = it.animatedValue as Int
                    transform(startOffset, endOffset, currentOffset)
                }
                addListener(
                    onCancel = { editorAnimation = null },
                    onEnd = { editorAnimation = null }
                )
                // TODO: 计算时长和设置插值器
                val p = (startOffset - endOffset).absoluteValue.toFloat()
                duration = (2000 * (p / inputView.height)).toLong()
                start()
            }
        }
    }

    /**
     * IME动画开始
     *
     * @param endValue IME动画结束时的偏移值
     */
    @CallSuper
    open fun onImeAnimationStart(endValue: Int) {
        val inputView = editorAdapter?.inputView ?: return
        startOffset = inputView.editorOffset
        endOffset = when {
            isIme(current) -> endValue
            else -> getEditorHeight()
        }
    }

    /**
     * IME动画更新
     *
     * @param currentValue IME动画运行中的偏移值
     */
    @CallSuper
    open fun onImeAnimationUpdate(currentValue: Int) {
        if (!handleImeUpdate || startOffset == endOffset) return
        val finalOffset = when {
            endOffset == NO_OFFSET -> currentValue
            isIme(previous) && current != null -> if (startOffset < endOffset) {
                (startOffset + (startOffset - currentValue)).coerceAtMost(endOffset)
            } else {
                currentValue.coerceAtLeast(endOffset)
            }
            previous != null && isIme(current) -> if (startOffset < endOffset) {
                currentValue.coerceAtLeast(startOffset)
            } else {
                (startOffset - currentValue).coerceAtLeast(endOffset)
            }
            else -> currentValue
        }
        transform(startOffset, endOffset, finalOffset)
    }

    /**
     * IME动画结束
     */
    @CallSuper
    open fun onImeAnimationEnd() {
        startOffset = NO_OFFSET
        endOffset = NO_OFFSET
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

    private companion object {
        const val NO_OFFSET = -1
    }
}