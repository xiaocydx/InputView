package com.xiaocydx.inputview

import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator

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
 * [Editor]的透明度过渡动画
 */
open class AlphaEditorAnimator : EditorAnimator() {

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
        updateEditorOffset(currentOffset)
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

    override fun getAnimationInterpolator(
        startView: View?, endView: View?,
        startOffset: Int, endOffset: Int
    ): Interpolator {
        // 两个非IME的Editor切换，匀速的动画效果更流畅
        if (startOffset != 0 && endOffset != 0) return LinearInterpolator()
        return super.getAnimationInterpolator(startView, endView, startOffset, endOffset)
    }
}

/**
 * 不运行[Editor]的过渡动画，仅记录Insets动画的状态
 */
class NopEditorAnimator : EditorAnimator() {
    override val canRunAnimation: Boolean = false

    override fun onAnimationUpdate(
        startView: View?, endView: View?,
        startOffset: Int, endOffset: Int, currentOffset: Int
    ) {
        updateEditorOffset(currentOffset)
    }
}