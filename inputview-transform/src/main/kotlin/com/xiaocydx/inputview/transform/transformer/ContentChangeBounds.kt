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
import androidx.transition.getBounds
import androidx.transition.setLeftTopRightBottomCompat

/**
 * @author xcc
 * @date 2024/7/24
 */
class ContentChangeBounds() : ContentTransformer() {
    private val startBounds = Rect()
    private val endBounds = Rect()
    private val currentBounds = Rect()
    private val evaluator = RectEvaluator(currentBounds)

    constructor(match: ContentMatch) : this() {
        setMatch(match)
    }

    override fun onMatch(state: ImperfectState) = with(state) {
        startView() != null && endView() != null && startView() != endView()
    }

    override fun onStart(state: TransformState) {
        state.startView()?.getBounds(startBounds)
        state.endView()?.getBounds(endBounds)
    }

    override fun onUpdate(state: TransformState) {
        if (startBounds == endBounds) return
        evaluator.evaluate(state.interpolatedFraction, startBounds, endBounds)
        state.endView()?.setLeftTopRightBottomCompat(currentBounds)
    }
}