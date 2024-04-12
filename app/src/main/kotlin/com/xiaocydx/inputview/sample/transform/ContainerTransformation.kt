/*
 * Copyright 2023 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaocydx.inputview.sample.transform

import android.view.View
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.sample.transform.OverlayTransformation.ContainerState
import com.xiaocydx.inputview.sample.transform.OverlayTransformation.EnforcerScope
import kotlinx.coroutines.CoroutineScope

/**
 * 覆盖层的容器变换动画，[editors]共用一个View和变换逻辑
 *
 * @author xcc
 * @date 2024/4/10
 */
abstract class ContainerTransformation<S : ContainerState>(
    vararg editors: Editor
) : OverlayTransformation<S> {
    private val editors = editors.toSet()

    /**
     * 添加进`state.container`的View，实现类应当确保每次调用该函数都是同一个View
     */
    protected abstract fun getView(state: S): View

    /**
     * 调用自[prepare]，当[editors]包含`state.previous`或`state.current`时，该函数才被调用
     */
    protected open fun onPrepare(state: S) = Unit

    /**
     * 调用自[start]，当[editors]包含`state.previous`或`state.current`时，该函数才被调用
     */
    protected open fun onStart(state: S) = Unit

    /**
     * 调用自[update]，当[editors]包含`state.previous`或`state.current`时，该函数才被调用
     */
    protected open fun onUpdate(state: S) = Unit

    /**
     * 调用自[onEnd]，当[editors]包含`state.previous`或`state.current`时，该函数才被调用
     */
    protected open fun onEnd(state: S) = Unit

    /**
     * 调用自[launch]，当[editors]包含`state.current`时，该函数才被调用
     */
    protected open fun onLaunch(state: S, scope: CoroutineScope) = Unit

    /**
     * [editors]是否包含`state.previous`
     */
    protected fun isPrevious(state: S): Boolean {
        if (state.previous == null) return false
        return editors.contains(state.previous)
    }

    /**
     * [editors]是否包含`state.current`
     */
    protected fun isCurrent(state: S): Boolean {
        if (state.current == null) return false
        return editors.contains(state.current)
    }

    final override fun prepare(state: S) {
        if (!isPrevious(state) && !isCurrent(state)) return
        val view = getView(state)
        if (view.parent !== state.container) {
            state.container.addView(view)
        }
        onPrepare(state)
    }

    final override fun start(state: S) {
        if (!isPrevious(state) && !isCurrent(state)) return
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
}