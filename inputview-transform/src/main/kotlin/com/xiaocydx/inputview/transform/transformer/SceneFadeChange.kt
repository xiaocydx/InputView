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

fun TransformerOwner.addSceneFadeChange(
    contentMatch: ContentMatch? = null,
    editorMatch: EditorMatch? = null,
) {
    addTransformer(ContentFadeChange().apply { setMatch(contentMatch) })
    addTransformer(EditorFadeChange().apply { setMatch(editorMatch) })
}

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