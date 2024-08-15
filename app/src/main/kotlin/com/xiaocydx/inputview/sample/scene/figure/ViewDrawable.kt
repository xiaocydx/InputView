package com.xiaocydx.inputview.sample.scene.figure

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.graphics.withMatrix
import java.lang.ref.WeakReference
import kotlin.math.min

/**
 * @author xcc
 * @date 2024/8/15
 */
class ViewDrawable : Drawable() {
    private val matrix = Matrix()
    private var ref: WeakReference<View>? = null
    private var scale = 1f
    private var translationY = 0f
    private var marginTop = 0
    private var marginBottom = 0
    private var marginHorizontal = 0

    val target: View?
        get() = ref?.get()

    var targetBounds: ViewBounds = ViewBounds()
        private set

    fun addToHost(host: View) {
        host.overlay.add(this)
        host.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            setBounds(0, 0, host.width, host.height)
        }
    }

    fun setTarget(ref: WeakReference<View>?): View? {
        val previous = target
        this.ref = ref
        targetBounds = ref?.get()?.let(ViewBounds::from) ?: ViewBounds()
        invalidateSelf()
        return previous
    }

    fun setValues(scale: Float, translationY: Float) {
        this.scale = scale
        this.translationY = translationY
        invalidateSelf()
    }

    fun setMargins(
        top: Int = marginTop,
        bottom: Int = marginBottom,
        horizontal: Int = marginHorizontal
    ) {
        marginTop = top
        marginBottom = bottom
        marginHorizontal = horizontal
    }

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
        val x = targetBounds.left.toFloat()
        val y = targetBounds.top.toFloat()
        val width = targetBounds.width.toFloat()
        val height = targetBounds.height.toFloat()
        matrix.reset()
        matrix.postTranslate(-width / 2, -height / 2)
        matrix.postScale(scale, scale)
        matrix.postTranslate(width / 2, height / 2)
        matrix.postTranslate(x, y + translationY)
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