package com.xiaocydx.inputview.sample.transform.figure.content

import android.animation.FloatEvaluator
import com.xiaocydx.inputview.sample.transform.figure.FigureContent.Cover
import com.xiaocydx.inputview.sample.transform.figure.FigureScene
import com.xiaocydx.inputview.sample.transform.figure.FitCenter
import com.xiaocydx.inputview.sample.transform.figure.ViewDrawable
import com.xiaocydx.inputview.sample.transform.figure.pager.FigureView
import com.xiaocydx.inputview.transform.ImperfectState
import com.xiaocydx.inputview.transform.TransformState
import com.xiaocydx.inputview.transform.Transformer
import com.xiaocydx.inputview.transform.isCurrent
import com.xiaocydx.inputview.transform.isEnter
import com.xiaocydx.inputview.transform.isPrevious
import com.xiaocydx.inputview.transform.isReturn

/**
 * [CoverFragment]的进入和退出变换
 */
class CoverEnterReturn(private val drawable: ViewDrawable<FigureView>) : Transformer() {
    private var fitCenter: FitCenter? = null
    private val evaluator = FloatEvaluator()

    override fun match(state: ImperfectState) = with(state) {
        isEnter(Cover) || isReturn(Cover)
    }

    override fun onStart(state: TransformState): Unit = with(state) {
        drawable.target?.alpha = 0f
        val extraMarginBottom = if (current != null) endOffset else startOffset
        fitCenter = drawable.calculateFitCenter(extraMarginBottom = extraMarginBottom)
    }

    override fun onUpdate(state: TransformState) = with(state) {
        val isEnter = current != null
        drawable.target?.setAnimationAlpha(when {
            isEnter -> 1f - interpolatedFraction
            else -> interpolatedFraction
        })
        val startScale = if (isEnter) 1f else fitCenter!!.scale
        val startTransY = if (isEnter) 0f else fitCenter!!.translationY
        val endScale = if (isEnter) fitCenter!!.scale else 1f
        val endTransY = if (isEnter) fitCenter!!.translationY else 0f
        val scale = evaluator.evaluate(interpolatedFraction, startScale, endScale)
        val translationY = evaluator.evaluate(interpolatedFraction, startTransY, endTransY)
        drawable.setValues(scale, translationY)
    }

    override fun onEnd(state: TransformState) {
        drawable.target?.alpha = 1f
    }
}

/**
 * [CoverFragment]的居中变换，支持转换到其它[FigureScene]
 */
class CoverChangeFitCenter(private val drawable: ViewDrawable<FigureView>) : Transformer() {
    private var start: FitCenter? = null
    private var end: FitCenter? = null
    private val evaluator = FloatEvaluator()

    override fun match(state: ImperfectState) = with(state) {
        previous != null && current != null && (isPrevious(Cover) || isCurrent(Cover))
    }

    override fun onStart(state: TransformState) = with(state) {
        drawable.target?.setAnimationAlpha(0f)
        start = drawable.calculateFitCenter(extraMarginBottom = startOffset)
        end = drawable.calculateFitCenter(extraMarginBottom = endOffset)
    }

    override fun onUpdate(state: TransformState) = with(state) {
        val scale = evaluator.evaluate(interpolatedFraction, start!!.scale, end!!.scale)
        val translationY = evaluator.evaluate(interpolatedFraction, start!!.translationY, end!!.translationY)
        drawable.setValues(scale, translationY)
    }

    override fun onEnd(state: TransformState) = with(state) {
        if (!isCurrent(Cover)) drawable.target?.setAnimationAlpha(1f)
    }
}