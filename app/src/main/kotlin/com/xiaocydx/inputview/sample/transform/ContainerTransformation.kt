package com.xiaocydx.inputview.sample.transform

import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.sample.transform.OverlayTransformation.ContainerState
import com.xiaocydx.inputview.sample.transform.OverlayTransformation.EnforcerScope
import kotlinx.coroutines.CoroutineScope

/**
 * @author xcc
 * @date 2024/4/10
 */
abstract class ContainerTransformation<S : ContainerState>(
    vararg editors: Editor
) : OverlayTransformation<S> {
    private val editors = editors.toSet()
    protected abstract fun getView(state: S): View
    protected open fun onPrepare(state: S) = Unit
    protected open fun onStart(state: S) = Unit
    protected open fun onUpdate(state: S) = Unit
    protected open fun onEnd(state: S) = Unit
    protected open fun onLaunch(state: S, scope: CoroutineScope) = Unit

    final override fun prepare(state: S) {
        if (!isPrevious(state) && !isCurrent(state)) return
        val view = getView(state)
        if (view.parent !== state.container) {
            view.isInvisible = true
            state.container.addView(view)
        }
        onPrepare(state)
    }

    final override fun start(state: S) {
        if (!isPrevious(state) && !isCurrent(state)) return
        getView(state).isVisible = true
        onStart(state)
    }

    final override fun update(state: S) {
        val isPrevious = isPrevious(state)
        val isCurrent = isCurrent(state)
        when {
            isPrevious && !isCurrent && state.current != null -> {
                getView(state).alpha = state.startViewAlpha
            }
            !isPrevious && isCurrent && state.previous != null -> {
                getView(state).alpha = state.endViewAlpha
            }
            isPrevious && isCurrent -> getView(state).alpha = 1f
            !isPrevious && !isCurrent -> return
        }
        onUpdate(state)
    }

    final override fun end(state: S) {
        if (!isPrevious(state) && !isCurrent(state)) return
        if (!isCurrent(state)) {
            val view = getView(state)
            view.alpha = 1f
            state.container.removeView(view)
        }
        onEnd(state)
    }

    final override fun launch(state: S, scope: EnforcerScope) {
        if (!isCurrent(state)) return
        onLaunch(state, scope)
    }

    protected fun isPrevious(state: S): Boolean {
        if (state.previous == null) return false
        return editors.contains(state.previous)
    }

    protected fun isCurrent(state: S): Boolean {
        if (state.current == null) return false
        return editors.contains(state.current)
    }
}