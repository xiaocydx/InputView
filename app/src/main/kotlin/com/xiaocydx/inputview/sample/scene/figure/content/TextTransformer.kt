package com.xiaocydx.inputview.sample.scene.figure.content

import android.animation.RectEvaluator
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.updatePadding
import com.xiaocydx.inputview.sample.databinding.FragmentTextBinding
import com.xiaocydx.inputview.sample.scene.figure.FigureContent.Text
import com.xiaocydx.inputview.sample.scene.figure.FigureScene
import com.xiaocydx.inputview.transform.ImperfectState
import com.xiaocydx.inputview.transform.TransformState
import com.xiaocydx.inputview.transform.Transformer
import com.xiaocydx.inputview.transform.ViewLocation
import com.xiaocydx.inputview.transform.isCurrent
import com.xiaocydx.inputview.transform.isEnter
import com.xiaocydx.inputview.transform.isPrevious
import com.xiaocydx.inputview.transform.isReturn
import com.xiaocydx.inputview.transform.set
import com.xiaocydx.inputview.transform.updatePaddings
import com.xiaocydx.insets.isGestureNavigationBar
import com.xiaocydx.insets.navigationBarHeight
import com.xiaocydx.insets.statusBarHeight
import com.xiaocydx.insets.updateLayoutSize
import com.xiaocydx.insets.updateMargins
import java.lang.ref.WeakReference

/**
 * [TextFragment]的进入和退出变换
 */
class TextEnterReturn(
    private val binding: FragmentTextBinding,
    private val textTarget: () -> WeakReference<View>?,
    private val lastInsets: () -> WindowInsetsCompat
) : Transformer() {
    private val point = IntArray(2)
    private val startPaddings = Rect()
    private val endPaddings = Rect()
    private val currentPaddings = Rect()
    private val paddingsEvaluator = RectEvaluator(currentPaddings)
    private val initialToolsHeight = binding.llTools.layoutParams.height
    private var toolsMarginBottom = 0

    override fun match(state: ImperfectState) = with(state) {
        isEnter(Text) || isReturn(Text)
    }

    override fun onPrepare(state: ImperfectState) = with(state) {
        val isEnter = isEnter(Text)
        // contentView布局完成过，取contentView的位置进行计算
        contentView.getLocationInWindow(point)

        val textLocation = textTarget()?.get()?.let(ViewLocation::from) ?: ViewLocation()
        val paddings = if (isEnter) startPaddings else endPaddings
        paddings.set(textLocation)
        paddings.right = rootView.width - paddings.right
        paddings.bottom = point[1] + rootView.height - paddings.bottom

        // 在下一帧布局之前设置startPaddings和toolsHeight
        if (isEnter) binding.root.updatePaddings(startPaddings)
        if (isEnter) binding.llTools.updateLayoutSize(height = 0)
    }

    override fun onStart(state: TransformState) = with(state) {
        textTarget()?.get()?.alpha = 0f
        val paddings = if (isEnter(Text)) endPaddings else startPaddings
        val bottom = if (isEnter(Text)) endOffset else startOffset
        paddings.set(0, lastInsets().statusBarHeight, 0, bottom)
        toolsMarginBottom = binding.llTools.marginBottom
    }

    override fun onUpdate(state: TransformState) = with(state) {
        paddingsEvaluator.evaluate(state.interpolatedFraction, startPaddings, endPaddings)
        binding.root.updatePaddings(currentPaddings)

        val fraction = if (isEnter(Text)) interpolatedFraction else 1f - interpolatedFraction
        binding.llTools.alpha = fraction
        binding.llTools.updateLayoutSize(height = (initialToolsHeight * fraction).toInt())
        if (isReturn(Text)) binding.llTools.updateMargins(bottom = (toolsMarginBottom * fraction).toInt())
    }

    override fun onEnd(state: TransformState) {
        textTarget()?.get()?.alpha = 1f
        binding.llTools.alpha = 1f
        binding.llTools.updateLayoutSize(height = initialToolsHeight)
    }
}

/**
 * [TextFragment]的`paddings`变换，支持转换到其它[FigureScene]
 */
class TextChangePaddings(
    private val binding: FragmentTextBinding,
    private val lastInsets: () -> WindowInsetsCompat
) : Transformer() {
    private var paddingTop = 0
    private var toolsMarginBottom = 0

    override fun match(state: ImperfectState) = with(state) {
        previous != null && current != null && (isPrevious(Text) || isCurrent(Text))
    }

    override fun onStart(state: TransformState) {
        // 无论父级是否构建了导航栏Insets的消费逻辑，此处都做手势导航栏的判断
        val isGesture = lastInsets().isGestureNavigationBar(binding.root)
        paddingTop = lastInsets().statusBarHeight
        toolsMarginBottom = if (isGesture) lastInsets().navigationBarHeight else 0
    }

    override fun onUpdate(state: TransformState) = with(state) {
        val fraction = when (currentOffset) {
            in 0..toolsMarginBottom -> {
                1f - currentOffset.toFloat() / toolsMarginBottom
            }
            else -> 0f
        }
        binding.llTools.updateMargins(bottom = (toolsMarginBottom * fraction).toInt())
        binding.root.updatePadding(0, paddingTop, 0, state.currentOffset)
    }
}