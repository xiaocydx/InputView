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

import android.view.Gravity.BOTTOM
import androidx.transition.updateLayoutGravity

/**
 * @author xcc
 * @date 2024/7/24
 */
class ContentChangeTranslation() : ContentTransformer() {

    constructor(matchContent: Content) : this() {
        setMatchContent(matchContent)
    }

    constructor(match: ContentMatch) : this() {
        setMatch(match)
    }

    override fun onMatch(state: ImperfectState) = with(state) {
        startView() != null || endView() != null
    }

    override fun onPrepare(state: ImperfectState): Unit = with(state) {
        startView()?.updateLayoutGravity(BOTTOM)
        endView()?.updateLayoutGravity(BOTTOM)
    }

    override fun onUpdate(state: TransformState) = with(state) {
        if (previous == null || current == null) {
            val fraction = when (previous) {
                null -> 1f - interpolatedFraction
                else -> interpolatedFraction
            }
            val view = startView() ?: endView()
            val height = view?.height ?: 0
            val offset = (height * fraction).toInt()
            rootView.translationY = offset.toFloat()
        }
        startView()?.translationY = -currentOffset.toFloat()
        endView()?.translationY = -currentOffset.toFloat()
    }

    override fun onEnd(state: TransformState) {
        state.rootView.translationY = 0f
    }
}