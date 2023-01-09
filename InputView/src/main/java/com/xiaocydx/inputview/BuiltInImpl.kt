package com.xiaocydx.inputview

import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE

/**
 * [Editor]的IME默认实现
 */
object Ime : Editor

/**
 * 支持只需要IME的场景
 */
class ImeAdapter : EditorAdapter<Ime>() {
    override val editors: List<Ime> = listOf(Ime)

    override fun isIme(editor: Ime): Boolean = editor === Ime

    override fun onCreateView(parent: ViewGroup, editor: Ime): View? = null
}

/**
 * 运行动画平移`contentView`，类似[SOFT_INPUT_ADJUST_PAN]
 */
fun EditorAnimator.Companion.pan(): EditorAnimator {
    return EditorAnimatorImpl(isResize = false, canRunAnimation = true)
}

/**
 * 运行动画修改`contentView`的尺寸，类似[SOFT_INPUT_ADJUST_RESIZE]
 */
fun EditorAnimator.Companion.resize(): EditorAnimator {
    return EditorAnimatorImpl(isResize = true, canRunAnimation = true)
}

/**
 * 不运行动画平移`contentView`，类似[SOFT_INPUT_ADJUST_PAN]
 */
fun EditorAnimator.Companion.nopPan(): EditorAnimator {
    return EditorAnimatorImpl(isResize = false, canRunAnimation = false)
}

/**
 * 不运行动画修改`contentView`的尺寸，类似[SOFT_INPUT_ADJUST_RESIZE]
 */
fun EditorAnimator.Companion.nopResize(): EditorAnimator {
    return EditorAnimatorImpl(isResize = true, canRunAnimation = false)
}

private class EditorAnimatorImpl(
    private val isResize: Boolean,
    canRunAnimation: Boolean
) : EditorAnimator(canRunAnimation) {

    override fun onAnimationStart(
        startView: View?, endView: View?,
        startOffset: Int, endOffset: Int
    ) {
        startView?.alpha = 1f
        endView?.alpha = 0f
    }

    override fun onAnimationUpdate(
        startView: View?, endView: View?,
        startOffset: Int, endOffset: Int, currentOffset: Int
    ) {
        val inputView = inputView ?: return
        if (isResize) {
            inputView.offsetContentSize(currentOffset)
        } else {
            val previousOffset = inputView.editorOffset
            val diff = previousOffset - currentOffset
            inputView.offsetChildrenLocation(currentOffset, diff)
        }

        var start = startOffset
        var end = endOffset
        var current = currentOffset
        if (start > end) {
            // 反转start到end的过程，只按start < end计算alpha
            val diff = start - current
            start = end.also { end = start }
            current = start + diff
        }

        var threshold = (end - start) * 0.4f
        threshold = threshold.coerceAtLeast(0.5f)
        when {
            current >= 0 && current <= start + threshold -> {
                val fraction = (current - start) / threshold
                startView?.alpha = 1 - fraction
                endView?.alpha = 0f
            }
            current >= end - threshold && current <= end -> {
                val fraction = (current - (end - threshold)) / threshold
                startView?.alpha = 0f
                endView?.alpha = fraction
            }
            else -> {
                startView?.alpha = 0f
                endView?.alpha = 0f
            }
        }
    }

    override fun onAnimationEnd(
        startView: View?, endView: View?,
        startOffset: Int, endOffset: Int
    ) {
        startView?.alpha = 1f
        endView?.alpha = 1f
    }
}