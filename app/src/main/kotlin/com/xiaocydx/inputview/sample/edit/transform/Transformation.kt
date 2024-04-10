package com.xiaocydx.inputview.sample.edit.transform

import android.view.ViewGroup
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.sample.edit.VideoEditor
import kotlinx.coroutines.CoroutineScope

/**
 * @author xcc
 * @date 2024/4/10
 */
interface Transformation {

    fun attach(state: State) = Unit

    fun start(state: State) = Unit

    fun update(state: State) = Unit

    fun end(state: State) = Unit

    fun launch(state: State, scope: CoroutineScope) = Unit

    class State(
        val inputView: InputView,
        val container: ViewGroup,
        val previous: VideoEditor?,
        val current: VideoEditor?
    ) {
        var startAnchorY = 0; private set
        var endAnchorY = 0; private set
        var currentAnchorY = 0; private set
        var startViewAlpha = 1f; private set
        var endViewAlpha = 1f; private set
        var interpolatedFraction = 0f; private set

        fun setAnchorY(start: Int, end: Int) {
            startAnchorY = start
            endAnchorY = end
            currentAnchorY = start
        }

        fun setViewAlpha(start: Float, end: Float) {
            startViewAlpha = start
            endViewAlpha = end
        }

        fun setInterpolatedFraction(fraction: Float) {
            interpolatedFraction = fraction
            currentAnchorY = startAnchorY + ((endAnchorY - startAnchorY) * fraction).toInt()
        }
    }
}

operator fun Transformation.plus(
    other: Transformation
): Transformation = CombinedTransformation(this, other)

private class CombinedTransformation(
    private val first: Transformation,
    private val second: Transformation
) : Transformation {
    override fun attach(state: Transformation.State) {
        first.attach(state)
        second.attach(state)
    }

    override fun start(state: Transformation.State) {
        first.start(state)
        second.start(state)
    }

    override fun update(state: Transformation.State) {
        first.update(state)
        second.update(state)
    }

    override fun end(state: Transformation.State) {
        first.end(state)
        second.end(state)
    }

    override fun launch(state: Transformation.State, scope: CoroutineScope) {
        first.launch(state, scope)
        second.launch(state, scope)
    }
}