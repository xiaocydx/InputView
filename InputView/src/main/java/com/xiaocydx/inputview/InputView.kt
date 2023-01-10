package com.xiaocydx.inputview

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.View.MeasureSpec.*
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewTreeObserver
import android.widget.EditText
import androidx.annotation.IntRange
import androidx.core.view.*

/**
 * 输入控件
 *
 * 1. 在[Activity.onCreate]调用`InputView.init(window)`。
 * 2. [InputView]初始化时只能有一个子View，该子View将作为[contentView]。
 * 3. [setEditText]设置的[EditText]，用于兼容Android各版本显示和隐藏软键盘。
 * 4. [setEditorAdapter]设置的[EditorAdapter]，支持多种[Editor]的视图创建和显示。
 * 5. [setEditorAnimator]设置的[EditorAnimator]，支持[Editor]之间的切换动画。
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
    private var editorMode: EditorMode = EditorMode.ADJUST_RESIZE
    private var editorAnimator: EditorAnimator = DefaultEditorAnimator()
    private var contentView: View? = null

    /**
     * 编辑区的偏移值，可作为[EditorAnimator]动画的起始值
     */
    @get:IntRange(from = 0)
    var editorOffset = 0
        private set

    init {
        setEditorAdapter(ImeAdapter())
        WindowInsetsAnimationHandler().attach()
    }

    /**
     * 设置用于兼容Android各版本显示和隐藏IME的[EditText]
     *
     * 显示IME[editText]会获得焦点，隐藏IME会清除[editText]的焦点，
     * 可以观察[EditorAdapter.addEditorVisibleListener]的更改结果，
     * 处理[editText]的焦点。
     */
    fun setEditText(editText: EditText) {
        editorView.setEditText(editText)
    }

    /**
     * 设置[EditorMode]，默认是[EditorMode.ADJUST_RESIZE]
     *
     * @param mode
     * 1. [EditorMode.ADJUST_PAN]，显示[Editor]时平移[contentView]。
     * 2. [EditorMode.ADJUST_RESIZE]，显示[Editor]时修改[contentView]的尺寸。
     */
    fun setEditorMode(mode: EditorMode) {
        editorMode = mode
        requestLayout()
    }

    /**
     * 设置[EditorAdapter]，默认是[ImeAdapter]
     *
     * [ImeAdapter]可用于只需要IME的场景：
     * ```
     * val adapter = ImeAdapter()
     * inputView.setEditorAdapter(adapter)
     *
     * // 显示IME
     * adapter.notifyShowIme()
     * // 隐藏IME
     * adapter.notifyHideIme()
     * ```
     */
    fun setEditorAdapter(adapter: EditorAdapter<*>) {
        val previous = editorView.adapter
        previous?.let(editorAnimator::detach)
        previous?.detach(this, editorView)
        editorView.setAdapter(adapter)
        adapter.attach(this, editorView)
        adapter.let(editorAnimator::attach)
        editorOffset = 0
        requestLayout()
    }

    /**
     * 设置[EditorAnimator]，默认是[DefaultEditorAnimator]
     *
     * @param animator 若为`null`，则不运行动画
     */
    fun setEditorAnimator(animator: EditorAnimator?) {
        val adapter = editorView.adapter
        adapter?.let(editorAnimator::detach)
        editorAnimator = animator ?: NopEditorAnimator()
        adapter?.let(editorAnimator::attach)
    }

    override fun shouldDelayChildPressedState(): Boolean = false

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    /**
     * 更新编辑区的偏移值，该函数仅由[EditorAnimator]调用
     *
     * [EditorView]更改[Editor]后，[EditorAnimator]在下一帧[ViewTreeObserver.OnDrawListener.onDraw]，
     * 获取当前[Editor]的偏移（包括IME的偏移），此时已完成[editorView]和[contentView]的measure和layout，
     * 若[editorMode]为[EditorMode.ADJUST_PAN]，则偏移[editorView]和[contentView]即可，
     * 若[editorMode]为[EditorMode.ADJUST_RESIZE]，并且[resizeInNextLayout]不为true，
     * 则对[contentView]重新measure和layout，确保[contentView]的尺寸和位置在当前帧draw之前是正确的，
     * measure有测量缓存，layout有边界对比，对[contentView]重新measure和layout不一定会产生性能损耗。
     */
    internal fun updateEditorOffset(offset: Int, resizeInNextLayout: Boolean) {
        val safeOffset = offset.coerceAtLeast(0)
        if (editorOffset == safeOffset) return
        val diff = editorOffset - safeOffset
        editorOffset = safeOffset

        val contentView = contentView ?: return
        when (editorMode) {
            EditorMode.ADJUST_PAN -> {
                editorView.offsetTopAndBottom(diff)
                contentView.offsetTopAndBottom(diff)
            }
            EditorMode.ADJUST_RESIZE -> if (!resizeInNextLayout) {
                editorView.offsetTopAndBottom(diff)
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
        var maxContentHeight = measuredHeight - verticalMargin
        if (editorMode === EditorMode.ADJUST_RESIZE) {
            maxContentHeight -= editorOffset
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
            val bottom = editorView.top - it.marginBottom
            val right = left + it.measuredWidth
            val top = bottom - it.measuredHeight
            it.layout(left, top, right, bottom)
        }
    }

    // TODO: 支持导航栏edge-to-edge
    private inner class WindowInsetsAnimationHandler :
            WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
        private var typeMask = NO_TYPE_MASK
        private val imeType = WindowInsetsCompat.Type.ime()
        private val navBarsType = WindowInsetsCompat.Type.navigationBars()

        fun attach() {
            ViewCompat.setWindowInsetsAnimationCallback(editorView, this)
        }

        /**
         * 该函数调用自[ViewTreeObserver.OnDrawListener.onDraw]，
         * 此时可以获取[contentView]和[editorView]的最新尺寸。
         */
        override fun onStart(
            animation: WindowInsetsAnimationCompat,
            bounds: WindowInsetsAnimationCompat.BoundsCompat
        ): WindowInsetsAnimationCompat.BoundsCompat {
            if (typeMask == NO_TYPE_MASK && animation.typeMask and imeType == imeType) {
                val insets = ViewCompat.getRootWindowInsets(editorView)
                if (insets != null) {
                    val imeInsets = insets.getInsets(imeType)
                    val navBarsInsets = insets.getInsets(navBarsType)
                    typeMask = animation.typeMask
                    editorView.dispatchIme(isShow = imeInsets.bottom > 0)
                    val value = (imeInsets.bottom - navBarsInsets.bottom).coerceAtLeast(0)
                    editorAnimator.onWindowInsetsAnimationStart(value, animation)
                }
            }
            return super.onStart(animation, bounds)
        }

        override fun onProgress(
            insets: WindowInsetsCompat,
            runningAnimations: MutableList<WindowInsetsAnimationCompat>
        ): WindowInsetsCompat {
            val animation = runningAnimations.firstOrNull { it.typeMask == typeMask }
            if (animation != null) {
                val imeInsets = insets.getInsets(imeType)
                val navBarsInsets = insets.getInsets(navBarsType)
                val value = (imeInsets.bottom - navBarsInsets.bottom).coerceAtLeast(0)
                editorAnimator.onWindowInsetsAnimationProgress(value, animation)
            }
            return insets
        }

        override fun onEnd(animation: WindowInsetsAnimationCompat) {
            if (animation.typeMask == typeMask) {
                editorAnimator.onWindowInsetsAnimationEnd(animation)
            }
            typeMask = NO_TYPE_MASK
        }
    }

    companion object {
        private const val NO_TYPE_MASK = -1
    }
}