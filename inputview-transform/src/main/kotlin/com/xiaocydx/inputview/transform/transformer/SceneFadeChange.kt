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

import com.xiaocydx.inputview.Editor

/**
 * 添加[ContentFadeChange]和[EditorFadeChange]
 *
 * @param contentMatch [ContentFadeChange]的匹配条件
 * @param editorMatch  [EditorFadeChange]的匹配条件
 */
fun TransformerOwner.addSceneFadeChange(
    contentMatch: ContentMatch? = null,
    editorMatch: EditorMatch? = null,
) {
    add(ContentFadeChange().apply { setMatch(contentMatch) })
    add(EditorFadeChange().apply { setMatch(editorMatch) })
}

/**
 * [Content]的透明度变换
 *
 * ### 匹配变换
 * 当`state`的前后[Content]不同时，才进行变换。可调用[setMatch]设置匹配条件。
 *
 * ### 变换效果
 * 1. `state.previous`和`state.current`其中一个为null（表示进入和退出），将匹配的[Content]视图透明度设为`1f`。
 * 2. `state.previous`和`state.current`都不为null，将匹配的[Content]视图透明度设为[TransformViews.alpha]。
 */
class ContentFadeChange() : ContentTransformer() {

    constructor(match: ContentMatch) : this() {
        setMatch(match)
    }

    override fun onMatch(state: ImperfectState) = with(state) {
        previous?.content !== current?.content
    }

    override fun onUpdate(state: TransformState) = with(state) {
        if (previous == null || current == null) {
            startView()?.alpha = 1f
            endView()?.alpha = 1f
        } else {
            startView()?.alpha = startViews.alpha
            endView()?.alpha = endViews.alpha
        }
    }

    override fun onEnd(state: TransformState) = with(state) {
        startView()?.alpha = 1f
        endView()?.alpha = 1f
    }
}

/**
 * [Editor]的透明度变换
 *
 * ### 匹配变换
 * 当`state`的前后[Editor]不同时，才进行变换。可调用[setMatch]设置匹配条件。
 *
 * ### 变换效果
 * 1. `state.previous`和`state.current`其中一个为null（表示进入和退出），将匹配的[Editor]视图透明度设为`1f`。
 * 2. `state.previous`和`state.current`都不为null，将匹配的[Editor]视图透明度设为[TransformViews.alpha]。
 * 3. `startView`和`endView`其中一个为`null`，根据`state.animatedFraction`计算透明度。
 *
 * 第3点是[EditorFadeChange]跟[ContentFadeChange]的区别，目的是实现更好的变换效果。
 */
class EditorFadeChange() : EditorTransformer() {

    constructor(match: EditorMatch) : this() {
        setMatch(match)
    }

    override fun onMatch(state: ImperfectState) = with(state) {
        previous?.editor !== current?.editor
    }

    override fun onUpdate(state: TransformState) = with(state) {
        val startView = startView()
        val endView = endView()
        when {
            previous == null || current == null -> {
                startView()?.alpha = 1f
                endView()?.alpha = 1f
            }
            startView == null && endView != null -> {
                endView.alpha = animatedFraction
            }
            startView != null && endView == null -> {
                startView.alpha = 1f - animatedFraction
            }
            else -> {
                startView?.alpha = startViews.alpha
                endView?.alpha = endViews.alpha
            }
        }
    }

    override fun onEnd(state: TransformState) = with(state) {
        startView()?.alpha = 1f
        endView()?.alpha = 1f
    }
}