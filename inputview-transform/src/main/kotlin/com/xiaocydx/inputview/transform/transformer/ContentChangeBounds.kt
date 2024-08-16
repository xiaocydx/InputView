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
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.transition.setLeftTopRightBottomCompat

/**
 * 对匹配的[Content]视图进行边界变换
 *
 * ### 匹配变换
 * 当匹配到两个[Content]视图时，才进行变换。可调用[setMatch]设置匹配条件。
 *
 * ### 变换效果
 * 两个[Content]视图中高度更高的那一个，调用`View.setLeftTopRightBottom()`更改边界。
 *
 * ### 适用场景
 * 1. 匹配的[Content]，其视图高度为固定值或[WRAP_CONTENT]。
 * 2. 可搭配[ChangeScale]、[ContentChangeTranslation]使用。
 *
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
        val view = if (startBounds.height() > endBounds.height()) state.startView() else state.endView()
        view?.setLeftTopRightBottomCompat(currentBounds)
    }

    override fun onEnd(state: TransformState) {
        state.startView()?.setLeftTopRightBottomCompat(startBounds)
        state.endView()?.setLeftTopRightBottomCompat(endBounds)
    }
}