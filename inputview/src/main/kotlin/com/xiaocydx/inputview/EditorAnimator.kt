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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Build
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.VisibleForTesting
import androidx.core.view.OneShotPreDrawListener
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.ime
import com.xiaocydx.insets.contains
import com.xiaocydx.insets.imeHeight
import java.lang.Integer.max
import kotlin.math.absoluteValue
import kotlin.math.min

/**
 * [Editor]的动画实现类
 *
 * @author xcc
 * @date 2023/1/8
 */
abstract class EditorAnimator(
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
    val durationMillis: Long = ANIMATION_DURATION_MILLIS,

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
    val interpolator: Interpolator = ANIMATION_INTERPOLATOR
) {
    private var host: EditorHost? = null
    private var animationRecord: AnimationRecord? = null
    private val animationDispatcher = AnimationDispatcher()
    private var animationInterceptor = emptyAnimationInterceptor()
    private var animationCallback: AnimationCallback? = null
    private val animationCallbacks = ArrayList<AnimationCallback>(2)
    internal open val canRunAnimation: Boolean = true
    internal val isActive: Boolean
        get() = animationRecord != null

    /**
     * 动画是否运行中
     */
    val isRunning: Boolean
        get() = animationRecord?.isRunning == true

    /**
     * 设置[AnimationInterceptor]，应用场景可以参考[setWindowFocusInterceptor]
     *
     * 若有更多的拦截条件，则自行实现和组合[AnimationInterceptor]，例如：
     * ```
     * val interceptor1 = CustomAnimationInterceptor()
     * val interceptor2 = editorAnimator.createWindowFocusInterceptor()
     * editorAnimator.setAnimationInterceptor(interceptor1 + interceptor2)
     * ```
     */
    fun setAnimationInterceptor(interceptor: AnimationInterceptor) {
        animationInterceptor = interceptor
    }

    /**
     * 添加[AnimationCallback]
     *
     * 在[AnimationCallback]的各个函数可以调用[removeAnimationCallback]，
     * 先添加的[AnimationCallback]后执行，即倒序遍历[AnimationCallback]。
     */
    fun addAnimationCallback(callback: AnimationCallback) {
        if (animationCallbacks.contains(callback)) return
        animationCallbacks.add(callback)
        dispatchAddedAnimationCallback(callback)
    }

    /**
     * 移除[AnimationCallback]
     */
    fun removeAnimationCallback(callback: AnimationCallback) {
        animationCallbacks.remove(callback)
    }

    /**
     * 设置最先分发的[callback]，该函数仅由实现类调用
     */
    protected fun setAnimationCallback(callback: AnimationCallback) {
        require(!isActive) { "实现类只能在初始化时调用该函数" }
        this.animationCallback = callback
    }

    internal fun forEachCallback(action: (AnimationCallback) -> Unit) {
        animationCallbacks.forEach(action)
    }

    @VisibleForTesting
    internal fun containsCallback(callback: AnimationCallback): Boolean {
        return animationCallbacks.contains(callback)
    }

    private fun resetAnimationRecord(record: AnimationRecord) {
        endAnimation()
        assert(animationRecord == null) { "animationRecord未被置空" }
        animationRecord = record
        dispatchAnimationPrepare(record)
    }

    private fun runSimpleAnimationIfNecessary(record: AnimationRecord) {
        if (record.isImmediately()) {
            dispatchAnimationStart(record)
            dispatchAnimationEnd(record)
            return
        }

        ValueAnimator.ofFloat(0f, 1f).apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    dispatchAnimationStart(record)
                }

                override fun onAnimationEnd(animation: Animator) {
                    dispatchAnimationEnd(record)
                }
            })
            addUpdateListener { dispatchAnimationUpdate(record) }
            duration = durationMillis
            interpolator = LinearInterpolator()
            record.setSimpleAnimation(this)
            start()
        }
    }

    private fun dispatchAnimationPrepare(record: AnimationRecord) {
        dispatchAnimationCallback { onAnimationPrepare(record.previous, record.current) }
    }

    private fun dispatchAnimationStart(record: AnimationRecord) {
        if (!record.checkAnimationOffset()) return
        dispatchAnimationCallback { onAnimationStart(record) }
    }

    private fun dispatchAnimationUpdate(record: AnimationRecord) {
        if (!record.checkAnimationOffset()) return
        record.updateAnimationCurrent()
        dispatchAnimationCallback { onAnimationUpdate(record) }
    }

    private fun dispatchAnimationEnd(record: AnimationRecord) {
        if (record.checkAnimationOffset()) {
            record.updateAnimationToEnd()
            dispatchAnimationUpdate(record)
            dispatchAnimationCallback { onAnimationEnd(record) }
        }
        record.removeStartViewIfNecessary()
        record.removePreDrawRunSimpleAnimation()
        animationRecord = null
    }

    private fun dispatchAddedAnimationCallback(callback: AnimationCallback) {
        val record = animationRecord ?: return
        if (record.isRunning && record.checkAnimationOffset()) {
            // 每个函数都支持移除callback，因此需要再次检查callbacks是否包含callback
            callback.onAnimationPrepare(record.previous, record.current)
            callback.takeIf { animationCallbacks.contains(it) }?.onAnimationStart(record)
            callback.takeIf { animationCallbacks.contains(it) }?.onAnimationUpdate(record)
        }
    }

    private inline fun dispatchAnimationCallback(action: AnimationCallback.() -> Unit) {
        animationCallback?.action()
        for (index in animationCallbacks.indices.reversed()) animationCallbacks[index].apply(action)
    }

    internal fun getEditorHost() = host

    internal fun canChangeEditor(current: Editor?, next: Editor?): Boolean {
        return !animationInterceptor.onInterceptChange(current, next)
    }

    internal fun endAnimation() {
        animationRecord?.endAnimation()
    }

    internal fun calculateEndOffset(): Int {
        host?.apply {
            return if (current === ime) {
                // 当前是IME，若还未进行WindowInsets分发，更新lastInsets，
                // 则lastInsets.imeOffset为0，这种情况imeOffset不是有效值。
                val lastInsets = animationDispatcher.lastInsets
                val offset = lastInsets?.imeOffset ?: NO_VALUE
                if (offset != 0) offset else NO_VALUE
            } else {
                currentView?.measuredHeight ?: 0
            }
        }
        return NO_VALUE
    }

    internal fun onPendingChanged(previous: Editor?, current: Editor?, immediately: Boolean) {
        if (host == null) return
        animationDispatcher.onPendingChanged(previous, current, immediately)
    }

    internal fun onAttachedToHost(host: EditorHost) {
        this.host = host
        host.setOnApplyWindowInsetsListener(animationDispatcher)
        host.takeIf { enableWindowInsetsAnimation() }
            ?.setWindowInsetsAnimationCallback(durationMillis, interpolator, animationDispatcher)
    }

    internal fun onDetachedFromHost(host: EditorHost) {
        endAnimation()
        host.setOnApplyWindowInsetsListener(null)
        host.takeIf { enableWindowInsetsAnimation() }
            ?.setWindowInsetsAnimationCallback(durationMillis, interpolator, callback = null)
        this.host = null
    }

    private fun enableWindowInsetsAnimation(): Boolean {
        return canRunAnimation && (Build.VERSION.SDK_INT >= 30 || ENABLE_INSETS_ANIMATION_BELOW_R)
    }

    private inner class AnimationDispatcher : OnApplyWindowInsetsListenerCompat,
            WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
        private var lastImeHeight = 0
        var stableImeHeight = 0; private set
        var lastInsets: WindowInsetsCompat? = null; private set

        /**
         * ### StartView和EndView
         * 当[canRunAnimation]为`true`时，[EditorHost]更改[Editor]会保留之前[Editor]的子View，
         * 参与动画过程，动画结束时调用[AnimationRecord.removeStartViewIfNecessary]移除子View，
         * 之前[Editor]的子View作为StartView，当前[Editor]的子View作为EndView。
         *
         * ### InsetsAnimation和SimpleAnimation
         * 若[previous]和[current]其中一个是IME，则通过[AnimationDispatcher]运行InsetsAnimation。
         * 若[previous]和[current]不是IME，则调用[runSimpleAnimationIfNecessary]运行SimpleAnimation，
         *
         * ### 主动更改和被动更改
         * 直接调用[EditorHost.showChecked]或[EditorHost.hideChecked]更改[Editor]属于主动更改，
         * 点击EditText显示IME、按返回键隐藏IME等操作，会在[onApplyWindowInsets]判断IME的显示情况，
         * 然后调用[EditorHost.dispatchImeShown]更改[Editor]，这属于被动更改。
         */
        fun onPendingChanged(previous: Editor?, current: Editor?, immediately: Boolean) {
            val record = AnimationRecord(previous, current, immediately)
            resetAnimationRecord(record)
            record.setStartViewAndEndView()
            record.setAnimationStartOffset()
            // Android 9.0以下的WindowInsets可变（Reflect模块已兼容），
            // Android 9.0、Android 10的window包含FLAG_FULLSCREEN，
            // 可能导致insetsAnimation的回调不会执行。
            record.setPreDrawRunSimpleAnimation action@{
                // 运行insetsAnimation的一帧执行顺序：
                // 1. WindowInsetsAnimationCompat.Callback.onPrepare()
                // 2. AnimationDispatcher.onApplyWindowInsets()
                //    -> host.dispatchImeShown()
                //    -> AnimationDispatcher.onPendingChanged()
                //    -> record.setPreDrawRunSimpleAnimation()
                // 3. WindowInsetsAnimationCompat.Callback.onStart()
                // 4. PreDrawRunSimpleAnimation.invoke()
                // 若执行过程缺少第1、3步，则可能是没有运行insetsAnimation，
                // 第4步判断没有insetsAnimation，运行simpleAnimation进行兼容。
                if (record.insetsAnimation != null) return@action
                record.setAnimationEndOffset()
                runSimpleAnimationIfNecessary(record)
            }
        }

        override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
            host?.apply {
                lastInsets = insets
                val imeHeight = insets.imeHeight
                when {
                    lastImeHeight == 0 && imeHeight > 0 -> dispatchImeShown(shown = true)
                    lastImeHeight > 0 && imeHeight == 0 -> dispatchImeShown(shown = false)
                    lastImeHeight > 0 && imeHeight > 0 && lastImeHeight != imeHeight && current === ime -> {
                        // 调整IME高度后，运行simpleAnimation修正editorOffset
                        runSimpleAnimationFixEditorOffset(endOffset = insets.imeOffset)
                    }
                }
                lastImeHeight = imeHeight
                if (imeHeight > 0) stableImeHeight = imeHeight
            }
            return insets
        }

        private fun runSimpleAnimationFixEditorOffset(endOffset: Int) {
            val current = host?.current ?: return
            val record = AnimationRecord(current, current, immediately = false)
            resetAnimationRecord(record)
            record.setAnimationStartOffset()
            record.setAnimationEndOffset(endOffset)
            runSimpleAnimationIfNecessary(record)
        }

        override fun onStart(
            animation: WindowInsetsAnimationCompat,
            bounds: WindowInsetsAnimationCompat.BoundsCompat
        ): WindowInsetsAnimationCompat.BoundsCompat {
            val record = animationRecord?.takeIf { it.simpleAnimation == null }
            if (record == null || !animation.contains(ime())) return bounds
            record.apply {
                setAnimationEndOffset()
                setInsetsAnimation(animation)
                removePreDrawRunSimpleAnimation()
            }
            dispatchAnimationStart(record)
            if (record.isImmediately()) {
                dispatchAnimationEnd(record)
            }
            return bounds
        }

        override fun onProgress(
            insets: WindowInsetsCompat,
            runningAnimations: List<WindowInsetsAnimationCompat>
        ): WindowInsetsCompat {
            animationRecord?.takeIf { it.handleInsetsAnimation }
                ?.takeIf { runningAnimations.contains(it.insetsAnimation) }
                ?.let(::dispatchAnimationUpdate)
            return insets
        }

        override fun onEnd(animation: WindowInsetsAnimationCompat) {
            animationRecord?.takeIf { it.handleInsetsAnimation }
                ?.takeIf { it.insetsAnimation === animation }
                ?.let(::dispatchAnimationEnd)
        }
    }

    private inner class AnimationRecord(
        override val previous: Editor?,
        override val current: Editor?,
        private val immediately: Boolean
    ) : AnimationState {
        private var realStart = NO_VALUE
        private var realEnd = NO_VALUE
        private var preDrawListener: OneShotPreDrawListener? = null
        override var startView: View? = null; private set
        override var endView: View? = null; private set
        override var startOffset = NO_VALUE; private set
        override var endOffset = NO_VALUE; private set
        override var currentOffset = NO_VALUE; private set
        override val navBarOffset get() = host?.navBarOffset ?: 0
        override var animatedFraction = 0f; private set
        override var durationMillis = 0L; private set
        override var interpolator: Interpolator; private set

        var insetsAnimation: WindowInsetsAnimationCompat? = null; private set
        var simpleAnimation: ValueAnimator? = null; private set
        val handleInsetsAnimation: Boolean
            get() = simpleAnimation == null && insetsAnimation != null
        val isRunning: Boolean
            get() = simpleAnimation != null || insetsAnimation != null

        init {
            durationMillis = this@EditorAnimator.durationMillis
            interpolator = this@EditorAnimator.interpolator
        }

        fun isImmediately(): Boolean {
            return !canRunAnimation || immediately
                    || (previous === current && startOffset == endOffset)
        }

        override fun isIme(editor: Editor?): Boolean {
            return editor != null && host != null && host!!.ime === editor
        }

        override fun updateEditorOffset(offset: Int) {
            val host = host ?: return
            checkAnimationOffset()
            val min = min(startOffset, endOffset)
            val max = max(startOffset, endOffset)
            host.updateEditorOffset(offset.coerceAtLeast(min).coerceAtMost(max))
        }

        fun checkAnimationOffset(): Boolean {
            return startOffset != NO_VALUE
                    && endOffset != NO_VALUE
                    && currentOffset != NO_VALUE
        }

        fun setStartViewAndEndView() {
            startView = host?.previousView
            endView = host?.currentView
        }

        fun removeStartViewIfNecessary() {
            val host = host ?: return
            val view = startView
            if (view != null && view !== host.currentView) {
                host.removeEditorView(view)
            }
        }

        private fun setAnimationOffset(
            startOffset: Int = this.startOffset,
            endOffset: Int = this.endOffset,
            currentOffset: Int = this.currentOffset
        ) {
            this.startOffset = startOffset
            this.endOffset = endOffset
            val min = min(startOffset, endOffset)
            val max = max(startOffset, endOffset)
            this.currentOffset = currentOffset.coerceAtLeast(min).coerceAtMost(max)
        }

        fun setAnimationStartOffset() {
            setAnimationOffset(NO_VALUE, NO_VALUE, NO_VALUE)
            val startOffset = host?.editorOffset ?: return
            setAnimationOffset(startOffset, endOffset, startOffset)
            val stableImeHeight = animationDispatcher.stableImeHeight
            realStart = if (isIme(previous) && current == null) stableImeHeight else startOffset
        }

        fun setAnimationEndOffset(endOffset: Int = calculateEndOffset()) {
            setAnimationOffset(endOffset = endOffset)
            val stableImeHeight = animationDispatcher.stableImeHeight
            realEnd = if (previous == null && isIme(current)) stableImeHeight else endOffset
        }

        fun updateAnimationCurrent() {
            if (animatedFraction < 1f) {
                animatedFraction = simpleAnimation?.animatedFraction
                        ?: insetsAnimation?.fraction ?: 0f
            }
            val realCurrent = realStart + (realEnd - realStart) * interpolatedFraction
            val realTotal = (realEnd - realStart).absoluteValue
            val total = (endOffset - startOffset).absoluteValue
            val diff = (realTotal - total).absoluteValue
            setAnimationOffset(currentOffset = (realCurrent - diff).toInt())
        }

        fun updateAnimationToEnd() {
            animatedFraction = 1f
            updateAnimationCurrent()
        }

        fun setInsetsAnimation(animation: WindowInsetsAnimationCompat) {
            insetsAnimation = animation
            durationMillis = animation.durationMillis
            animation.interpolator?.let { interpolator = it }
        }

        fun setSimpleAnimation(animation: ValueAnimator) {
            simpleAnimation = animation
            durationMillis = animation.duration
            interpolator = this@EditorAnimator.interpolator
        }

        fun setPreDrawRunSimpleAnimation(action: () -> Unit) {
            removePreDrawRunSimpleAnimation()
            preDrawListener = host?.addPreDrawAction(action)
        }

        fun removePreDrawRunSimpleAnimation() {
            preDrawListener?.removeListener()
            preDrawListener = null
        }

        fun endAnimation() {
            if (simpleAnimation != null) {
                // simpleAnimation.end()会置空insetsAnimation
                simpleAnimation!!.end()
            } else {
                // 该分支处理两种情况
                // 1. simpleAnimation或insetsAnimation为null，需要重置准备工作.
                // 2. 存在逻辑缺陷，insetsAnimation不可结束，需要更新为结束值.
                dispatchAnimationEnd(this)
            }
        }
    }

    companion object {
        private const val ENABLE_INSETS_ANIMATION_BELOW_R = true
        internal const val ANIMATION_DURATION_MILLIS = 200L
        internal val ANIMATION_INTERPOLATOR = DecelerateInterpolator()
    }
}

