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

@Suppress("UNCHECKED_CAST")
internal fun <C : Content, E : Editor> defaultEditorConverter() = run {
    DefaultEditorConverter as SceneEditorConverter<C, E>
}

private object DefaultEditorConverter : SceneEditorConverter<Content, Editor> {
    override fun nextScene(currentScene: Scene<Content, Editor>?, nextEditor: Editor?) = run {
        if (nextEditor == null) null else currentScene
    }
}