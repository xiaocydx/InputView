package com.xiaocydx.inputview

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.EditText
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.core.animation.addListener
import androidx.core.view.*
import kotlin.math.absoluteValue

/**
 * [InputView]编辑区的[Editor]过渡动画
 *
 * @author xcc
 * @date 2023/1/8
 */
abstract class EditorAnimator {
    private var inputView: InputView? = null
    private var editorView: EditorView? = null
    private var animationRecord: AnimationRecord? = null
    private val animationDispatcher = AnimationDispatcher()
    private val callbacks = ArrayList<AnimationCallback>(2)
    internal open val canRunAnimation: Boolean = true

    /**
     * 动画是否运行中
     */
    val isRunning: Boolean
        get() = animationRecord != null

    /**
     * 添加[AnimationCallback]
     *
     * 在[EditorChangedListener]的各个函数可以调用[removeAnimationCallback]。
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
     * 动画开始
     *
     * @param state [InputView]编辑区的动画状态
     */
    protected open fun onAnimationStart(state: AnimationState) = Unit

    /**
     * 动画更新
     *
     * @param state [InputView]编辑区的动画状态
     */
    protected open fun onAnimationUpdate(state: AnimationState) = Unit

    /**
     * 动画结束
     *
     * **注意**：动画结束时，应当将`state.startView`和`state.endView`恢复为初始状态。
     *
     * @param state [InputView]编辑区的动画状态
     */
    protected open fun onAnimationEnd(state: AnimationState) = Unit

    /**
     * 获取动画时长，单位：ms
     *
     * **注意**：内部实现配合Insets动画显示或隐藏IME时，不会调用该函数。
     *
     * @param state [InputView]编辑区的动画状态
     */
    protected open fun getAnimationDuration(state: AnimationState): Long = ANIMATION_DURATION

    /**
     * 获取动画时长，单位：ms
     *
     * **注意**：内部实现配合Insets动画显示或隐藏IME时，不会调用该函数。
     *
     * @param state [InputView]编辑区的动画状态
     */
    protected open fun getAnimationInterpolator(state: AnimationState): Interpolator = ANIMATION_INTERPOLATOR

    /**
     * 当前[EditorAnimator]添加到[adapter]
     */
    protected open fun onAttachToEditorAdapter(adapter: EditorAdapter<*>) = Unit

    /**
     * 当前[EditorAnimator]从[adapter]移除
     */
    protected open fun onDetachFromEditorAdapter(adapter: EditorAdapter<*>) = Unit

