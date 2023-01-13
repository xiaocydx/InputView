package com.xiaocydx.inputview

import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import androidx.annotation.FloatRange

/**
 * [Editor]的IME默认实现
 */
object Ime : Editor

/**
 * 用于只需要IME的场景
 *
 * ```
 * val adapter = ImeAdapter()
 * inputView.editorAdapter = adapter
 *
 * // 显示IME
 * adapter.notifyShowIme()
 *
 * // 隐藏IME
 * adapter.notifyHideIme()
 * ```
 */
class ImeAdapter : EditorAdapter<Ime>() {
    override val editors: List<Ime> = listOf(Ime)

    override fun isIme(editor: Ime): Boolean = editor === Ime

    override fun onCreateView(parent: ViewGroup, editor: Ime): View? = null
}

/**
 * [Editor]的淡入淡出动画
 */
open class FadeEditorAnimator(
    @FloatRange(from = 0.0, to = 0.5)
    private val thresholdFraction: Float = 0.4f
) : EditorAnimator() {

    override fun onAnimationStart(state: AnimationState): Unit = with(state) {
        startView?.alpha = 1f
        endView?.alpha = 0f
    }

    override fun onAnimationUpdate(state: AnimationState): Unit = with(state) {
        var start = startOffset
        var end = endOffset
        var current = currentOffset
        if (start > end) {
            // 反转start到end的过程，只按start < end计算alpha
            val diff = start - current
            start = end.also { end = start }
            current = start + diff
        }

        val threshold = (end - start) * thresholdFraction
        when {
            startView == null && endView != null -> {
                endView!!.alpha = fraction
            }
            startView != null && endView == null -> {
                startView!!.alpha = 1 - fraction
            }
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

    override fun onAnimationEnd(state: AnimationState): Unit = with(state) {
        startView?.alpha = 1f
        endView?.alpha = 1f
    }

    override fun getAnimationInterpolator(state: AnimationState): Interpolator = state.run {
        // 两个非IME的Editor切换，匀速的动画效果更流畅
        if (startView != null && endView != null) return LinearInterpolator()
        super.getAnimationInterpolator(state)
    }
}

/**
 * 不运行[Editor]的过渡动画，仅记录动画状态，分发动画回调
 */
class NopEditorAnimator : EditorAnimator() {
    override val canRunAnimation: Boolean = false
}