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
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.EditText
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import com.xiaocydx.inputview.AnimationState
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.EditorAdapter
import com.xiaocydx.inputview.EditorAnimator
import com.xiaocydx.inputview.EditorMode
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.transform.Transformer.Companion.SEQUENCE_FIRST
import com.xiaocydx.inputview.transform.Transformer.Companion.SEQUENCE_LAST

/**
 * [Overlay]的变换操作
 *
 * ### 添加和移除
 * 1. 调用[Overlay.add]和[Overlay.remove]。
 * 2. 实现[Overlay.Transform]，调用`view.transform.add()`和`view.transform.remove()`。
 *
 * 通过第2种方式添加的[Transformer]，其生命周期跟View一致，并且：
 * 1. 当View附加到Window时，[Transformer]同步添加到[Overlay]。
 * 2. 当View从Window分离时，从[Overlay]同步移除[Transformer]。
 *
 * ### [match]
 * 实现类需要重写[match]，根据`state`提供的属性决定是否让[Transformer]执行变换操作。
 * 可以调用[ImperfectState.isPrevious]、[ImperfectState.isCurrent]等扩展简化代码。
 *
 * ### [sequence]
 * 1. [Overlay]按[Transformer]的添加顺序分发调用[match]。
 * 2. [Overlay]按[sequence]从小到大的顺序分发调用[onPrepare]、[onStart]、[onUpdate]、[onEnd]、[onPreDraw]。
 *
 * [sequence]默认为[SEQUENCE_FIRST]，若[Transformer]的实现类需要在最后执行变换操作，
 * 则将[sequence]重写为[SEQUENCE_LAST]。例如[ChangeScale]在最后计算`target`的锚点。
 *
 * ### [onPreDraw]
 * [onEnd]完成后，在每一帧绘制之前，[onPreDraw]都会被调用。[onPreDraw]主要用于检查状态是否发生改变，
 * 改变则调用[requestTransform]请求重新执行变换操作。例如[ContentBackground]在[onEnd]记录View尺寸，
 * 在[onPreDraw]检查View尺寸是否改变，改变则调用[requestTransform]重新计算背景范围。
 *
 * ### [requestTransform]
 * [onEnd]完成后，可根据需求调用[requestTransform]请求重新执行变换操作。
 * 导航栏模式更改不需要调用[requestTransform]，[Overlay]会重新分发调用。
 *
 * @author xcc
 * @date 2024/7/24
 */
abstract class Transformer {
    internal var owner: TransformerOwner? = null
        private set

    /**
     * [Transformer]的变换操作分发顺序
     */
    open val sequence = SEQUENCE_FIRST

    /**
     * [Transformer]的匹配条件
     *
     * @return 返回`true`才会调用[onPrepare]、[onStart]、[onUpdate]、[onEnd]、[onPreDraw]。
     */
    abstract fun match(state: ImperfectState): Boolean

    /**
     * [Transformer]准备
     *
     * 该函数在布局之前被调用，此时可以修改View布局相关的属性。
     */
    open fun onPrepare(state: ImperfectState) = Unit

    /**
     * [Transformer]开始
     *
     * 该函数在`preDraw`阶段调用，此时可以获取View的尺寸以及对View做变换处理。
     */
    open fun onStart(state: TransformState) = Unit

    /**
     * [Transformer]更新
     *
     * 可以在该函数下更改View的属性，例如`paddings`、`margins`、变换属性。
     */
    open fun onUpdate(state: TransformState) = Unit

    /**
     * [Transformer]结束
     *
     * **注意**：在结束时，应当将View恢复为初始状态。
     */
    open fun onEnd(state: TransformState) = Unit

    /**
     * [onEnd]完成后，在每一帧绘制之前，[onPreDraw]都会被调用
     */
    open fun onPreDraw(state: TransformState) = Unit

    /**
     * 请求重新调用[onPrepare]、[onStart]、[onUpdate]、[onEnd]
     *
     * **注意**：当[match]返回`true`时，该函数才会有效。
     */
    fun requestTransform() {
        owner?.requestTransform(this)
    }

