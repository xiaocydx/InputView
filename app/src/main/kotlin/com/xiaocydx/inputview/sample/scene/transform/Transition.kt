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

@file:JvmName("TransitionInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.transition

import android.graphics.Rect
import android.view.View
import androidx.core.view.updatePadding

inline fun View.getBounds(bounds: Rect, change: Rect.() -> Unit = {}) {
    bounds.set(left, top, right, bottom)
    bounds.change()
}

inline fun View.getPaddings(paddings: Rect, change: Rect.() -> Unit = {}) {
    paddings.set(paddingLeft, paddingTop, paddingRight, paddingBottom)
    paddings.change()
}

fun View.setLeftTopRightBottomCompat(rect: Rect) {
    setLeftTopRightBottomCompat(rect.left, rect.top, rect.right, rect.bottom)
}

fun View.setLeftTopRightBottomCompat(left: Int, top: Int, right: Int, bottom: Int) {
    ViewUtils.setLeftTopRightBottom(this, left, top, right, bottom)
}

fun View.updatePaddings(paddings: Rect) {
    paddings.apply { updatePadding(left, top, right, bottom) }
}