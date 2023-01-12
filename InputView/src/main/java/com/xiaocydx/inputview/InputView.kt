package com.xiaocydx.inputview

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.view.View.MeasureSpec.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import androidx.annotation.IntRange
import androidx.core.view.*

/**
 * 输入控件
 *
 * 1. 在[Activity.onCreate]调用`InputView.init(window)`。
 * 2. [InputView]初始化时只能有一个子View，该子View作为[contentView]。
 * 3. [editText]用于兼容Android各版本显示和隐藏软键盘。
 * 4. [editorAdapter]支持多种[Editor]的视图创建和显示。
 * 5. [editorAnimator]支持[Editor]之间的过渡动画。
 *
 * [contentView]的初始化布局位置等同于[Gravity.CENTER]，其测量高度不受`layoutParams.height`影响，
 * 最大值是[InputView]的测量高度，[Editor]的视图位于[contentView]下方，通知显示[Editor]的视图时，
 * 会平移[contentView]，或者修改[contentView]的尺寸，具体是哪一种行为取决于[EditorMode]。
 *
 * @author xcc
 * @date 2023/1/6
 */
class InputView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {
    private val editorView = EditorView(context)
    private var contentView: View? = null
    private var window: ViewTreeWindow? = null

    /**
     * 编辑区的偏移值，可作为[EditorAnimator]动画的起始值
     */
    @get:IntRange(from = 0)
    internal var editorOffset = 0
        private set

    /**
     * 导航栏的偏移值，可作为[EditorAnimator]动画的计算参数
     */
    @get:IntRange(from = 0)
    internal var navBarOffset = 0
        private set(value) {
            if (field != value) requestLayout()
            field = value
        }

    /**
     * 用于兼容Android各版本显示和隐藏IME的[EditText]
     *
     * 显示IME[editText]会获得焦点，隐藏IME会清除[editText]的焦点，
     * 可以观察[EditorAdapter.addEditorChangedListener]的更改结果，
     * 处理[editText]的焦点。
     */
    var editText: EditText?
        get() = editorView.editText
        set(value) {
            editorView.editText = value
        }

    /**
     * [EditorMode]默认为[EditorMode.ADJUST_RESIZE]
     *
     * 1. [EditorMode.ADJUST_PAN]，显示[Editor]时平移[contentView]。
     * 2. [EditorMode.ADJUST_RESIZE]，显示[Editor]时修改[contentView]的尺寸。
     */
    var editorMode: EditorMode = EditorMode.ADJUST_RESIZE
        set(value) {
            if (field === value) return
            field = value
            requestLayout()
        }

    /**
     * [editorAdapter]默认为[ImeAdapter]
     *
     * [ImeAdapter]用于只需要IME的场景，若需要显示多种[Editor]，
     * 则继承并实现[EditorAdapter]，其注释介绍了如何实现以及注意点。
     *
     * [EditorAdapter.onCreateView]创建的视图可能需要处理手势导航栏边到边，
     * [EditorHelper]提供了辅助函数，这些函数能够帮助处理手势导航栏边到边。
     */
    var editorAdapter: EditorAdapter<*>
        get() = requireNotNull(editorView.adapter) { "未初始化EditorAdapter" }
        set(value) {
            val previous = editorView.adapter
            if (previous === value) return
            previous?.let(editorAnimator::detach)
            previous?.detach(this, editorView)
            editorView.setAdapter(value)
            value.attach(this, editorView)
            value.let(editorAnimator::attach)
            editorOffset = 0
            requestLayout()
        }

    /**
     * [editorAnimator]默认为[FadeEditorAnimator]
     *
     * 若需要调整动画的参数，则可以继承[FadeEditorAnimator]进行修改，
     * 或者参考[FadeEditorAnimator]，继承[EditorAnimator]实现动画。
     */
    var editorAnimator: EditorAnimator = FadeEditorAnimator()
        set(value) {
            val adapter = editorView.adapter
            adapter?.let(field::detach)
            field = value
            adapter?.let(field::attach)
        }

    init {
        editorAdapter = ImeAdapter()
    }

