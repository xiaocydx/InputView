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

package com.xiaocydx.inputview.sample.scene.transform

import android.view.ViewGroup
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import com.xiaocydx.inputview.AnimationState
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformation.State
import kotlinx.coroutines.CoroutineScope

/**
 * 覆盖层变换动画，由[OverlayTransformationEnforcer]完成调度
 *
 * @author xcc
 * @date 2024/4/10
 */
@Deprecated(
    message = "实现类的职责不够清晰，调度流程不够完善",
    replaceWith = ReplaceWith("待替换为InputView.createOverlay()")
)
interface OverlayTransformation<in S : State> {

    /**
     * 动画准备时被调用，此时可以初始化View的状态，确保动画的首帧内容正确绘制
     */
    fun prepare(state: S) = Unit

    /**
     * 动画开始时被调用，此时[state]的`anchorY`属性可用，可以计算动画的起始值和结束值，
     * 该函数在布局完成之后，Draw之前被调用，因此可以做一些不需要重新测量和布局的操作。
     */
    fun start(state: S) = Unit

    /**
     * 动画运行时被调用，此时[state]的全部属性可用，可以结合[start]计算的数值更新View的属性
     */
    fun update(state: S) = Unit

    /**
     * 动画结束时被调用，此时[state]的全部属性可用，可以做一些移除操作
     */
    fun end(state: S) = Unit

    /**
     * 动画结束后被调用，此时[state]的全部属性可用，可以用[scope]启动协程，完成状态的收集工作，
     * [scope]的上下文包含`Dispatchers.Main.immediate`，当再次开始动画时，启动的协程会被取消。
     *
     * 调用[EnforcerScope.requestDispatch]，传入修改后的[state]，
     * 会按[prepare]、[start]、[update]、[end]的顺序进行分发调用。
     */
    fun launch(state: S, scope: EnforcerScope) = Unit

    /**
     * [State]的提供者，可继承[State]实现更多的属性，例如[ContainerState]
     */
    fun interface StateProvider<S : State> {
        fun createState(): S
    }

    /**
     * 执行[OverlayTransformation]的作用域
     */
    interface EnforcerScope : CoroutineScope {

        /**
         * 修改[state]的属性后，可调用该函数，请求分发[state]，
         * 若[state]跟[launch]传入的`state`不一致，则请求无效。
         */
        fun requestDispatch(state: State)
    }

    /**
     * 动画状态，各属性通过[InputView]和[AnimationState]的属性映射而来
     */
    open class State(val inputView: InputView) {
        /**
         * 动画起始[Editor]
         */
        var previous: Editor? = null; private set

        /**
         * 动画结束[Editor]
         */
        var current: Editor? = null; private set

        /**
         * 相对于Window的初始锚点Y
         */
        @get:IntRange(from = 0)
        var initialAnchorY = 0; private set

        /**
         * 相对于Window的动画起始锚点Y
         */
        @get:IntRange(from = 0)
        var startAnchorY = 0; private set

        /**
         * 相对于Window的动画结束锚点Y
         */
        @get:IntRange(from = 0)
        var endAnchorY = 0; private set

        /**
         * 对于Window的动画当前锚点Y
         */
        @get:IntRange(from = 0)
        var currentAnchorY = 0; private set

        /**
         * `startView.alpha`，可用于两个非`null`[Editor]之间的变换
         */
        @get:FloatRange(from = 0.0, to = 1.0)
        var startViewAlpha = 1f; private set

        /**
         * `endView.alpha`，可用于两个非`null`[Editor]之间的变换
         */
        @get:FloatRange(from = 0.0, to = 1.0)
        var endViewAlpha = 1f; private set

        /**
         * 动画起始状态和结束状态之间的原始分数进度
         */
        @get:FloatRange(from = 0.0, to = 1.0)
        var animatedFraction = 0f; private set

        /**
         * 动画起始状态和结束状态之间的插值器分数进度
         */
        @get:FloatRange(from = 0.0, to = 1.0)
        var interpolatedFraction = 0f; private set

        fun setEditor(previous: Editor?, current: Editor?) {
            this.previous = previous
            this.current = current
        }

        fun setAnchorY(initial: Int, start: Int, end: Int) {
            initialAnchorY = initial
            startAnchorY = start
            endAnchorY = end
            currentAnchorY = start
        }

        fun setViewAlpha(start: Float, end: Float) {
            startViewAlpha = start
            endViewAlpha = end
        }

        fun setFraction(animated: Float, interpolated: Float) {
            animatedFraction = animated
            interpolatedFraction = interpolated
            currentAnchorY = startAnchorY + ((endAnchorY - startAnchorY) * interpolated).toInt()
        }
    }

    open class ContainerState(inputView: InputView, val container: ViewGroup) : State(inputView)
}