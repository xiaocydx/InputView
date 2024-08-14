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
 * [Overlay]的场景，用于[Overlay.go]
 *
 * 推荐用`enum class`或`sealed class`实现[Scene]，例如：
 * ```
 * enum class VideoScene(
 *     override val content: VideoContent,
 *     override val editor: VideoEditor
 * ) : Scene<VideoContent, VideoEditor> {
 *     INPUT_TEXT(VideoContent.Text, VideoEditor.Ime),
 *     INPUT_EMOJI(VideoContent.Text, VideoEditor.Emoji),
 *     SELECT_AUDIO(VideoContent.Title, VideoEditor.Audio),
 *     SELECT_IMAGE(VideoContent.Title, VideoEditor.Image)
 * }
 *
 * enum class VideoContent : Content {
 *     TEXT, TITLE
 * }
 *
 * enum class VideoEditor : Editor {
 *     IME, EMOJI, AUDIO, IMAGE
 * }
 * ```
 *
 * 用`enum class`或`sealed class`能更好的进行模式匹配。
 *
 * @author xcc
 * @date 2024/7/24
 */
interface Scene<C : Content, E : Editor> {

    /**
     * [Overlay]内容区的内容
     */
    val content: C

    /**
     * [Overlay]编辑区的编辑器
     */
    val editor: E
}