package com.xiaocydx.inputview.sample.scene.figure

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.graphics.withMatrix
import java.lang.ref.WeakReference
import kotlin.math.min

/**
 * @author xcc
 * @date 2024/8/15
 */
class ViewDrawable<T : View> : Drawable() {
    private val matrix = Matrix()
    private var scale = 1f
    private var translationY = 0f
    private var marginTop = 0
    private var marginBottom = 0
    private var marginHorizontal = 0
    private var targetRef: WeakReference<T>? = null
    private var targetBounds: ViewBounds = ViewBounds()

    /**
     * 用于绘制的目标View
     */
    val target: T?
        get() = targetRef?.get()

    /**
     * 附加到[host]，当[host]尺寸变更时，同步调用[setBounds]
     */
    fun attachToHost(host: View) {
        host.overlay.add(this)
        host.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            setBounds(0, 0, host.width, host.height)
        }
    }

    /**
     * 设置用于绘制的目标View，内部弱引用持有[target]，
     * [target]当前尺寸和在Window的位置，将作为绘制参数。
     */
    fun setTarget(target: T?): T? {
        val previous = target
        this.targetRef = target?.let(::WeakReference)
        targetBounds = targetRef?.get()?.let(ViewBounds::from) ?: ViewBounds()
        invalidateSelf()
        return previous
    }

    /**
     * 设置绘制[target]的[scale]和[translationY]
     *
     * @param scale        相对[target]尺寸的缩放
     * @param translationY 相对[target]中心点的y轴平移
     */
    fun setValues(scale: Float, translationY: Float) {
        this.scale = scale
        this.translationY = translationY
        invalidateSelf()
    }

    /**
     * 设置用于[calculateFitCenter]的`margins`
     */
    fun setMargins(
        top: Int = marginTop,
        bottom: Int = marginBottom,
        horizontal: Int = marginHorizontal
    ) {
        marginTop = top
        marginBottom = bottom
        marginHorizontal = horizontal
    }

    /**
     * 在`bounds`去除`margin`后的区域中，计算[target]居中的绘制参数
     */
    fun calculateFitCenter(
        extraMarginTop: Int = 0,
        extraMarginBottom: Int = 0,
        extraMarginHorizontal: Int = 0,
    ): FitCenter {
        val marginTop = marginTop + extraMarginTop
        val marginBottom = marginBottom + extraMarginBottom
        val marginHorizontal = marginHorizontal + extraMarginHorizontal

        val maxWidth = bounds.width() - marginHorizontal
        val maxHeight = bounds.height() - marginTop - marginBottom
        val scaleX = maxWidth.toFloat() / targetBounds.width
        val scaleY = maxHeight.toFloat() / targetBounds.height
        val minScale = min(scaleX, scaleY)

        val targetY = marginTop + (maxHeight.toFloat() / 2)
        val initialY = targetBounds.top + (targetBounds.height.toFloat() / 2)
        val translationY = targetY - initialY
        return FitCenter(minScale, translationY)
    }

    override fun draw(canvas: Canvas) {
        val target = target ?: return
        val targetW = targetBounds.width.toFloat()
        val targetH = targetBounds.height.toFloat()
        val targetCenterX = targetBounds.left.toFloat() + targetW / 2
        val targetCenterY = targetBounds.top.toFloat() + targetH / 2
        // canvas的矩阵变换是右乘，为了让代码容易理解，用matrix编写左乘
        matrix.reset()
        // target默认绘制在左上角
        matrix.postTranslate(-targetW / 2, -targetH / 2)
        matrix.postScale(scale, scale)
        matrix.postTranslate(targetCenterX, targetCenterY + translationY)
        canvas.withMatrix(matrix, target::draw)
    }

    override fun setAlpha(alpha: Int) = Unit
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit
    override fun getOpacity() = PixelFormat.UNKNOWN
}

data class FitCenter(val scale: Float, val translationY: Float)

data class ViewBounds(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) {
    val width = right - left
    val height = bottom - top

    companion object {
        fun from(view: View) = run {
            val out = IntArray(2)
            view.getLocationInWindow(out)
            ViewBounds(
                left = out[0],
                top = out[1],
                right = out[0] + view.width,
                bottom = out[1] + view.height
            )
        }
    }
}

fun Rect.set(bounds: ViewBounds) {
    bounds.apply { set(left, top, right, bottom) }
}