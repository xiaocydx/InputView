package com.xiaocydx.inputview.sample.scene.figure.pager

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import androidx.annotation.CallSuper
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginRight
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.xiaocydx.inputview.sample.common.CustomLayout
import com.xiaocydx.inputview.sample.common.layoutParams
import com.xiaocydx.inputview.sample.common.setRoundRectOutlineProvider
import com.xiaocydx.inputview.sample.scene.figure.Figure

/**
 * @author xcc
 * @date 2024/3/8
 */
class FigureView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : CustomLayout(context, attrs) {
    private val corners = 8.dp
    private var ratio = 1f
    private val ivCover = AppCompatImageView(context).apply {
        setRoundRectOutlineProvider(corners)
        addView(this, matchParent, matchParent)
    }

    val tvDubbing = AppCompatTextView(context).apply {
        maxLines = 1
        gravity = Gravity.CENTER
        verticalPadding = 5.dp
        horizontalPadding = 6.dp
        setTextColor(Color.WHITE)
        setBackgroundColor(0x4D000000)
        setRoundRectOutlineProvider(4.dp)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, 11.sp.toFloat())
        addView(this, wrapContent, wrapContent) {
            rightMargin = 4.dp
            bottomMargin = 6.dp
        }
    }

    init {
        layoutParams(matchParent, wrapContent)
    }

    fun setFigure(requestManager: RequestManager, figure: Figure) {
        ratio = figure.coverRatio
        ivCover.requestLayout()
        tvDubbing.text = figure.dubbing.name
        requestManager.load(figure.coverUrl)
            .placeholder(ColorDrawable(0xFF212123.toInt()))
            .transform(MultiTransformation(CenterCrop(), RoundedCorners(corners)))
            .into(DrawableImageViewTarget(ivCover).waitForLayout())
    }

    fun setAnimationAlpha(value: Float) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child === ivCover) continue
            child.alpha = value
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        alpha = 1f
        setAnimationAlpha(1f)
    }

    @CallSuper
    override fun onMeasureChildren(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (layoutParams.width < 0 || layoutParams.height < 0) {
            // 尺寸对齐长边
            var finalWidth = measuredWidth
            var finalHeight = measuredHeight
            if (measuredWidth < measuredHeight) {
                finalWidth = (measuredHeight * ratio).toInt()
            } else {
                finalHeight = (measuredWidth / ratio).toInt()
            }
            // 若尺寸超出边界，则对齐短边
            if (finalWidth > measuredWidth) {
                finalWidth = measuredWidth
                finalHeight = (measuredWidth / ratio).toInt()
            } else if (finalHeight > measuredHeight) {
                finalWidth = (measuredHeight * ratio).toInt()
                finalHeight = measuredHeight
            }
            setMeasuredDimension(finalWidth, finalHeight)
        }
        ivCover.measureWithDefault()
        tvDubbing.measureWithDefault()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        ivCover.layout(x = 0, y = 0)
        tvDubbing.let { it.layoutFromRight(x = it.marginRight, y = height - it.measuredHeightWithMargins) }
    }
}