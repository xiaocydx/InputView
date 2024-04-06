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

package com.xiaocydx.inputview

import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import androidx.annotation.IntRange
import androidx.annotation.VisibleForTesting
import androidx.core.view.OneShotPreDrawListener
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat

/**
 * [Editor]的宿主，作用是让内部实现不用区分[InputView]和[EditorContainer]
 *
 * @author xcc
 * @date 2023/1/19
 */
internal interface EditorHost {

    /**
     * IME偏移
     */
    val WindowInsetsCompat.imeOffset: Int

    /**
     * Window是否具有焦点
     */
    val hasWindowFocus: Boolean

    /**
     * 编辑区的偏移值
     */
    @get:IntRange(from = 0)
    val editorOffset: Int

    /**
     * 导航栏的偏移值，若不支持手势导航栏EdgeToEdge，则该属性值为0
     */
    @get:IntRange(from = 0)
    val navBarOffset: Int

    /**
     * 表示IME的[Editor]
     */
    val ime: Editor?

    /**
     * 当前显示的[Editor]
     */
    val current: Editor?

    /**
     * [Editor]的视图容器
     */
    val container: ViewGroup?

    /**
     * 更改[Editor]后，之前[Editor]的视图
     */
    val previousView: View?

    /**
     * 更改[Editor]后，当前[Editor]的视图
     */
    val currentView: View?

    /**
     * 移除[Editor]的视图
     */
    fun removeEditorView(view: View)

    /**
     * 更新编辑区的偏移值
     */
    fun updateEditorOffset(offset: Int)

    /**
     * 分发IME的显示情况，仅在`onApplyWindowInsets()`下调用
     */
    fun dispatchImeShown(shown: Boolean): Boolean

    /**
     * 显示[Editor]
     */
    fun showChecked(editor: Editor): Boolean

    /**
     * 隐藏[Editor]
     */
    fun hideChecked(editor: Editor): Boolean

    /**
     * 添加[AnimationCallback]
     */
    fun addAnimationCallback(callback: AnimationCallback)

    /**
     * 移除[AnimationCallback]
     */
    fun removeAnimationCallback(callback: AnimationCallback)

    /**
     * 是否包含[AnimationCallback]
     */
    @VisibleForTesting
    fun containsAnimationCallback(callback: AnimationCallback): Boolean

    /**
     * 添加绘制帧draw之前的任务，[OneShotPreDrawListener.removeListener]移除任务
     */
    fun addPreDrawAction(action: () -> Unit): OneShotPreDrawListener

    /**
     * 添加[OnApplyWindowInsetsListenerCompat]，`null`表示移除
     *
     * @param listener 必须返回传入的[WindowInsetsCompat]，不可做消费处理
     */
    fun setOnApplyWindowInsetsListener(listener: OnApplyWindowInsetsListenerCompat?)

    /**
     * 添加[WindowInsetsAnimationCompat.Callback]，`null`表示移除
     *
     * @param durationMillis Android 11及以上IME动画的时长
     * @param interpolator   Android 11及以上IME动画的插值器
     */
    fun setWindowInsetsAnimationCallback(
        durationMillis: Long,
        interpolator: Interpolator,
        callback: WindowInsetsAnimationCompat.Callback?
    )
}

internal const val NO_VALUE = -1

internal interface Replicable

internal interface ReplicableAnimationCallback : AnimationCallback, Replicable

internal typealias OnApplyWindowInsetsListenerCompat = androidx.core.view.OnApplyWindowInsetsListener