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

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.xiaocydx.inputview.transform

import android.graphics.Rect
import android.view.View
import android.widget.FrameLayout
import com.xiaocydx.inputview.updatePadding

/**
 * 获取View的`left`、`top`、`right`、`bottom`
 *
 * @param change 获取完边界后，修改数值
 */
inline fun View.getBounds(bounds: Rect, change: Rect.() -> Unit = {}) {
    bounds.set(left, top, right, bottom)
    bounds.change()
}

/**
 * 获取View的`paddings`
 *
 * @param change 获取完`paddings`后，修改数值
 */
inline fun View.getPaddings(paddings: Rect, change: Rect.() -> Unit = {}) {
    paddings.set(paddingLeft, paddingTop, paddingRight, paddingBottom)
    paddings.change()
}

/**
 * 更新View的`paddings`
 */
fun View.updatePaddings(paddings: Rect) {
    paddings.apply { updatePadding(left, top, right, bottom) }
}

/**
 * 更新View的布局位置，当`layoutParams`类型为[FrameLayout.LayoutParams]时有效
 */
fun View.updateLayoutGravity(gravity: Int) {
    val lp = layoutParams as? FrameLayout.LayoutParams
    if (lp == null || lp.gravity == gravity) return
    lp.gravity = gravity
    layoutParams = lp
}

/**
 * View在Window中的位置
 */
data class ViewLocation(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) {
    val width = right - left
    val height = bottom - top

    constructor(rect: Rect) : this(
        left = rect.left, top = rect.top,
        right = rect.right, bottom = rect.bottom
    )

    companion object {
        fun from(view: View) = run {
            val out = IntArray(2)
            view.getLocationInWindow(out)
            ViewLocation(
                left = out[0],
                top = out[1],
                right = out[0] + view.width,
                bottom = out[1] + view.height
            )
        }
    }
}

fun ViewLocation.toRect() = Rect(left, top, right, bottom)

fun Rect.set(location: ViewLocation) {
    location.apply { set(left, top, right, bottom) }
}