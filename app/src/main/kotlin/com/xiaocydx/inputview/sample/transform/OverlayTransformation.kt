package com.xiaocydx.inputview.sample.transform

import android.view.ViewGroup
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.sample.transform.OverlayTransformation.State
import kotlinx.coroutines.CoroutineScope

/**
 * @author xcc
 * @date 2024/4/10
 */
interface OverlayTransformation<in S : State> {

    fun prepare(state: S) = Unit

    fun start(state: S) = Unit

    fun update(state: S) = Unit

    fun end(state: S) = Unit

    fun launch(state: S, scope: EnforcerScope) = Unit

    interface EnforcerScope : CoroutineScope {
        fun requestDispatch(state: State)
    }

    fun interface StateProvider<S : State> {
        fun createState(): S
    }

    open class ContainerState(
        inputView: InputView,
        val container: ViewGroup
    ) : State(inputView)

    open class State(val inputView: InputView) {
        var previous: Editor? = null; private set
        var current: Editor? = null; private set
        var initialAnchorY = 0; private set
        var startAnchorY = 0; private set
        var endAnchorY = 0; private set
        var currentAnchorY = 0; private set
        var startViewAlpha = 1f; private set
        var endViewAlpha = 1f; private set
        var interpolatedFraction = 0f; private set

        fun setEditor(previous: Editor?, current: Editor?) {
            this.previous = previous
            this.current = current
        }

        fun setAnchorY(initial: Int, start: Int, end: Int) {
            initialAnchorY = initial
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