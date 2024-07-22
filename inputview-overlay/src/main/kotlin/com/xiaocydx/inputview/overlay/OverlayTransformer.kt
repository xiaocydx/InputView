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

package com.xiaocydx.inputview.overlay

import android.view.ViewGroup
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import com.xiaocydx.inputview.InputView

/**
 * @author xcc
 * @date 2024/7/22
 */
interface OverlayTransformer {

    // TODO: 优先级用于修改状态值
    // val priority: Int
    //     get() = 0

    fun onPrepare(state: PrepareState) = Unit

    fun onStart(state: TransformState) = Unit

    fun onUpdate(state: TransformState) = Unit

    fun onEnd(state: TransformState) = Unit
}

interface PrepareState {
    val inputView: InputView

    val container: ViewGroup

    val previous: OverlayScene<*, *>?

    val current: OverlayScene<*, *>?
}

interface TransformState : PrepareState {
    /**
     * 相对于Window的初始锚点Y
     */
    @get:IntRange(from = 0)
    val initialAnchorY: Int

    /**
     * 相对于Window的动画起始锚点Y
     */
    @get:IntRange(from = 0)
    val startAnchorY: Int

    /**
     * 相对于Window的动画结束锚点Y
     */
    @get:IntRange(from = 0)
    val endAnchorY: Int

    /**
     * 对于Window的动画当前锚点Y
     */
    @get:IntRange(from = 0)
    val currentAnchorY: Int

    /**
     * 动画起始状态和结束状态之间的原始分数进度
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    val animatedFraction: Float

    /**
     * 动画起始状态和结束状态之间的插值器分数进度
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    val interpolatedFraction: Float
}