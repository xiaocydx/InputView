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
import com.xiaocydx.inputview.EditorAnimator
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.InputView

/**
 * 构建跟[lifecycleOwner]关联的[Overlay]
 *
 * @param contentAdapter 用于[Overlay.go]通知更改[Content]
 * @param editorAdapter  用于[Overlay.go]通知更改[Editor],
 * @param editorAnimator [Overlay]的动画执行器，参与调度[Transformer]。
 * 当类型为[FadeEditorAnimator]时，其计算值会赋值给[TransformViews.alpha]。
 */
inline fun <S, C, E> InputView.Companion.createOverlay(
    lifecycleOwner: LifecycleOwner,
    contentAdapter: ContentAdapter<C>,
    editorAdapter: EditorAdapter<E>,
    editorAnimator: EditorAnimator = FadeEditorAnimator(),
    initializer: Overlay<S>.() -> Unit = {}
): Overlay<S> where S : Scene<C, E>, C : Content, E : Editor {
    val overlay: Overlay<S> = OverlayImpl(
        lifecycleOwner, contentAdapter,
        editorAdapter, editorAnimator
    )
    return overlay.apply(initializer)
}

/**
 * 包含[InputView]的覆盖层，负责更改[Scene]和调度[Transformer]
 *
 * ### 视图层级
 * 1. 最顶层的是`rootView`，`rootView`添加到[attach]指定的`rootParent`。
 * 2. `rootView`的child顺序：`backgroundView`、`contentView`、`inputView`。
 * [Transformer]函数的`state`形参，其中View类型的属性即为`rootView`的child。
 *
 * ### [Transformer]
 * [Overlay]实现了[TransformerOwner]，能添加[Transformer]实现变换操作。
 * 构建[Overlay]的`editorAnimator`是动画执行器，参与调度[Transformer]。
 * 调用[go]会更改[Scene]，进而调度执行[Transformer]。
 */
interface Overlay<S : Scene<*, *>> : TransformerOwner {

    /**
     * 之前的[Scene]，调用[go]会更改[previous]
     */
    val previous: S?

    /**
     * 当前的[Scene]，调用[go]会更改[current]
     */
    val current: S?

    /**
     * 构建[Overlay]时关联的[lifecycleOwner]
     *
     * 当`Lifecycle.currentState`转换为[DESTROYED]时：
     * 1. 将[current]更改为`null`。
     * 2. 从`rootParent`移除`rootView`。
     */
    val lifecycleOwner: LifecycleOwner

    /**
     * [Scene]更改的监听，调用[go]更改成功，会触发该监听
     */
    var sceneChangedListener: SceneChangedListener<S>?

    /**
     * `nextEditor`转换为[Scene]的转换器
     *
     * 当未调用[go]通知更改[Editor]时，会执行转换器将`nextEditor`转换为[Scene]。
     * 转换器主要用于未调用[go]通知显示和隐藏IME的情况，比如点击EditText显示IME、
     * 点击隐藏或手势回退IME。
     */
    var sceneEditorConverter: SceneEditorConverter<S>

    /**
     * 初始化[Overlay]，并将`rootView`添加到[rootParent]
     *
     * @param window     [Activity.getWindow]或[Dialog.getWindow]。
     * @param rootParent `rootView`的父级，传入`null`会将id为[ROOT_PARENT_ID]的View作为父级。
     *
     * @return `true`-初始化成功，`false`-已初始化
     */
    fun attach(window: Window, rootParent: ViewGroup? = null): Boolean

    /**
     * 将[current]更改为[scene]，若更改成功，则运行动画调度执行[Transformer]
     *
     * @return `true`-更改成功，`false`-未调用[attach]或没有改变
     */
    fun go(scene: S?): Boolean

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

    /**
     * 提供扩展函数[transform]
     */
    interface Transform {

        /**
         * 调用[TransformerOwner.add]添加[Transformer]：
         * 1. 当View附加到Window时，[Transformer]同步添加到[Overlay]。
         * 2. 当View从Window分离时，从[Overlay]同步移除[Transformer]。
         */
        fun View.transform(): TransformerOwner = viewTransform()
    }

    companion object {
        internal const val ROOT_PARENT_ID = android.R.id.content
    }
}

/**
 * [Scene]更改的监听，调用[Overlay.go]更改成功，会触发该监听
 */
fun interface SceneChangedListener<S : Scene<*, *>> {

    /**
     * [Scene]已更改
     *
     * @param previous 之前的[Scene]
     * @param current  当前的[Scene]
     */
    fun onChanged(previous: S?, current: S?)
}

/**
 * `nextEditor`转换为[Scene]的转换器
 *
 * 当未调用[Overlay.go]通知更改[Editor]时，会执行转换器将`nextEditor`转换为[Scene]。
 * 转换器主要用于未调用[Overlay.go]通知显示和隐藏IME的情况，比如点击EditText显示IME、
 * 点击隐藏或手势回退IME。
 */
fun interface SceneEditorConverter<S : Scene<*, *>> {

    /**
     * [nextEditor]转换为下一个[Scene]
     *
     * @param previous   之前的[Scene]
     * @param current    当前的[Scene]
     * @param nextEditor 下一个[Editor]
     */
    fun nextScene(previous: S?, current: S?, nextEditor: Editor?): S?

    companion object {
        private val default = SceneEditorConverter<Scene<*, *>> { _, c, n -> if (n == null) null else c }

        @Suppress("UNCHECKED_CAST")
        internal fun <S : Scene<*, *>> default() = run { default as SceneEditorConverter<S> }
    }
}