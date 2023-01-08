package com.xiaocydx.inputview

import android.animation.Animator
import android.animation.ValueAnimator
import androidx.annotation.CallSuper
import androidx.core.animation.addListener
import androidx.core.view.doOnPreDraw
import kotlin.math.absoluteValue

/**
 * @author xcc
 * @date 2023/1/8
 */
abstract class EditorAnimator : EditorVisibleListener<EditorType> {
    private var previous: EditorType? = null
    private var current: EditorType? = null
    private var startOffset = NO_OFFSET
    private var endOffset = NO_OFFSET
    private var editorAnimation: Animator? = null
    private var editorAdapter: EditorAdapter<*>? = null
    protected val inputView: InputView?
        get() = editorAdapter?.inputView

    internal fun attach(adapter: EditorAdapter<*>) {
        editorAdapter = adapter
        adapter.addEditorVisibleListener(this)
    }

    internal fun detach(adapter: EditorAdapter<*>) {
        require(editorAdapter === adapter)
        editorAdapter = null
        adapter.removeEditorVisibleListener(this)
    }

    protected abstract fun transform(startOffset: Int, endOffset: Int, currentOffset: Int)

    protected fun isImeType(type: EditorType?): Boolean {
        val editorView = editorAdapter?.editorView
        if (type == null || editorView == null) return false
        return editorView.imeType == type
    }

    protected fun getEditorEndOffset(): Int {
        return editorAdapter?.editorView?.height ?: NO_OFFSET
    }

    @CallSuper
    override fun onVisibleChanged(previous: EditorType?, current: EditorType?) {
        this.previous = previous
        this.current = current
        if (editorAnimation?.isRunning == true) {
            editorAnimation?.cancel()
        }
        if (isImeType(previous) || isImeType(current)) {
            return
        }
        val inputView = editorAdapter?.inputView ?: return
        val startOffset = inputView.editorOffset
        inputView.doOnPreDraw {
            val endOffset = getEditorEndOffset()
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
                // TODO: 计算时长
                val p = (startOffset - endOffset).absoluteValue.toFloat()
                duration = (2000 * (p / inputView.height)).toLong()
                start()
            }
        }
    }

    @CallSuper
    open fun onImeAnimationStart(endValue: Int) {
        val inputView = editorAdapter?.inputView ?: return
        startOffset = inputView.editorOffset
        endOffset = when {
            isImeType(current) -> endValue
            else -> getEditorEndOffset()
        }
    }

    @CallSuper
    open fun onImeAnimationUpdate(currentValue: Int) {
        if (startOffset == endOffset) return
        val finalOffset = when {
            endOffset == NO_OFFSET -> currentValue
            isImeType(previous) && current != null -> if (startOffset < endOffset) {
                (startOffset + (startOffset - currentValue)).coerceAtMost(endOffset)
            } else {
                currentValue.coerceAtLeast(endOffset)
            }
            previous != null && isImeType(current) -> if (startOffset < endOffset) {
                currentValue.coerceAtLeast(startOffset)
            } else {
                (startOffset - currentValue).coerceAtLeast(endOffset)
            }
            else -> currentValue
        }
        transform(startOffset, endOffset, finalOffset)
    }

    @CallSuper
    open fun onImeAnimationEnd() {
        startOffset = NO_OFFSET
        endOffset = NO_OFFSET
    }

    private companion object {
        const val NO_OFFSET = -1
    }
}