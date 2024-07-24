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

@file:Suppress("PackageDirectoryMismatch")

package com.xiaocydx.inputview.transform

import android.animation.RectEvaluator
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.annotation.CallSuper
import androidx.transition.getBounds
import androidx.transition.setLeftTopRightBottomCompat

/**
 * @author xcc
 * @date 2024/7/24
 */
open class ContentBoundsChange() : Transformer {
    private val startBounds = Rect()
    private val endBounds = Rect()
    private val currentBounds = Rect()
    private val boundsEvaluator = RectEvaluator(currentBounds)
    private var match: ((Content?) -> Boolean)? = null
    private var matchStart = false
    private var matchEnd = false
    private var canTransform = false

    constructor(match: (Content?) -> Boolean) : this() {
        setMatch(match)
    }

    fun setMatch(match: ((Content?) -> Boolean)?) {
        this.match = match
    }

    @CallSuper
    override fun match(state: ImperfectState) = with(state) {
        matchStart = match?.invoke(previous?.content) ?: true
        matchEnd = match?.invoke(current?.content) ?: true
        startView() != null && endView() != null && startView() != endView()
    }

    @CallSuper
    override fun onStart(state: TransformState) = with(state) {
        startView()?.getBounds(startBounds)
        endView()?.getBounds(endBounds)
        canTransform = startBounds != endBounds
    }

    @CallSuper
    override fun onUpdate(state: TransformState) = with(state) {
        if (!canTransform) return@with
        boundsEvaluator.evaluate(interpolatedFraction, startBounds, endBounds)
        endView()?.setLeftTopRightBottomCompat(currentBounds)
    }

    protected fun ImperfectState.startView(): View? {
        return startViews.content?.takeIf { matchStart && it.layoutParams?.height != MATCH_PARENT }
    }

    protected fun ImperfectState.endView(): View? {
        return endViews.content?.takeIf { matchEnd && it.layoutParams?.height != MATCH_PARENT }
    }
}