    internal fun onAttachedToOwner(owner: TransformerOwner) {
        check(this.owner == null) { "Transformer已关联TransformerOwner" }
        this.owner = owner
    }

    internal fun onDetachedFromOwner(owner: TransformerOwner) {
        if (this.owner !== owner) return
        this.owner = null
    }

    companion object {
        const val SEQUENCE_FIRST = Int.MIN_VALUE
        const val SEQUENCE_LAST = Int.MAX_VALUE
    }
}

/**
 * [Transformer]的不完整状态，用于[Transformer.match]和[Transformer.onPrepare]
 */
interface ImperfectState {

    /**
     * [Overlay]的`rootView`，其child顺序：`backgroundView`、`contentView`、`inputView`
     */
    val rootView: View

    /**
     * 背景View，宽高默认为[MATCH_PARENT]
     *
     * 可调用[View.setBackground]设置背景，或[View.getOverlay]添加背景Drawable
     */
    val backgroundView: View

    /**
     * [Content]视图的容器，宽高默认为[MATCH_PARENT]
     */
    val contentView: View

    /**
     * [Editor]视图的容器，宽高默认为[MATCH_PARENT]
     *
     * 可设置[InputView.editText]，例如[ContentChangeEditText]动态设置[Content]视图中的[EditText]。
     * **注意**：不允许设置[EditorMode]、[EditorAdapter]、[EditorAnimator]，这会导致[Overlay]出现调度异常。
     */
    val inputView: InputView

    /**
     * 之前的[Scene]
     */
    val previous: Scene<*, *>?

    /**
     * 当前的[Scene]
     */
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

    /**
     * 通过[FadeEditorAnimator]计算的alpha，默认不对[content]和[editor]设置该属性
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    val alpha: Float
}

/**
 * 是否为之前的[Scene]
 */
fun ImperfectState.isPrevious(scene: Scene<*, *>?) = previous === scene

/**
 * 是否为之前的[Content]
 */
fun ImperfectState.isPrevious(content: Content?) = previous?.content === content

/**
 * 是否为之前的[Editor]
 */
fun ImperfectState.isPrevious(editor: Editor?) = previous?.editor === editor

/**
 * 是否为当前的[Scene]
 */
fun ImperfectState.isCurrent(scene: Scene<*, *>?) = current === scene

/**
 * 是否为当前的[Content]
 */
fun ImperfectState.isCurrent(content: Content?) = current?.content === content

/**
 * 是否为当前的[Editor]
 */
fun ImperfectState.isCurrent(editor: Editor?) = current?.editor === editor

/**
 * 是否为进入的[Scene]
 */
fun ImperfectState.isEnter(scene: Scene<*, *>?) = previous == null && isCurrent(scene)

/**
 * 是否为进入的[Content]
 */
fun ImperfectState.isEnter(content: Content?) = previous == null && isCurrent(content)

/**
 * 是否为进入的[Editor]
 */
fun ImperfectState.isEnter(editor: Editor?) = previous == null && isCurrent(editor)

/**
 * 是否为退出的[Scene]
 */
fun ImperfectState.isReturn(scene: Scene<*, *>?) = isPrevious(scene) && current == null

/**
 * 是否为退出的[Content]
 */
fun ImperfectState.isReturn(content: Content?) = isPrevious(content) && current == null

/**
 * 是否为退出的[Editor]
 */
fun ImperfectState.isReturn(editor: Editor?) = isPrevious(editor) && current == null

/**
 * 从`startViews`或`endViews`获取匹配[content]的View，获取不到返回`null`
 */
fun ImperfectState.view(content: Content) = when {
    isPrevious(content) -> startViews.content
    isCurrent(content) -> endViews.content
    else -> null
}

/**
 * 从`startViews`或`endViews`获取匹配[editor]的View，获取不到返回`null`
 */
fun ImperfectState.view(editor: Editor?) = when {
    isPrevious(editor) -> startViews.editor
    isCurrent(editor) -> endViews.editor
    else -> null
}