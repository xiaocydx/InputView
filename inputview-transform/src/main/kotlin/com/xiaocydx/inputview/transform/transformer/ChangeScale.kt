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
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.xiaocydx.inputview.Editor
import java.lang.Integer.min
import java.lang.ref.WeakReference

/**
 * 对`target`进行缩放变换
 *
 * ### 匹配变换
 * 当[contentMatch]和[editorMatch]都匹配时，才对`target`进行变换。
 * 若[contentMatch]和[editorMatch]都为`null`，则表示匹配所有情况。
 *
 * ### 变换效果
 * 取[Content]和[Editor]之间最小顶部Y作为锚点：
 * 1. 上升平移过程，`target`依赖锚点进行缩小。
 * 2. 下降平移过程，`target`依赖锚点进行放大。

 * 比如，[Content]的视图是布局在[Editor]之上的标题栏，
 * 标题栏顶部Y比[Editor]顶部Y小，将标题栏顶部Y作为锚点。
 *
 * ### 适用场景
 * 1. 匹配的[Content]，其视图高度为固定值或[WRAP_CONTENT]。
 * 2. 可搭配[ContentChangeBounds]、[ContentChangeTranslation]使用。
 *
 * @author xcc
 * @date 2024/7/25
 */
class ChangeScale(
    target: View,
    private val contentMatch: ContentMatch? = null,
    private val editorMatch: EditorMatch? = null
) : Transformer() {
    private val ref = WeakReference(target)
    private val point = IntArray(2)
    private var targetBottom = 0
    private var inputViewBottom = 0
    private var endViewHeight = 0
    override val sequence = SEQUENCE_LAST

    override fun match(state: ImperfectState): Boolean {
        var matchStart = contentMatch?.match(start = true, state.previous?.content) ?: true
        var matchEnd = contentMatch?.match(start = false, state.current?.content) ?: true
        if (!matchStart && !matchEnd) return false
        matchStart = editorMatch?.match(start = true, state.previous?.editor) ?: true
        matchEnd = editorMatch?.match(start = false, state.current?.editor) ?: true
        return (matchStart || matchEnd) && ref.get() != null
    }

    override fun onStart(state: TransformState) {
        val target = ref.get() ?: return
        target.getLocationInWindow(point)
        targetBottom = point[1] + target.height
        state.inputView.getLocationInWindow(point)
        inputViewBottom = point[1] + state.inputView.height
    }

    override fun onUpdate(state: TransformState) {
        val target = ref.get() ?: return
        val anchor = state.calculateAnchor()
        val dy = (targetBottom - anchor).coerceAtLeast(0)
        val scale = 1f - dy.toFloat() / target.height
        target.apply {
            scaleX = scale
            scaleY = scale
            pivotX = target.width.toFloat() / 2
            pivotY = 0f
        }
    }

    override fun onEnd(state: TransformState) = with(state) {
        endViewHeight = endViews.content?.height ?: 0
    }

    override fun onPreDraw(state: TransformState) = with(state) {
        val height = endViews.content?.height ?: 0
        if (endViewHeight != height) requestTransform()
    }

    private fun TransformState.calculateAnchor(): Int {
        val editorTop = inputViewBottom - currentOffset

        point[1] = Int.MAX_VALUE
        startViews.content?.getLocationInWindow(point)
        val startContentTop = point[1]

        var endContentTop = startContentTop
        if (startViews.content != endViews.content) {
            point[1] = Int.MAX_VALUE
            endViews.content?.getLocationInWindow(point)
            endContentTop = point[1]
        }

        val contentTop = min(startContentTop, endContentTop)
        return min(contentTop, editorTop)
    }
}