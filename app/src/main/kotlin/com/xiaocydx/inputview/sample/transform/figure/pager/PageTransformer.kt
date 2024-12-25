package com.xiaocydx.inputview.sample.transform.figure.pager

import android.view.View
import androidx.annotation.FloatRange
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.absoluteValue
import kotlin.math.sign

class ScaleInTransformer(
    @FloatRange(from = 0.0, to = 1.0)
    minScale: Float = 0.85f
) : ViewPager2.PageTransformer {
    private val maxScale = 1f
    private val minScale = minScale.coerceAtLeast(0f)

    override fun transformPage(page: View, position: Float) {
        val fraction = position.absoluteValue
            .coerceAtLeast(0f)
            .coerceAtMost(1f)
        val width = page.width.toFloat()
        val height = page.height.toFloat()
        val sign = position.sign
        val pivotX = width * PIVOT_CENTER
        page.pivotX = pivotX - pivotX * sign * fraction
        page.pivotY = height * PIVOT_CENTER
        val scale = maxScale + (minScale - maxScale) * fraction
        page.scaleY = scale
        page.scaleX = scale
    }

    private companion object {
        const val PIVOT_CENTER = 0.5f
    }
}

class FadeInTransformer(
    @FloatRange(from = 0.0, to = 1.0)
    minAlpha: Float = 0.4f
) : ViewPager2.PageTransformer {
    private val maxAlpha = 1f
    private val minAlpha = minAlpha.coerceAtLeast(0f)

    override fun transformPage(page: View, position: Float) {
        val fraction = position.absoluteValue
            .coerceAtLeast(0f)
            .coerceAtMost(1f)
        page.alpha = maxAlpha + (minAlpha - maxAlpha) * fraction
    }
}