/**
 * [EditorAnimator]的动画状态
 */
interface AnimationState {

    /**
     * 动画起始[Editor]
     */
    val previous: Editor?

    /**
     * 动画结束[Editor]
     */
    val current: Editor?

    /**
     * 动画起始[Editor]的视图
     */
    val startView: View?

    /**
     * 动画结束[Editor]的视图
     */
    val endView: View?

    /**
     * 动画起始偏移值
     */
    @get:IntRange(from = 0)
    val startOffset: Int

    /**
     * 动画结束偏移值
     */
    @get:IntRange(from = 0)
    val endOffset: Int

    /**
     * 基于[interpolator]计算的动画当前偏移值
     */
    @get:IntRange(from = 0)
    val currentOffset: Int

    /**
     * 导航栏偏移，若不支持手势导航栏EdgeToEdge，则该属性值为0
     */
    @get:IntRange(from = 0)
    val navBarOffset: Int

    /**
     * 动画时长
     */
    @get:IntRange(from = 0)
    val durationMillis: Long

    /**
     * 计算[currentOffset]的插值器
     */
    val interpolator: Interpolator

    /**
     * 动画起始状态和结束状态之间的原始分数进度
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    val animatedFraction: Float

    /**
     * [startOffset]和[endOffset]之间的分数进度
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    val interpolatedFraction: Float
        get() = interpolator.getInterpolation(animatedFraction)

    /**
     * [editor]是否为IME
     */
    fun isIme(editor: Editor?): Boolean

