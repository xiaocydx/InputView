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

import android.view.View
import android.view.ViewGroup
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import com.xiaocydx.inputview.AnimationState
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.InputView

/**
 * @author xcc
 * @date 2024/7/24
 */
interface Transformer {

    fun match(state: ImperfectState): Boolean

    fun onPrepare(state: ImperfectState) = Unit

    fun onStart(state: TransformState) = Unit

    fun onUpdate(state: TransformState) = Unit

    fun onEnd(state: TransformState) = Unit
}

/**
 * [Transformer]的不完整状态，用于[Transformer.match]和[Transformer.onPrepare]
 */
interface ImperfectState {

    val rootView: ViewGroup

    val inputView: InputView

    val contentView: ViewGroup

    val previous: Scene<*, *>?

    val current: Scene<*, *>?

    /**
     * 动画起始视图组，其中[TransformViews.editor]等同于[AnimationState.startView]
     */
    val startViews: TransformViews

    /**
     * 动画结束视图组，其中[TransformViews.editor]等同于[AnimationState.endView]
     */
    val endViews: TransformViews
}

/**
 * [Transformer]的动画状态
 */
interface TransformState : ImperfectState {

    /**
     * 动画起始偏移值，等同于[AnimationState.startOffset]
     */
    @get:IntRange(from = 0)
    val startOffset: Int

    /**
     * 动画结束偏移值，等同于[AnimationState.endOffset]
     */
    @get:IntRange(from = 0)
    val endOffset: Int

    /**
     * 基于[interpolatedFraction]计算的动画当前偏移值，等同于[AnimationState.currentOffset]
     */
    @get:IntRange(from = 0)
    val currentOffset: Int

    /**
     * 动画起始状态和结束状态之间的原始分数进度，等同于[AnimationState.animatedFraction]
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    val animatedFraction: Float

    /**
     * 动画起始状态和结束状态之间的插值器分数进度，等同于[AnimationState.interpolatedFraction]
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    val interpolatedFraction: Float
}

/**
 * [Transformer]的动画视图组
 */
interface TransformViews {
    /**
     * [Scene.content]的视图
     */
    val content: View?

    /**
     * [Scene.editor]的视图
     */
    val editor: View?

    @get:FloatRange(from = 0.0, to = 1.0)
    val alpha: Float

    fun applyAlpha(alpha: Float = this.alpha) {
        content?.alpha = alpha
        editor?.alpha = alpha
    }
}

fun ImperfectState.isPrevious(scene: Scene<*, *>?) = previous === scene

fun ImperfectState.isPrevious(content: Content?) = previous?.content === content

fun ImperfectState.isPrevious(editor: Editor?) = previous?.editor === editor

fun ImperfectState.isCurrent(scene: Scene<*, *>?) = current === scene

fun ImperfectState.isCurrent(content: Content?) = current?.content === content

fun ImperfectState.isCurrent(editor: Editor?) = current?.editor === editor

fun ImperfectState.view(content: Content) = when {
    isPrevious(content) -> startViews.content
    isCurrent(content) -> endViews.content
    else -> null
}

fun ImperfectState.view(editor: Editor?) = when {
    isPrevious(editor) -> startViews.editor
    isCurrent(editor) -> endViews.editor
    else -> null
}

fun ImperfectState.alpha(content: Content) = when {
    isPrevious(content) -> startViews.alpha
    isCurrent(content) -> endViews.alpha
    else -> 1f
}

fun ImperfectState.alpha(editor: Editor?) = when {
    isPrevious(editor) -> startViews.alpha
    isCurrent(editor) -> endViews.alpha
    else -> 1f
}