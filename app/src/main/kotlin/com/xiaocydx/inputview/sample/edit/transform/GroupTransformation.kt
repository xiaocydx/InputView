package com.xiaocydx.inputview.sample.edit.transform

import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.xiaocydx.inputview.sample.edit.VideoEditor
import com.xiaocydx.inputview.sample.edit.transform.Transformation.State
import kotlinx.coroutines.CoroutineScope

/**
 * @author xcc
 * @date 2024/4/10
 */
abstract class GroupTransformation(vararg editors: VideoEditor) : Transformation {
    private val editors = editors.toSet()
    protected abstract fun getView(state: State): View
    protected open fun onAttach(state: State) = Unit
    protected open fun onStart(state: State) = Unit
    protected open fun onUpdate(state: State) = Unit
    protected open fun onEnd(state: State) = Unit
    protected open fun onLaunch(state: State, scope: CoroutineScope) = Unit

    override fun attach(state: State) {
        if (!isPrevious(state) && !isCurrent(state)) return
        val view = getView(state)
        if (view.parent !== state.container) {
            view.isInvisible = true
            state.container.addView(view)
        }
        onAttach(state)
    }

    override fun start(state: State) {
        if (!isPrevious(state) && !isCurrent(state)) return
        getView(state).isVisible = true
        onStart(state)
    }

    override fun update(state: State) {
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

    override fun end(state: State) {
        if (!isPrevious(state) && !isCurrent(state)) return
        if (!isCurrent(state)) {
            val view = getView(state)
            view.alpha = 1f
            state.container.removeView(view)
        }
        onEnd(state)
    }

    override fun launch(state: State, scope: CoroutineScope) {
        if (!isCurrent(state)) return
        onLaunch(state, scope)
    }

    protected fun isPrevious(state: State): Boolean {
        if (state.previous == null) return false
        return editors.contains(state.previous)
    }

    protected fun isCurrent(state: State): Boolean {
        if (state.current == null) return false
        return editors.contains(state.current)
    }
}