    /**
     * 更新编辑区的偏移值
     *
     * @param offset 取值范围为[startOffset]到[endOffset]，
     * [EditorAnimator]的默认实现按[currentOffset]进行更新。
     */
    fun updateEditorOffset(offset: Int)
}

/**
 * [EditorAnimator]的动画回调
 */
interface AnimationCallback {

    /**
     * 动画准备
     *
     * 该函数在布局之前被调用，此时可以修改[InputView.editorMode]以及更新`contentView`。
     *
     * @param previous 动画起始[Editor]
     * @param current  动画结束[Editor]
     */
    fun onAnimationPrepare(previous: Editor?, current: Editor?) = Unit

    /**
     * 动画开始
     *
     * 该函数在`preDraw`阶段调用，此时可以获取View的尺寸以及对View做变换、边界处理。
     */
    fun onAnimationStart(state: AnimationState) = Unit

    /**
     * 动画更新
     *
     * 后执行的[AnimationCallback]，可以在该函数下覆盖`state.startView`和`state.endView`的变换属性。
     */
    fun onAnimationUpdate(state: AnimationState) = Unit

    /**
     * 动画结束
     *
     * **注意**：动画结束时，应当将`state.startView`和`state.endView`恢复为初始状态。
     */
    fun onAnimationEnd(state: AnimationState) = Unit
}

