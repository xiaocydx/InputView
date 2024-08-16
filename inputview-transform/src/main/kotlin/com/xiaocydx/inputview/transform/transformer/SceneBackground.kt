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

import android.animation.RectEvaluator
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.annotation.ColorInt
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.InputView

/**
 * 添加[ContentBackground]和[EditorBackground]，配置统一的[color]
 *
 * @param contentMatch [ContentBackground]的匹配条件
 * @param editorMatch  [EditorBackground]的匹配条件
 */
fun TransformerOwner.addSceneBackground(
    @ColorInt color: Int,
    contentMatch: ContentMatch? = null,
    editorMatch: EditorMatch? = null,
) {
    add(ContentBackground(color).apply { setMatch(contentMatch) })
    add(EditorBackground(color).apply { setMatch(editorMatch) })
}

/**
 * [Content]的背景
 *
 * ### 匹配变换
 * 当匹配的[Content]视图不为`null`时，才进行变换。可调用[setMatch]设置匹配条件。
 *
 * ### 变换效果
 * 围绕匹配的[Content]视图绘制背景，变换期间更改背景高度。
 *
 * ### 适用场景
 * 1. 匹配的[Content]，其视图高度为固定值或[WRAP_CONTENT]。
 * 2. 可搭配[ContentChangeBounds]、[ContentChangeTranslation]使用。
 */
class ContentBackground(private val drawable: Drawable) : ContentTransformer() {
    private val startBounds = Rect()
    private val endBounds = Rect()
    private val currentBounds = Rect()
    private val evaluator = RectEvaluator(currentBounds)
    private var endViewHeight = 0

    constructor(
        @ColorInt color: Int
    ) : this(ColorDrawable(color))

    constructor(
        @ColorInt color: Int,
        matchContent: Content
    ) : this(color) {
        setMatchContent(matchContent)
    }

    constructor(
        @ColorInt color: Int,
        match: ContentMatch
    ) : this(color) {
        setMatch(match)
    }

    override fun onMatch(state: ImperfectState) = with(state) {
        startView() != null || endView() != null
    }

    override fun onPrepare(state: ImperfectState) = with(state) {
        backgroundView.overlay.remove(drawable)
        backgroundView.overlay.add(drawable)
    }

    override fun onStart(state: TransformState): Unit = with(state) {
        startBounds.setEmpty()
        endBounds.setEmpty()
        startView()?.getBounds(startBounds)
        endView()?.getBounds(endBounds)
    }

    override fun onUpdate(state: TransformState): Unit = with(state) {
        startView()?.appendTranslation(startBounds)
        endView()?.appendTranslation(endBounds)
        currentBounds.setEmpty()
        when {
            startBounds == endBounds -> currentBounds.set(startBounds)
            startBounds.isEmpty && !endBounds.isEmpty -> currentBounds.set(endBounds)
            !startBounds.isEmpty && endBounds.isEmpty -> currentBounds.set(startBounds)
            !startBounds.isEmpty && !endBounds.isEmpty -> {
                evaluator.evaluate(interpolatedFraction, startBounds, endBounds)
            }
        }
        drawable.bounds = currentBounds
        startView()?.removeTranslation(startBounds)
        endView()?.removeTranslation(endBounds)
    }

    override fun onEnd(state: TransformState) = with(state) {
        endViewHeight = endView()?.height ?: 0
        if (endView() == null) backgroundView.overlay.remove(drawable)
    }

    override fun onPreDraw(state: TransformState) = with(state) {
        val height = endView()?.height ?: 0
        if (endViewHeight != height) requestTransform()
    }

    private fun View.appendTranslation(rect: Rect) {
        rect.offset(0, translationY.toInt())
    }

    private fun View.removeTranslation(rect: Rect) {
        rect.offset(0, -translationY.toInt())
    }
}

/**
 * [Editor]的背景
 *
 * ### 匹配变换
 * 当匹配的[Editor]视图不为`null`时，才进行变换。可调用[setMatch]设置匹配条件。
 *
 * ### 变换效果
 * 围绕匹配的[Editor]视图绘制背景，变换期间更改背景高度。
 *
 * ### 适用场景
 * 用于代替[InputView.editorBackground]，跟[ContentBackground]统一使用方式。
 */
class EditorBackground(private val drawable: Drawable) : EditorTransformer() {

    constructor(
        @ColorInt color: Int
    ) : this(ColorDrawable(color))

    constructor(
        @ColorInt color: Int,
        matchEditor: Editor
    ) : this(color) {
        setMatchEditor(matchEditor)
    }

    constructor(
        @ColorInt color: Int,
        match: EditorMatch
    ) : this(color) {
        setMatch(match)
    }

    override fun onMatch(state: ImperfectState) = with(state) {
        startView() != null || endView() != null
    }

    override fun onPrepare(state: ImperfectState) = with(state) {
        backgroundView.overlay.remove(drawable)
        backgroundView.overlay.add(drawable)
    }

    override fun onUpdate(state: TransformState) = with(state) {
        val top = contentView.bottom - currentOffset
        drawable.setBounds(contentView.left, top, contentView.right, contentView.bottom)
    }

    override fun onEnd(state: TransformState) = with(state) {
        if (endView() == null) backgroundView.overlay.remove(drawable)
    }
}