    override fun shouldDelayChildPressedState(): Boolean = false

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (window == null) {
            window = findViewTreeWindow()
        }
    }

    /**
     * [contentView]和[editorView]之间会有一个[navBarOffset]区域，
     * 当支持手势导航栏边到边时，[navBarOffset]等于导航栏的高度，此时显示[Editor]，
     * 在[editorOffset]超过[navBarOffset]后，才会更新[contentView]的尺寸或位置。
     *
     * 不能在[WindowInsetsAnimationCompat.Callback.onStart]判断IME显示情况，
     * 原因是`onStart()`的执行时机是`doOnPreDraw`，此时已完成measure和layout，
     * 观察更改[Editor]的逻辑可能会申请重新布局，若在`onStart()`更改[Editor]，
     * 则重新布局要在下一帧进行，当前帧draw绘制的是旧内容，对于不需要运行动画的场景，
     * 看起来像是“闪烁”了一下。
     *
     * 当前帧WindowInsets的分发[dispatchApplyWindowInsets]一般早于measure、layout、draw，
     * 因此在[onApplyWindowInsets]判断IME显示情况并更改[Editor]，确保draw之前尺寸和位置正确。
     */
    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets)
        navBarOffset = window?.getNavigationOffset(insetsCompat) ?: 0
        val imeHeight = window?.getImeHeight(insetsCompat) ?: 0
        editorView.dispatchIme(isShow = imeHeight > 0)
        return super.onApplyWindowInsets(insets)
    }

    /**
     * 更新编辑区的偏移值，该函数仅由[EditorAnimator]调用
     *
     * [EditorView]更改[Editor]后，[EditorAnimator]在下一帧[ViewTreeObserver.OnDrawListener.onDraw]，
     * 获取当前[Editor]的偏移（包括IME的偏移），此时已完成[editorView]和[contentView]的measure和layout。
     *
     * 若[editorMode]为[EditorMode.ADJUST_PAN]，则偏移[editorView]和[contentView]即可，
     * 若[editorMode]为[EditorMode.ADJUST_RESIZE]，并且[resizeInNextLayout]不为true，
     * 则对[contentView]重新measure和layout，确保[contentView]的尺寸和位置在当前帧draw之前是正确的，
     * measure有测量缓存，layout有边界对比，对[contentView]重新measure和layout不一定会产生性能损耗。
     */
    @Suppress("KotlinConstantConditions", "ConvertTwoComparisonsToRangeCheck")
    internal fun updateEditorOffset(offset: Int, resizeInNextLayout: Boolean) {
        val current = offset.coerceAtLeast(0)
        if (editorOffset == current) return
        val previous = editorOffset
        val editorDiff = previous - current
        editorOffset = current

        val contentView = contentView ?: return
        when (editorMode) {
            EditorMode.ADJUST_PAN -> {
                editorView.offsetTopAndBottom(editorDiff)
                val threshold = navBarOffset
                val contentDiff = when {
                    previous >= threshold && current >= threshold -> editorDiff
                    previous < threshold && current > threshold -> threshold - current
                    previous > threshold && current < threshold -> previous - threshold
                    else -> Int.MIN_VALUE
                }
                if (contentDiff != Int.MIN_VALUE) {
                    contentView.offsetTopAndBottom(contentDiff)
                }
            }
            EditorMode.ADJUST_RESIZE -> if (!resizeInNextLayout) {
                editorView.offsetTopAndBottom(editorDiff)
                measureContentView(contentView)
                layoutContentView(contentView)
            } else {
                requestLayout()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        checkContentView()
        val contentView = contentView ?: return
        measureContentView(contentView)
        editorView.measure(widthMeasureSpec, measuredHeight.toAtMostMeasureSpec())
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val contentView = contentView ?: return
        // 基于inputView底部向下布局editorView，
        // 通过editorOffset向上偏移editorView。
        editorView.let {
            val left = 0
            val top = measuredHeight - editorOffset
            val right = left + it.measuredWidth
            val bottom = top + it.measuredHeight
            it.layout(left, top, right, bottom)
        }

        // 基于editorView顶部向上布局contentView
        layoutContentView(contentView)
    }

    private fun Int.toExactlyMeasureSpec() = makeMeasureSpec(this, EXACTLY)

    private fun Int.toAtMostMeasureSpec() = makeMeasureSpec(this, AT_MOST)

    private fun checkContentView() {
        if (childCount == 0) return
        if (contentView == null) {
            require(childCount == 1) { "InputView初始化时只能有一个子View" }
            contentView = getChildAt(0)
            addView(editorView, MATCH_PARENT, WRAP_CONTENT)
        }
    }

    private fun measureContentView(contentView: View) {
        val horizontalMargin = contentView.let { it.marginLeft + it.marginRight }
        val verticalMargin = contentView.let { it.marginTop + it.marginBottom }
        val maxContentWidth = measuredWidth - horizontalMargin
        var maxContentHeight = measuredHeight - verticalMargin - navBarOffset
        if (editorMode === EditorMode.ADJUST_RESIZE) {
            maxContentHeight -= getLayoutOffset()
        }
        contentView.measure(
            when (val width = contentView.layoutParams.width) {
                MATCH_PARENT -> maxContentWidth.toExactlyMeasureSpec()
                WRAP_CONTENT -> maxContentWidth.toAtMostMeasureSpec()
                else -> width.coerceAtMost(maxContentWidth).toExactlyMeasureSpec()
            },
            maxContentHeight.toExactlyMeasureSpec()
        )
    }

    private fun layoutContentView(contentView: View) {
        contentView.let {
            val left = (measuredWidth - it.measuredWidth) / 2
            val bottom = height - it.marginBottom - navBarOffset - getLayoutOffset()
            val right = left + it.measuredWidth
            val top = bottom - it.measuredHeight
            it.layout(left, top, right, bottom)
        }
    }

    private fun getLayoutOffset(): Int {
        return (editorOffset - navBarOffset).coerceAtLeast(0)
    }

    companion object
}