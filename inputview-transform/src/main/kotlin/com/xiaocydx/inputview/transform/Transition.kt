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

@file:JvmName("TransformTransitionInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.transition

import android.graphics.Rect
import android.view.View

/**
 * [View.setLeftTopRightBottom]的兼容函数
 */
fun View.setLeftTopRightBottomCompat(rect: Rect) {
    setLeftTopRightBottomCompat(rect.left, rect.top, rect.right, rect.bottom)
}

/**
 * [View.setLeftTopRightBottom]的兼容函数
 */
fun View.setLeftTopRightBottomCompat(left: Int, top: Int, right: Int, bottom: Int) {
    ViewUtils.setLeftTopRightBottom(this, left, top, right, bottom)
}