    /**
     * 重置[afterDispatchTouchEvent]的处理
     */
    internal fun beforeDispatchTouchEvent(ev: MotionEvent) {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            // 按下时，将EditText重置为获得焦点显示IME
            inputView?.editTextHolder?.showSoftInputOnFocus = true
        }
    }

    /**
     * 点击[EditText]显示IME，需要隐藏`textSelectHandle`，避免动画运行时不断跨进程通信，从而造成卡顿。
     * 由于`textSelectHandle`是Android 10的属性，因此先清除焦点再获得焦点，实现隐藏`textSelectHandle`。
     *
     * **注意**：`textSelectHandle`指的是[EditText.getTextSelectHandle]，即[EditText]的水滴状指示器。
     */
    internal fun afterDispatchTouchEvent(ev: MotionEvent) {
        val editText = inputView?.editTextHolder ?: return
        val ime = editorView?.ime ?: return
        val current = editorView?.current
        if (ev.action == MotionEvent.ACTION_UP && current !== ime
                && editText.showSoftInputOnFocus && editText.isTouched(ev)) {
            // 点击EditText显示IME，抬起时隐藏textSelectHandle
            editText.hideTextSelectHandle()
        }
        if (ev.action != MotionEvent.ACTION_DOWN) {
            // 选中EditText的内容时，不显示IME
            editText.showSoftInputOnFocus = !editText.hasTextSelectHandleLeftToRight
        }
    }

    private fun resetAnimationRecord(record: AnimationRecord) {
        endAnimation()
        assert(animationRecord == null) { "animationRecord未被置空" }
        animationRecord = record
    }

    private inline fun runSimpleAnimationIfNecessary(
        record: AnimationRecord,
        block: ValueAnimator.() -> Boolean = { false }
    ) {
        if (!canRunAnimation || record.startOffset == record.endOffset) {
            dispatchAnimationStart(record)
            dispatchAnimationEnd(record)
            return
        }

        ValueAnimator.ofInt(record.startOffset, record.endOffset).apply {
            addListener(
                onStart = { dispatchAnimationStart(record) },
                onCancel = { dispatchAnimationEnd(record) },
                onEnd = { dispatchAnimationEnd(record) }
            )
            addUpdateListener { dispatchAnimationUpdate(record, it.animatedValue as Int) }
            if (block(this)) return@apply start()

            duration = getAnimationDuration(record)
            interpolator = getAnimationInterpolator(record)
            start()
        }.also(record::setSimpleAnimation)
    }

    private fun dispatchAnimationStart(record: AnimationRecord) {
        val editText = inputView?.editTextHolder
        if (editText?.hasTextSelectHandleLeftToRight == true) {
            editText.hideTextSelectHandle(keepFocus = record.isIme(record.current))
        }
        if (!record.checkAnimationOffset()) return
        onAnimationStart(record)
        dispatchAnimationCallback { onAnimationStart(record) }
    }

    private fun dispatchAnimationUpdate(record: AnimationRecord, currentOffset: Int) {
        if (!record.checkAnimationOffset()) return
        record.setAnimationOffset(currentOffset = currentOffset)
        inputView?.updateEditorOffset(record.currentOffset)
        onAnimationUpdate(record)
        dispatchAnimationCallback { onAnimationUpdate(record) }
    }

    private fun dispatchAnimationEnd(record: AnimationRecord) {
        if (record.checkAnimationOffset()) {
            record.setAnimationOffset(currentOffset = record.endOffset)
            if (inputView != null && inputView!!.editorOffset != record.endOffset) {
                dispatchAnimationUpdate(record, record.endOffset)
            }
            onAnimationEnd(record)
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

    internal fun attach(adapter: EditorAdapter<*>) {
        inputView = adapter.inputView
        editorView = adapter.editorView
        animationDispatcher.attach(editorView!!, adapter)
        onAttachToEditorAdapter(adapter)
    }

    internal fun detach(adapter: EditorAdapter<*>) {
        assert(inputView === adapter.inputView) { "InputView不相同" }
        assert(editorView === adapter.editorView) { "EditorView不相同" }
        endAnimation()
        animationDispatcher.detach(editorView!!, adapter)
        inputView = null
        editorView = null
        onDetachFromEditorAdapter(adapter)
    }

    private inner class AnimationDispatcher :
            EditorChangedListener<Editor>, OnApplyWindowInsetsListener,
            WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
        private var imeHeight = 0

        fun attach(view: EditorView, adapter: EditorAdapter<*>) {
            adapter.addEditorChangedListener(this)
            ViewCompat.setOnApplyWindowInsetsListener(view, this)
            if (canRunAnimation) ViewCompat.setWindowInsetsAnimationCallback(view, this)
        }

        fun detach(view: EditorView, adapter: EditorAdapter<*>) {
            adapter.removeEditorChangedListener(this)
            ViewCompat.setOnApplyWindowInsetsListener(view, null)
            if (canRunAnimation) ViewCompat.setWindowInsetsAnimationCallback(view, null)
        }

        /**
         * ### StartView和EndView
         * [EditorView]更改[Editor]时会先移除全部子View，再添加当前[Editor]的子View，
         * 运行动画之前调用[AnimationRecord.addStartViewIfNecessary]将移除的子View添加回来，
         * 参与动画更新过程，动画结束时调用[AnimationRecord.removeStartViewIfNecessary]移除子View，
         * 添加回来的子View作为StartView，当前[Editor]的子View作为EndView。
         *
         * ### SimpleAnimation和InsetsAnimation
         * 若[previous]和[current]不是IME，则调用[runSimpleAnimationIfNecessary]运行SimpleAnimation，
         * 若[previous]和[current]其中一个是IME，则通过[AnimationDispatcher]运行InsetsAnimation。
         *
         * ### 主动更改和被动更改
         * 直接调用[EditorView.showChecked]或[EditorView.hideChecked]更改[Editor]属于主动更改，
         * 点击EditText显示IME、按返回键隐藏IME等操作，会在[onApplyWindowInsets]判断IME的显示情况，
         * 然后调用[EditorView.dispatchImeShown]更改[Editor]，这属于被动更改。
         */
        override fun onEditorChanged(previous: Editor?, current: Editor?) {
            val record = AnimationRecord(previous, current)
            resetAnimationRecord(record)
            record.setStartViewAndEndView()
            // 由于insetsAnimation的回调可能不会执行（Android 8.0首次显示IME可复现），因此做以下处理：
            // 1. 预测是否将要运行insetsAnimation。
            // 2. 若不会运行insetsAnimation，则立即添加PreDrawRunSimpleAnimation。
            // 3. 若将要运行insetsAnimation，则不立即添加PreDrawRunSimpleAnimation，
            // 而是在onApplyWindowInsets()显示或隐藏IME时，才添加PreDrawRunSimpleAnimation，
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
                // 若执行过程缺少第1、3步，则可能是insetsAnimation的内部逻辑出了问题，
                // 第4步判断没有insetsAnimation，运行的simpleAnimation能修复这个问题。
            }
        }

        /**
         * 该函数被调用之前，可能已更改[Editor]，执行了[onEditorChanged]创建[animationRecord]
         */
        override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
            inputView?.window?.apply {
                val lastImeHeight = insets.imeHeight
                when {
                    imeHeight == 0 && lastImeHeight > 0 -> {
                        editorView?.dispatchImeShown(shown = true)
                        animationRecord?.addPreDrawRunSimpleAnimation()
                    }
                    imeHeight > 0 && lastImeHeight == 0 -> {
                        editorView?.dispatchImeShown(shown = false)
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
            val current = editorView?.current ?: return
            resetAnimationRecord(record = AnimationRecord(current, current)
                .apply { setAnimationOffsetForCurrent() }
                .apply { setAnimationOffset(endOffset = endOffset) }
                .also(::runSimpleAnimationIfNecessary)
            )
        }

        override fun onStart(
            animation: WindowInsetsAnimationCompat,
            bounds: WindowInsetsAnimationCompat.BoundsCompat
        ): WindowInsetsAnimationCompat.BoundsCompat = inputView?.window?.run {
            val record = animationRecord
            if (record == null || !animation.containsImeType()) return bounds
            record.apply {
                setInsetsAnimation(animation)
                setAnimationOffsetForCurrent()
                removePreDrawRunSimpleAnimation()
            }
            when {
                (record.startOffset == record.endOffset)
                        || (record.imeToOther() || record.otherToIme()) -> {
                    // imeToOther的animation是0到imeEndOffset，
                    // otherToIme的animation是imeStartOffset到0，
                    // 这两种情况依靠animation进行更新，实现的动画效果并不理想，
                    // 因此用animation的duration和interpolator运行动画进行更新。
                    runSimpleAnimationIfNecessary(record) {
                        duration = animation.durationMillis
                        interpolator = animation.interpolator ?: ANIMATION_INTERPOLATOR
                        true
                    }
                }
                else -> dispatchAnimationStart(record)
            }
            bounds
        } ?: bounds

        override fun onProgress(
            insets: WindowInsetsCompat,
            runningAnimations: List<WindowInsetsAnimationCompat>
        ): WindowInsetsCompat = inputView?.window?.run {
            animationRecord?.takeIf { it.handleInsetsAnimation }
                ?.takeIf { runningAnimations.contains(it.insetsAnimation) }
                ?.let { dispatchAnimationUpdate(it, currentOffset = insets.imeOffset) }
            insets
        } ?: insets

        override fun onEnd(animation: WindowInsetsAnimationCompat) {
            animationRecord?.takeIf { it.handleInsetsAnimation }
                ?.takeIf { it.insetsAnimation === animation }
                ?.let(::dispatchAnimationEnd)
        }
    }

    private inner class AnimationRecord(
        override val previous: Editor? = null,
        override val current: Editor? = null
    ) : AnimationState {
        private var preDrawAction: (() -> Unit)? = null
        private var preDrawListener: OneShotPreDrawListener? = null
        override var startView: View? = null; private set
        override var endView: View? = null; private set
        override var startOffset: Int = NO_VALUE; private set
        override var endOffset: Int = NO_VALUE; private set
        override var currentOffset: Int = NO_VALUE; private set
        override val navBarOffset: Int
            get() = inputView?.navBarOffset ?: 0

        val willRunInsetsAnimation = isIme(previous) || isIme(current)
        var insetsAnimation: WindowInsetsAnimationCompat? = null; private set
        var simpleAnimation: Animator? = null; private set
        val handleInsetsAnimation: Boolean
            get() = simpleAnimation == null && insetsAnimation != null

        fun imeToOther() = isIme(previous) && current != null && !isIme(current)

        fun otherToIme() = previous != null && !isIme(previous) && isIme(current)

        fun isIme(editor: Editor?): Boolean {
            return editor != null && editorView != null && editorView!!.ime === editor
        }

        fun checkAnimationOffset(): Boolean {
            return startOffset != NO_VALUE && endOffset != NO_VALUE && currentOffset != NO_VALUE
        }

        fun setStartViewAndEndView() {
            addStartViewIfNecessary()
            endView = editorView?.changeRecord?.currentChild
        }

        private fun addStartViewIfNecessary() {
            val editorView = editorView
            if (!canRunAnimation || editorView == null) return
            val record = editorView.changeRecord
            if (startView !== record.previousChild
                    && startView !== record.currentChild) {
                removeStartViewIfNecessary()
            }
            val child = editorView.changeRecord.previousChild
            if (child != null && child.parent == null) {
                startView = child
                editorView.addView(child)
            }
        }

        fun removeStartViewIfNecessary() {
            val editorView = editorView ?: return
            val child = startView
            if (child != null && child.parent === editorView
                    && child !== editorView.changeRecord.currentChild) {
                editorView.removeView(child)
            }
            startView = null
        }

        fun setAnimationOffset(
            startOffset: Int = this.startOffset,
            endOffset: Int = this.endOffset,
            currentOffset: Int = this.currentOffset
        ) {
            this.startOffset = startOffset
            this.endOffset = endOffset
            // 切换不同的IME时，insetsAnimation计算的currentOffset可能不在min..max
            val min = startOffset.coerceAtMost(endOffset)
            val max = startOffset.coerceAtLeast(endOffset)
            this.currentOffset = currentOffset.coerceAtLeast(min).coerceAtMost(max)
        }

        fun setAnimationOffsetForCurrent() {
            setAnimationOffset(NO_VALUE, NO_VALUE, NO_VALUE)
            inputView?.window?.run {
                val startOffset = inputView?.editorOffset ?: NO_VALUE
                val endOffset = when {
                    editorView == null -> NO_VALUE
                    isIme(current) -> getRootWindowInsets()?.imeOffset ?: NO_VALUE
                    else -> editorView!!.changeRecord.currentChild?.height ?: 0
                }
                setAnimationOffset(startOffset, endOffset, startOffset)
            }
        }

        fun setInsetsAnimation(animation: WindowInsetsAnimationCompat) {
            insetsAnimation = animation
        }

        fun setSimpleAnimation(animation: Animator) {
            simpleAnimation = animation
        }

        fun setPreDrawRunSimpleAnimation(action: () -> Unit) {
            preDrawAction = action
        }

        fun addPreDrawRunSimpleAnimation() {
            val view = editorView ?: return
            val action = preDrawAction ?: return
            preDrawAction = null
            preDrawListener = OneShotPreDrawListener.add(view, action)
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
        private const val ANIMATION_DURATION = 250L
        private val ANIMATION_INTERPOLATOR = DecelerateInterpolator()
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
     * 动画当前偏移值
     */
    @get:IntRange(from = 0)
    val currentOffset: Int

    /**
     * 导航栏偏移，若不支持手势导航栏边到边，则该属性值为0
     */
    @get:IntRange(from = 0)
    val navBarOffset: Int

    /**
     * 动画起始状态和结束状态之间的分数进度
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    val fraction: Float
        get() {
            val diff = (currentOffset - startOffset).absoluteValue
            return diff.toFloat() / (endOffset - startOffset).absoluteValue
        }
}

/**
 * [EditorAnimator]的动画回调
 */
interface AnimationCallback {

    /**
     * 动画开始
     *
     * @param state [InputView]编辑区的动画状态
     */
    fun onAnimationStart(state: AnimationState) = Unit

    /**
     * 动画更新
     *
     * @param state [InputView]编辑区的动画状态
     */
    fun onAnimationUpdate(state: AnimationState) = Unit

    /**
     * 动画结束
     *
     * **注意**：动画结束时，应当将`state.startView`和`state.endView`恢复为初始状态。
     *
     * @param state [InputView]编辑区的动画状态
     */
    fun onAnimationEnd(state: AnimationState) = Unit
}