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
import android.view.Window
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.LifecycleOwner
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.EditorAdapter
import com.xiaocydx.inputview.InputView

fun <C : Content, E : Editor> InputView.Companion.createOverlay(
    window: Window,
    lifecycleOwner: LifecycleOwner,
    contentAdapter: ContentAdapter<C>,
    editorAdapter: EditorAdapter<E>,
): Overlay<C, E> = OverlayImpl(
    window, lifecycleOwner,
    contentAdapter, editorAdapter
)

interface Overlay<C : Content, E : Editor> : TransformerOwner {

    val currentScene: Scene<C, E>?

    fun setSceneChangedListener(listener: SceneChangedListener<C, E>)

    fun setSceneEditorConverter(converter: SceneEditorConverter<C, E>)

    fun addToOnBackPressedDispatcher(dispatcher: OnBackPressedDispatcher)

    fun attachToWindow(
        initCompat: Boolean,
        rootParent: ViewGroup? = null,
        initializer: ((inputView: InputView) -> Unit)? = null
    ): Boolean

    fun go(scene: Scene<C, E>?): Boolean

    interface Transform {

        /**
         * 调用[TransformerOwner.addTransformer]添加[Transformer]：
         * 1. 当View附加到Window时，[Transformer]同步添加到[Overlay]。
         * 2. 当View从Window分离时，[Transformer]从[Overlay]同步移除。
         */
        fun View.transform() = viewTransform()
    }
}

fun interface SceneChangedListener<C : Content, E : Editor> {
    fun onChanged(previous: Scene<C, E>?, current: Scene<C, E>?)
}

fun interface SceneEditorConverter<C : Content, E : Editor> {
    fun nextScene(currentScene: Scene<C, E>?, nextEditor: E?): Scene<C, E>?
}