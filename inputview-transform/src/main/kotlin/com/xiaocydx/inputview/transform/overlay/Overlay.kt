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

import android.app.Activity
import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.LifecycleOwner
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.EditorAdapter
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.InputView

/**
 * 构建跟[lifecycleOwner]关联的[Overlay]
 *
 * @param contentAdapter 用于[Overlay.go]通知更改[Content]
 * @param editorAdapter 用于[Overlay.go]通知更改[Editor]
 */
fun <C : Content, E : Editor> InputView.Companion.createOverlay(
    lifecycleOwner: LifecycleOwner,
    contentAdapter: ContentAdapter<C>,
    editorAdapter: EditorAdapter<E>,
): Overlay<C, E> = OverlayImpl(lifecycleOwner, contentAdapter, editorAdapter)

/**
 * 包含[InputView]的覆盖层，负责更改[Scene]和调度[Transformer]
 *
 * ### 视图层级
 * 1. 最顶层的是`rootView`，`rootView`添加到[attach]指定的`rootParent`。
 * 2. `rootView`的child顺序：`backgroundView`、`contentView`、`inputView`。
 * [Transformer]函数的`state`形参，其中View类型的属性即为`rootView`的child。
 *
 * ### [Transformer]
 * [Overlay]实现了[TransformerOwner]，能添加[Transformer]实现自定义变换操作。
 * [InputView.editorAnimator]是[Overlay]的动画执行器，参与调度[Transformer]。
 * 调用[go]会更改[Scene]，进而调度执行[Transformer]。
 */
interface Overlay<C : Content, E : Editor> : TransformerOwner {

    /**
     * 之前的[Scene]，调用[go]会更改[previous]
     */
    val previous: Scene<C, E>?

    /**
     * 当前的[Scene]，调用[go]会更改[current]
     */
    val current: Scene<C, E>?

    /**
     * 构建[Overlay]时关联的[lifecycleOwner]
     *
     * 当`Lifecycle.currentState`转换为[DESTROYED]时：
     * 1. 将[current]更改为`null`。
     * 2. 从`rootParent`移除`rootView`。
     */
    val lifecycleOwner: LifecycleOwner

    /**
     * [Scene]更改的监听
     */
    var sceneChangedListener: SceneChangedListener<C, E>?

    /**
     * 设置[Editor]到[Scene]的转换器
     */
    var sceneEditorConverter: SceneEditorConverter<C, E>

    /**
     * 初始化[Overlay]，并将`rootView`添加到[rootParent]
     *
     * @param window [Activity.getWindow]或[Dialog.getWindow]。
     * @param rootParent `rootView`的父级，传入`null`会将id为[ROOT_PARENT_ID]的View作为父级。
     * @param initializer [InputView]的初始化函数，当[InputView.editorAnimator]
     * 的类型为[FadeEditorAnimator]时，其计算的值会赋值给[TransformViews.alpha]。
     *
     * @return `true`-初始化成功，`false`-已初始化
     */
    fun attach(
        window: Window,
        rootParent: ViewGroup? = null,
        initializer: ((inputView: InputView) -> Unit)? = null
    ): Boolean

    /**
     * 将[current]更改为[scene]，若更改成功，则运行动画调度执行[Transformer]
     *
     * @return `true`-更改成功，`false`-未调用[attach]或没有改变
     */
    fun go(scene: Scene<C, E>?): Boolean

    /**
     * 将[Overlay]实现的[OnBackPressedCallback]，添加到[dispatcher]。
     * 当`lifecycleOwner.lifecycle.currentState`转换为[DESTROYED]时，
     * 从[dispatcher]移除添加的[OnBackPressedCallback]。
     *
     * 当进行回退时，若`current != null`，则将[current]更改为`null`。
     *
     * @return `true`-添加成功，`false`-已添加
     */
    fun addToOnBackPressedDispatcher(dispatcher: OnBackPressedDispatcher): Boolean

    interface Transform {

        /**
         * 调用[TransformerOwner.addTransformer]添加[Transformer]：
         * 1. 当View附加到Window时，[Transformer]同步添加到[Overlay]。
         * 2. 当View从Window分离时，[Transformer]从[Overlay]同步移除。
         */
        fun View.transform() = viewTransform()
    }

    companion object {
        const val ROOT_PARENT_ID = android.R.id.content
    }
}

fun interface SceneChangedListener<C : Content, E : Editor> {
    fun onChanged(previous: Scene<C, E>?, current: Scene<C, E>?)
}

fun interface SceneEditorConverter<C : Content, E : Editor> {
    fun nextScene(currentScene: Scene<C, E>?, nextEditor: E?): Scene<C, E>?
}