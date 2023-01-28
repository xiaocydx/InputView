package com.xiaocydx.inputview

import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import androidx.annotation.FloatRange
import androidx.annotation.IntRange

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
    /**
     * 淡入淡出的原始分数进度阈值
     *
     * 以`thresholdFraction = 0.4f`、`startView != null`、`endView != null`为例：
     * 1. [AnimationState.animatedFraction]在`[0f, 0.4f]`，
     * `startView.alpha`从`1f`变化到`0f`，`endView.alpha`保持为`0f`。
     *
     * 2. [AnimationState.animatedFraction]在`[0.4f, 0.6f]`，
     * `startView.alpha`和`endView.alpha`保持为`0f`。
     *
     * 3. [AnimationState.animatedFraction]在`[0.6f, 1f]`，
     * `startView.alpha`保持为`0f`，`endView.alpha`从`0f`变化到`1f`。
     */
    @FloatRange(from = 0.0, to = 0.5)
    private val thresholdFraction: Float = 0.4f,

    /**
     * 动画时长
     *
     * 显示或隐藏IME运行的动画：
     * 1. Android 11以下兼容代码的`durationMillis = 160ms`，
     * 内部实现不会将IME动画的时长修改为[durationMillis]，
     * 原因是Android 11以下无法跟IME完全贴合，保持兼容代码即可。
     *
     * 2. Android 11及以上系统代码的`durationMillis = 285ms`，
     * 内部实现会尝试将IME动画的时长修改为[durationMillis]。
     *
     * **注意**：实际动画时长以[AnimationState.durationMillis]为准。
     */
    @IntRange(from = 0)
    durationMillis: Long = ANIMATION_DURATION_MILLIS,

    /**
     * 动画插值器
     *
     * 显示或隐藏IME运行的动画：
     * 1. Android 11以下兼容代码的`interpolator = DecelerateInterpolator()`，
     * 内部实现不会将IME动画的插值器修改为[interpolator]，
     * 原因是Android 11以下无法跟IME完全贴合，保持兼容代码即可。
     *
     * 2. Android 11及以上系统代码的`interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)`，
     * 内部实现会尝试将IME动画的插值器修改为[interpolator]。
     *
     * **注意**：实际动画插值器以[AnimationState.interpolator]为准。
     */
    interpolator: Interpolator = ANIMATION_INTERPOLATOR
) : EditorAnimator(durationMillis, interpolator) {

    init {
        addAnimationCallback(EditorOffsetAndAlphaUpdater())
    }

    private inner class EditorOffsetAndAlphaUpdater : AnimationCallback {

        override fun onAnimationStart(state: AnimationState) {
            state.startView?.alpha = 1f
            state.endView?.alpha = 0f
        }

        override fun onAnimationUpdate(state: AnimationState): Unit = with(state) {
            var start = startOffset
            var end = endOffset
            var offset = (start + (end - start) * animatedFraction).toInt()

            if (!isIme(previous) && !isIme(current)
                    && startView != null && endView != null) {
                // 两个非IME的Editor切换，调整为匀速动画
                updateEditorOffset(currentOffset = offset)
            } else {
                updateEditorOffset(currentOffset)
            }

            if (start > end) {
                // 反转start到end的过程，只按start < end计算alpha
                val diff = start - offset
                start = end.also { end = start }
                offset = start + diff
            }

            val threshold = (end - start) * thresholdFraction
            when {
                startView == null && endView != null -> {
                    endView!!.alpha = animatedFraction
                }
                startView != null && endView == null -> {
                    startView!!.alpha = 1 - animatedFraction
                }
                offset >= 0 && offset <= start + threshold -> {
                    val fraction = (offset - start) / threshold
                    startView?.alpha = 1 - fraction
                    endView?.alpha = 0f
                }
                offset >= end - threshold && offset <= end -> {
                    val fraction = (offset - (end - threshold)) / threshold
                    startView?.alpha = 0f
                    endView?.alpha = fraction
                }
                else -> {
                    startView?.alpha = 0f
                    endView?.alpha = 0f
                }
            }
        }

        override fun onAnimationEnd(state: AnimationState) {
            state.startView?.alpha = 1f
            state.endView?.alpha = 1f
        }
    }
}

/**
 * 不运行[Editor]的过渡动画，仅记录动画状态，分发动画回调
 */
class NopEditorAnimator : EditorAnimator() {
    override val canRunAnimation: Boolean = false

    init {
        addAnimationCallback(EditorOffsetUpdater())
    }

    private inner class EditorOffsetUpdater : AnimationCallback {
        override fun onAnimationUpdate(state: AnimationState) {
            updateEditorOffset(state.currentOffset)
        }
    }
}