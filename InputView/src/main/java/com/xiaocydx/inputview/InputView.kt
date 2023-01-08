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
 * 5. [setEditorAnimator]设置的[EditorAnimator]，支持多种[Editor]的显示变换动画，
 * 默认实现为[DefaultEditorAnimator]，若不需要动画，则设置[NopEditorAnimator]。
 *
 * [contentView]的初始化布局位置等同于[Gravity.CENTER]，其测量高度不受`layoutParams.height`影响，
 * 最大值是[InputView]的测量高度，[Editor]的视图位于[contentView]下方，通知显示[Editor]的视图时，
 * 会平移[contentView]，或者修改[contentView]的尺寸，具体是哪一种行为取决于[EditorAnimator]的实现。
 *
 * @author xcc
 * @date 2023/1/6
 */
class InputView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {
    private val editorView = EditorView(context)
    private var editorAnimator: EditorAnimator = DefaultEditorAnimator.resize()
    private var contentView: View? = null

    /**
     * 编辑区的偏移值，可作为[EditorAnimator]动画的起始值
     */
    @get:IntRange(from = 0)
    var editorOffset = 0
        private set

    init {
        setEditorAdapter(DefaultEditorAdapter())
        ImeWindowInsetsHandler(this).attach()
    }

    /**
     * [editText]用于兼容Android各版本显示和隐藏IME
     *
     * 显示IME[editText]会获得焦点，隐藏IME若需要清除[editText]的焦点，
     * 则通过[EditorAdapter.addEditorVisibleListener]观察更改后的`current`，
     * 当`current`不是IME类型时，清除[editText]的焦点。
     */
    fun setEditText(editText: EditText) {
        editorView.setEditText(editText)
    }

    /**
     * [adapter]支持多种[Editor]的视图创建和显示
     *
     * 若只需要IME，则设置[DefaultEditorAnimator]：
     * ```
     * val adapter = DefaultEditorAdapter()
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
        previous?.detach(this, editorView)
        previous?.let(editorAnimator::detach)
        editorView.setAdapter(adapter)
        adapter.attach(this, editorView)
        adapter.let(editorAnimator::attach)
    }

    /**
     * [animator]支持多种[Editor]的显示动画
     *
     * 1. [DefaultEditorAnimator.pan]运行动画平移[contentView]。
     * 2. [DefaultEditorAnimator.resize]运行动画修改[contentView]的尺寸。
     * 3. [NopEditorAnimator.pan]不运行动画平移[contentView]。
     * 4. [NopEditorAnimator.pan]不运行动画修改[contentView]的尺寸。
     */
    fun setEditorAnimator(animator: EditorAnimator) {
        val adapter = editorView.adapter
        adapter?.let(editorAnimator::detach)
        editorAnimator = animator
        adapter?.let(editorAnimator::attach)
    }

    /**
     * 更新编辑区的偏移值，并平移[contentView]
     */
    fun offsetChildrenLocation(@IntRange(from = 0) offset: Int, diff: Int) {
        val safeOffset = offset.coerceAtLeast(0)
        if (editorOffset == safeOffset) return
        editorOffset = safeOffset
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            // 平移contentView不需要重新measure和layout
            child.offsetTopAndBottom(diff)
        }
    }

    /**
     * 更新编辑区的偏移值，并修改[contentView]的尺寸
     */
    fun offsetContentSize(@IntRange(from = 0) offset: Int) {
        val safeOffset = offset.coerceAtLeast(0)
        if (editorOffset == safeOffset) return
        editorOffset = safeOffset
        contentView?.updateLayoutParams {
            height = this@InputView.height - editorOffset
        }
    }

    override fun shouldDelayChildPressedState(): Boolean = false

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        checkContentView()
        val contentView = contentView ?: return
        val horizontalMargin = contentView.let { it.marginLeft + it.marginRight }
        val verticalMargin = contentView.let { it.marginTop + it.marginBottom }
        val maxContentWidth = measuredWidth - horizontalMargin
        val maxContentHeight = measuredHeight - verticalMargin
        contentView.measure(
            when (val width = contentView.layoutParams.width) {
                MATCH_PARENT -> maxContentWidth.toExactlyMeasureSpec()
                WRAP_CONTENT -> maxContentWidth.toAtMostMeasureSpec()
                else -> width.coerceAtMost(maxContentWidth).toExactlyMeasureSpec()
            },
            maxContentHeight.toExactlyMeasureSpec()
        )
        editorView.measure(widthMeasureSpec, measuredHeight.toAtMostMeasureSpec())
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        contentView ?: return
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
        contentView!!.let {
            val left = (measuredWidth - it.measuredWidth) / 2
            val bottom = editorView.top - it.marginBottom
            val right = left + it.measuredWidth
            val top = bottom - it.measuredHeight
            it.layout(left, top, right, bottom)
        }
    }

    // TODO: 支持导航栏edge-to-edge
    private class ImeWindowInsetsHandler(
        private val inputView: InputView
    ) : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
        /**
         * Android 8.0以下[imeType]跟[WindowInsetsAnimationCompat.getTypeMask]不一致，
         * 因此在[onStart]记录下[typeMask]，然后在[onProgress]跟[typeMask]进行匹配。
         */
        private var typeMask = NO_TYPE_MASK
        private val editorView = inputView.editorView
        private val imeType = WindowInsetsCompat.Type.ime()
        private val navBarsType = WindowInsetsCompat.Type.navigationBars()

        fun attach() {
            ViewCompat.setWindowInsetsAnimationCallback(editorView, this)
        }

        /**
         * 该函数调用自[ViewTreeObserver.OnDrawListener]，
         * 此时可以获取到编辑区和[contentView]的最新尺寸。
         */
        override fun onStart(
            animation: WindowInsetsAnimationCompat,
            bounds: WindowInsetsAnimationCompat.BoundsCompat
        ): WindowInsetsAnimationCompat.BoundsCompat {
            // 通过rootWindowInsets判断是显示还是隐藏IME，
            // 避免分发过程有父View替换IME的WindowInsets。
            val insets = ViewCompat.getRootWindowInsets(editorView)
            if (insets != null) {
                val imeInsets = insets.getInsets(imeType)
                val navBarsInsets = insets.getInsets(navBarsType)
                typeMask = animation.typeMask
                editorView.dispatchIme(isShow = imeInsets.bottom > 0)
                val value = (imeInsets.bottom - navBarsInsets.bottom).coerceAtLeast(0)
                inputView.editorAnimator.onImeAnimationStart(endValue = value)
            }
            return super.onStart(animation, bounds)
        }

        override fun onProgress(
            insets: WindowInsetsCompat,
            runningAnimations: MutableList<WindowInsetsAnimationCompat>
        ): WindowInsetsCompat {
            val imeAnimation = runningAnimations.firstOrNull { it.typeMask == typeMask }
            if (imeAnimation != null) {
                val imeInsets = insets.getInsets(imeType)
                val navBarsInsets = insets.getInsets(navBarsType)
                val value = (imeInsets.bottom - navBarsInsets.bottom).coerceAtLeast(0)
                inputView.editorAnimator.onImeAnimationUpdate(currentValue = value)
            }
            return insets
        }

        override fun onEnd(animation: WindowInsetsAnimationCompat) {
            typeMask = NO_TYPE_MASK
            inputView.editorAnimator.onImeAnimationEnd()
        }
    }

    companion object {
        private const val NO_TYPE_MASK = -1
    }
}