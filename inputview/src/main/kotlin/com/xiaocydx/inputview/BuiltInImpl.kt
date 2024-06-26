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

import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.widget.EditText
import androidx.annotation.FloatRange
import androidx.annotation.IntRange

/**
 * 禁用手势导航栏偏移，在支持手势导航栏EdgeToEdge的情况下，
 * `contentView`和编辑区也不会有手势导航栏高度的初始偏移，
 * 当调用者需要自行处理手势导航栏时，可以调用该函数。
 *
 * 关于手势导航栏偏移的描述，可以看[InputView.onApplyWindowInsets]的注释。
 */
fun InputView.disableGestureNavBarOffset() = disableNavBarOffset()

/**
 * 设置[WindowFocusInterceptor]
 *
 * 若有更多的拦截条件，则自行实现和组合[AnimationInterceptor]，
 * 详细解释可以看[EditorAnimator.setAnimationInterceptor]的注释。
 */
fun EditorAnimator.setWindowFocusInterceptor() {
    setAnimationInterceptor(WindowFocusInterceptor())
}

/**
 * `window.decorView.hasWindowFocus()`的动画拦截器
 *
 * 当更改[Editor]时，若`window.decorView.hasWindowFocus()`为`false`，则不更改[Editor]且不运行动画，
 * 该拦截器适用于存在多个Window的交互场景，例如显示了有[EditText]的[Dialog]，点击[EditText]显示IME，
 * 此时不需要将[Editor]更改为IME，也不需要运行IME动画。
 */
class WindowFocusInterceptor : AnimationInterceptor {
    private var animator: EditorAnimator? = null

    override fun onAttachedToAnimator(animator: EditorAnimator) {
        check(this.animator == null) { "WindowFocusInterceptor已关联EditorAnimator" }
        this.animator = animator
    }

    override fun onDetachedFromAnimator(animator: EditorAnimator) {
        this.animator = null
    }

    override fun onInterceptChange(current: Editor?, next: Editor?): Boolean {
        val host = animator?.getEditorHost() ?: return false
        return next != null && host.isRestored && !host.hasWindowFocus
    }
}

/**
 * 两个非IME的[Editor]切换，调整为线性更新编辑区的偏移值
 */
fun EditorAnimator.linearEditorOffset() {
    if (!canRunAnimation) return
    removeAnimationCallback(LinearEditorOffsetCallback)
    addAnimationCallback(LinearEditorOffsetCallback)
}

private object LinearEditorOffsetCallback : AnimationCallback {
    override fun onAnimationUpdate(state: AnimationState): Unit = with(state) {
        if (!isIme(previous) && !isIme(current) && startView != null && endView != null) {
            updateEditorOffset((startOffset + (endOffset - startOffset) * animatedFraction).toInt())
        }
    }
}

/**
 * [EditorAnimator.addAnimationCallback]的简化函数
 *
 * @param onPrepare 对应[AnimationCallback.onAnimationPrepare]
 * @param onStart   对应[AnimationCallback.onAnimationStart]
 * @param onUpdate  对应[AnimationCallback.onAnimationUpdate]
 * @param onEnd     对应[AnimationCallback.onAnimationEnd]
 * @return 返回添加的[AnimationCallback]，可用于[EditorAnimator.removeAnimationCallback]
 */
inline fun EditorAnimator.addAnimationCallback(
    crossinline onPrepare: (previous: Editor?, current: Editor?) -> Unit = { _, _ -> },
    crossinline onStart: (state: AnimationState) -> Unit = {},
    crossinline onUpdate: (state: AnimationState) -> Unit = {},
    crossinline onEnd: (state: AnimationState) -> Unit = {}
) = object : AnimationCallback {
    override fun onAnimationPrepare(previous: Editor?, current: Editor?) = onPrepare(previous, current)
    override fun onAnimationStart(state: AnimationState) = onStart(state)
    override fun onAnimationUpdate(state: AnimationState) = onUpdate(state)
    override fun onAnimationEnd(state: AnimationState) = onEnd(state)
}.also(::addAnimationCallback)

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
    override val ime: Ime = Ime
    override fun onCreateView(parent: ViewGroup, editor: Ime): View? = null
}

/**
 * [Editor]的淡入淡出动画
 */
class FadeEditorAnimator(
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
    val thresholdFraction: Float = 0.4f,

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
        setAnimationCallback(object : AnimationCallback {
            override fun onAnimationStart(state: AnimationState) {
                state.startView?.alpha = 1f
                state.endView?.alpha = 0f
            }

            override fun onAnimationUpdate(state: AnimationState): Unit = with(state) {
                updateEditorOffset(currentOffset)
                startView?.alpha = calculateAlpha(state, matchNull = true, start = true)
                endView?.alpha = calculateAlpha(state, matchNull = true, start = false)
            }

            override fun onAnimationEnd(state: AnimationState) {
                state.startView?.alpha = 1f
                state.endView?.alpha = 1f
            }
        })
    }

    /**
     * 根据[state]计算`startView.alpha`或`endView.alpha`
     *
     * @param state     [AnimationCallback]回调函数的参数
     * @param matchNull 是否匹配`startView`或`endView`为`null`的分支
     * @param start     `true`-计算`startView.alpha`，`false`-计算`endView.alpha`
     */
    fun calculateAlpha(
        state: AnimationState,
        matchNull: Boolean = false,
        start: Boolean
    ) = with(state) {
        val current = animatedFraction
        val threshold = thresholdFraction
        when {
            matchNull && startView == null && endView != null -> {
                if (start) 0f else current
            }
            matchNull && startView != null && endView == null -> {
                if (start) 1 - current else 0f
            }
            current <= threshold -> {
                if (start) 1f - (current / threshold) else 0f
            }
            current >= 1f - threshold -> {
                if (start) 0f else (current - (1f - threshold)) / threshold
            }
            else -> 0f
        }
    }
}

/**
 * 不运行[Editor]的动画，仅记录动画状态，分发动画回调
 */
class NopEditorAnimator : EditorAnimator() {
    override val canRunAnimation: Boolean = false

    init {
        setAnimationCallback(object : AnimationCallback {
            override fun onAnimationUpdate(state: AnimationState) {
                state.apply { updateEditorOffset(currentOffset) }
            }
        })
    }
}