/**
 * [EditorAnimator]的动画拦截器
 */
interface AnimationInterceptor {

    /**
     * 返回`true`拦截[Editor]的更改，[EditorAdapter.notifyShow]不生效
     */
    fun onInterceptChange(current: Editor?, next: Editor?): Boolean

    /**
     * 两个[AnimationInterceptor]合并为一个，用于多个[AnimationInterceptor]一起拦截的场景
     */
    operator fun plus(other: AnimationInterceptor): AnimationInterceptor = when (other) {
        is EmptyAnimationInterceptor -> this
        else -> CombinedAnimationInterceptor(this, other)
    }
}

/**
 * 空实现的[AnimationInterceptor]，可用于属性初始化场景
 */
fun emptyAnimationInterceptor(): AnimationInterceptor = EmptyAnimationInterceptor

private object EmptyAnimationInterceptor : AnimationInterceptor {

    override fun onInterceptChange(current: Editor?, next: Editor?) = false

    override fun plus(other: AnimationInterceptor) = other
}

private class CombinedAnimationInterceptor(
    private val first: AnimationInterceptor,
    private val second: AnimationInterceptor
) : AnimationInterceptor {

    override fun onInterceptChange(current: Editor?, next: Editor?): Boolean {
        return first.onInterceptChange(current, next) || second.onInterceptChange(current, next)
    }
}