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

import android.animation.ValueAnimator
import android.os.Build
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.core.animation.addListener
import androidx.core.view.OneShotPreDrawListener
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.ime
import com.xiaocydx.inputview.compat.contains

/**
 * [InputView]编辑区的[Editor]过渡动画
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
    private val durationMillis: Long = ANIMATION_DURATION_MILLIS,

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
    private val interpolator: Interpolator = ANIMATION_INTERPOLATOR
) {
    private var host: EditorHost? = null
    private var insets: WindowInsetsCompat? = null
    private var animationRecord: AnimationRecord? = null
    private val animationDispatcher = AnimationDispatcher()
    private val callbacks = ArrayList<AnimationCallback>(2)
    internal open val canRunAnimation: Boolean = true
    internal val isActive: Boolean
        get() = animationRecord != null

    /**
     * 动画是否运行中
     */
    val isRunning: Boolean
        get() = animationRecord?.isRunning == true

    /**
     * 添加[AnimationCallback]
     *
     * 在[AnimationCallback]的各个函数可以调用[removeAnimationCallback]。
     */
    fun addAnimationCallback(callback: AnimationCallback) {
        if (!callbacks.contains(callback)) callbacks.add(callback)
    }

    /**
     * 移除[AnimationCallback]
     */
    fun removeAnimationCallback(callback: AnimationCallback) {
        callbacks.remove(callback)
    }

    /**
     * 更新编辑区的偏移值
     *
     * **注意**：该函数只能在[AnimationCallback]的函数中调用，并确保[currentOffset]
     * 在[AnimationState.startOffset]到[AnimationState.endOffset]的范围内。
     */
    protected fun updateEditorOffset(@IntRange(from = 0) currentOffset: Int) {
        host?.updateEditorOffset(currentOffset)
    }

    internal fun forEachCallback(action: (AnimationCallback) -> Unit) {
        callbacks.forEach(action)
    }

    private fun resetAnimationRecord(record: AnimationRecord) {
        endAnimation()
        assert(animationRecord == null) { "animationRecord未被置空" }
        animationRecord = record
    }

    private fun runSimpleAnimationIfNecessary(record: AnimationRecord) {
        if (!canRunAnimation || record.startOffset == record.endOffset) {
            dispatchAnimationStart(record)
            dispatchAnimationEnd(record)
            return
        }

        ValueAnimator.ofFloat(0f, 1f).apply {
            addListener(
                onStart = { dispatchAnimationStart(record) },
                onCancel = { dispatchAnimationEnd(record) },
                onEnd = { dispatchAnimationEnd(record) }
            )
            addUpdateListener { dispatchAnimationUpdate(record) }
            duration = durationMillis
            interpolator = LinearInterpolator()
            record.setSimpleAnimation(this)
            start()
        }
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
            if (host != null && host!!.editorOffset != record.endOffset) {
                dispatchAnimationUpdate(record)
            }
            dispatchAnimationCallback { onAnimationEnd(record) }
        }
        record.removeStartViewIfNecessary()
        record.removePreDrawRunSimpleAnimation()
        animationRecord = null
    }

    private inline fun dispatchAnimationCallback(action: AnimationCallback.() -> Unit) {
        for (index in callbacks.indices.reversed()) callbacks[index].apply(action)
    }

    internal fun endAnimation() {
        animationRecord?.endAnimation()
    }

    internal fun onAttachToEditorHost(host: EditorHost) {
        this.host = host
        host.addEditorChangedListener(animationDispatcher)
        host.setOnApplyWindowInsetsListener(animationDispatcher)
        host.takeIf { enableWindowInsetsAnimation() }
            ?.setWindowInsetsAnimationCallback(durationMillis, interpolator, animationDispatcher)
    }

    internal fun onDetachFromEditorHost(host: EditorHost) {
        endAnimation()
        host.removeEditorChangedListener(animationDispatcher)
        host.setOnApplyWindowInsetsListener(null)
        host.takeIf { enableWindowInsetsAnimation() }
            ?.setWindowInsetsAnimationCallback(durationMillis, interpolator, callback = null)
        this.host = null
    }

    private fun enableWindowInsetsAnimation(): Boolean {
        return canRunAnimation && (Build.VERSION.SDK_INT >= 30 || ENABLE_INSETS_ANIMATION_BELOW_R)
    }

    private inner class AnimationDispatcher :
            ReplicableEditorChangedListener, OnApplyWindowInsetsListenerCompat,
            WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
        private var imeHeight = 0

        /**
         * ### StartView和EndView
         * 当[canRunAnimation]为`true`时，[EditorHost]更改[Editor]会保留之前[Editor]的子View，
         * 参与动画过程，动画结束时调用[AnimationRecord.removeStartViewIfNecessary]移除子View，
         * 之前[Editor]的子View作为StartView，当前[Editor]的子View作为EndView。
         *
         * ### SimpleAnimation和InsetsAnimation
         * 若[previous]和[current]不是IME，则调用[runSimpleAnimationIfNecessary]运行SimpleAnimation，
         * 若[previous]和[current]其中一个是IME，则通过[AnimationDispatcher]运行InsetsAnimation。
         *
         * ### 主动更改和被动更改
         * 直接调用[EditorHost.showChecked]或[EditorHost.hideChecked]更改[Editor]属于主动更改，
         * 点击EditText显示IME、按返回键隐藏IME等操作，会在[onApplyWindowInsets]判断IME的显示情况，
         * 然后调用[EditorHost.dispatchImeShown]更改[Editor]，这属于被动更改。
         */
        override fun onEditorChanged(previous: Editor?, current: Editor?) {
            val record = AnimationRecord(previous, current)
            resetAnimationRecord(record)
            record.setStartViewAndEndView()
            // Android 9.0以下的WindowInsets可变（InputView已兼容），
            // Android 9.0、Android 10的window包含FLAG_FULLSCREEN，
            // 可能导致insetsAnimation的回调不会执行，因此做以下处理：
            // 1. 预测是否将要运行insetsAnimation。
            // 2. 若不会运行insetsAnimation，则立即添加PreDrawRunSimpleAnimation。
            // 3. 若将要运行insetsAnimation，则推迟添加PreDrawRunSimpleAnimation，
            // 在onApplyWindowInsets()判断为显示或隐藏IME时，才添加PreDrawRunSimpleAnimation，
            // 这样做的目的是确保PreDrawRunSimpleAnimation在insetsAnimation的onStart()之后执行，
            // 当执行PreDrawRunSimpleAnimation时，若没有insetsAnimation，则运行simpleAnimation。
            record.setPreDrawRunSimpleAnimation action@{
                if (record.insetsAnimation != null) return@action
                record.setAnimationOffsetForCurrent()
                runSimpleAnimationIfNecessary(record)
            }
            if (!record.willRunInsetsAnimation) {
                record.addPreDrawRunSimpleAnimation()
            } else {
                // 将要运行insetsAnimation的一帧执行顺序：
                // 1. WindowInsetsAnimationCompat.Callback.onPrepare()
                // 2. AnimationDispatcher.onApplyWindowInsets()
                //    -> AnimationRecord.addPreDrawRunSimpleAnimation()
                // 3. WindowInsetsAnimationCompat.Callback.onStart()
                // 4. PreDrawRunSimpleAnimation.invoke()
                // 若执行过程缺少第1、3步，则可能是没有运行insetsAnimation，
                // 第4步判断没有insetsAnimation，运行simpleAnimation进行兼容。
            }
        }

        /**
         * 该函数被调用之前，可能已更改[Editor]，执行了[onEditorChanged]创建[animationRecord]
         */
        override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
            host?.window?.apply {
                this@EditorAnimator.insets = insets
                val lastImeHeight = insets.imeHeight
                when {
                    imeHeight == 0 && lastImeHeight > 0 -> {
                        host?.dispatchImeShown(shown = true)
                        animationRecord?.addPreDrawRunSimpleAnimation()
                    }
                    imeHeight > 0 && lastImeHeight == 0 -> {
                        host?.dispatchImeShown(shown = false)
                        animationRecord?.addPreDrawRunSimpleAnimation()
                    }
                    imeHeight > 0 && lastImeHeight > 0 && imeHeight != lastImeHeight -> {
                        // 调整IME高度后，运行simpleAnimation修正editorOffset
                        runSimpleAnimationFixEditorOffset(endOffset = insets.imeOffset)
                    }
                }
                imeHeight = lastImeHeight
            }
            return insets
        }

        private fun runSimpleAnimationFixEditorOffset(endOffset: Int) {
            val current = host?.current ?: return
            val record = AnimationRecord(current, current)
            resetAnimationRecord(record)
            record.setAnimationOffsetForCurrent()
            record.setAnimationOffset(endOffset = endOffset)
            runSimpleAnimationIfNecessary(record)
        }

        override fun onStart(
            animation: WindowInsetsAnimationCompat,
            bounds: WindowInsetsAnimationCompat.BoundsCompat
        ): WindowInsetsAnimationCompat.BoundsCompat {
            val record = animationRecord?.takeIf { it.simpleAnimation == null }
            if (record == null || !animation.contains(ime())) return bounds
            record.apply {
                setInsetsAnimation(animation)
                setAnimationOffsetForCurrent()
                removePreDrawRunSimpleAnimation()
            }
            dispatchAnimationStart(record)
            if (record.startOffset == record.endOffset) {
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
        override val current: Editor?
    ) : AnimationState {
        private var preDrawAction: (() -> Unit)? = null
        private var preDrawListener: OneShotPreDrawListener? = null
        override var startView: View? = null; private set
        override var endView: View? = null; private set
        override var startOffset: Int = NO_VALUE; private set
        override var endOffset: Int = NO_VALUE; private set
        override var currentOffset: Int = NO_VALUE; private set
        override val navBarOffset: Int get() = host?.navBarOffset ?: 0
        override var animatedFraction: Float = 0f; private set
        override var durationMillis: Long = 0; private set
        override lateinit var interpolator: Interpolator; private set

        val willRunInsetsAnimation = isIme(previous) || isIme(current)
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

        override fun isIme(editor: Editor?): Boolean {
            return editor != null && host != null && host!!.ime === editor
        }

        fun checkAnimationOffset(): Boolean {
            return startOffset != NO_VALUE && endOffset != NO_VALUE && currentOffset != NO_VALUE
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

        fun setAnimationOffset(
            startOffset: Int = this.startOffset,
            endOffset: Int = this.endOffset,
            currentOffset: Int = this.currentOffset
        ) {
            this.startOffset = startOffset
            this.endOffset = endOffset
            val min = startOffset.coerceAtMost(endOffset)
            val max = startOffset.coerceAtLeast(endOffset)
            this.currentOffset = currentOffset.coerceAtLeast(min).coerceAtMost(max)
        }

        fun setAnimationOffsetForCurrent() {
            setAnimationOffset(NO_VALUE, NO_VALUE, NO_VALUE)
            host?.window?.apply {
                val host = host!!
                val startOffset = host.editorOffset
                val endOffset = if (isIme(current)) {
                    insets?.imeOffset ?: NO_VALUE
                } else {
                    host.currentView?.height ?: 0
                }
                setAnimationOffset(startOffset, endOffset, startOffset)
            }
        }

        fun updateAnimationCurrent() {
            if (animatedFraction < 1f) {
                animatedFraction = simpleAnimation?.animatedFraction
                        ?: insetsAnimation?.fraction ?: 0f
            }
            val currentOffset = startOffset + (endOffset - startOffset) * interpolatedFraction
            setAnimationOffset(currentOffset = currentOffset.toInt())
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
            preDrawAction = action
        }

        fun addPreDrawRunSimpleAnimation() {
            val action = preDrawAction ?: return
            preDrawAction = null
            preDrawListener = host?.addPreDrawAction(action)
        }

        fun removePreDrawRunSimpleAnimation() {
            preDrawListener?.removeListener()
            preDrawListener = null
            preDrawAction = null
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
        private const val NO_VALUE = -1
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
     * 导航栏偏移，若不支持手势导航栏边到边，则该属性值为0
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
}

/**
 * [EditorAnimator]的动画回调
 */
interface AnimationCallback {

    /**
     * 动画开始
     *
     * @param state 编辑区的动画状态
     */
    fun onAnimationStart(state: AnimationState) = Unit

    /**
     * 动画更新
     *
     * @param state 编辑区的动画状态
     */
    fun onAnimationUpdate(state: AnimationState) = Unit

    /**
     * 动画结束
     *
     * **注意**：动画结束时，应当将`state.startView`和`state.endView`恢复为初始状态。
     *
     * @param state 编辑区的动画状态
     */
    fun onAnimationEnd(state: AnimationState) = Unit
}