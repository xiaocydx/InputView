package com.xiaocydx.inputview.sample.edit.transform

import android.graphics.Rect
import android.os.Looper
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewTreeObserver
import androidx.transition.getBounds
import androidx.transition.setLeftTopRightBottomCompat
import com.xiaocydx.inputview.sample.edit.transform.Transformation.EnforcerScope
import com.xiaocydx.inputview.sample.edit.transform.Transformation.State
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * @author xcc
 * @date 2024/4/12
 */
class ContainerTransformation : Transformation<State> {
    private val previousBounds = Rect()
    private val currentBounds = Rect()
    private var canTransform = false
    private var previousHeight = 0
    private var currentHeight = 0
    private var translationY = 0

    override fun prepare(state: State) = with(state) {
        require(container.layoutParams?.height == WRAP_CONTENT)
        container.getBounds(previousBounds)
    }

    override fun start(state: State) = with(state) {
        container.getBounds(currentBounds)
        previousHeight = previousBounds.height()
        currentHeight = currentBounds.height()
        var start = startAnchorY
        var end = endAnchorY
        when {
            startAnchorY == initialAnchorY -> {
                translationY = currentHeight
                end -= currentHeight
            }
            endAnchorY == initialAnchorY -> {
                translationY = currentHeight
                start -= currentHeight
            }
            else -> {
                translationY = 0
                start -= previousHeight
                end -= currentHeight
            }
        }
        inputView.translationY = translationY.toFloat()
        setAnchorY(initialAnchorY, start, end)

        canTransform = previous != null && current != null && previousHeight != currentHeight
        if (canTransform) container.setLeftTopRightBottomCompat(previousBounds)
    }

    override fun update(state: State) = with(state) {
        var fraction = interpolatedFraction
        if (translationY > 0f) {
            if (startAnchorY == initialAnchorY) fraction = 1 - fraction
            inputView.translationY = translationY * fraction
        }
        if (canTransform) {
            val dy = previousHeight + (currentHeight - previousHeight) * fraction
            container.getBounds(currentBounds) { top = bottom - dy.toInt() }
            container.setLeftTopRightBottomCompat(currentBounds)
        }
    }

    override fun end(state: State) {
        state.inputView.translationY = 0f
    }

    override fun launch(state: State, scope: EnforcerScope): Unit = with(state) {
        if (current == null) return
        // TODO: 尝试简化
        scope.launch {
            suspendCancellableCoroutine<Unit> { cont ->
                var old = container.top
                val listener = ViewTreeObserver.OnDrawListener {
                    val new = container.top
                    if (old != new) {
                        val end = container.bottom
                        state.setEditor(current, current)
                        state.setAnchorY(initialAnchorY, end, end)
                        scope.requestDispatch(state)
                        old = new
                    }
                }
                container.viewTreeObserver.addOnDrawListener(listener)
                cont.invokeOnCancellation {
                    assert(Thread.currentThread() === Looper.getMainLooper().thread)
                    container.viewTreeObserver.removeOnDrawListener(listener)
                }
            }
        }
    }
}