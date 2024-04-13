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

package com.xiaocydx.inputview.sample.scene.transform

import android.graphics.Rect
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewTreeObserver
import androidx.transition.getBounds
import androidx.transition.setLeftTopRightBottomCompat
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformation.ContainerState
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformation.EnforcerScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

/**
 * 通用的容器边界变换动画，处理容器高度和锚点Y的变更
 *
 * @author xcc
 * @date 2024/4/12
 */
class BoundsTransformation : OverlayTransformation<ContainerState> {
    private val previousBounds = Rect()
    private val currentBounds = Rect()
    private var canTransform = false
    private var previousHeight = 0
    private var currentHeight = 0
    private var translationY = 0

    override fun prepare(state: ContainerState) = with(state) {
        require(container.layoutParams?.height == WRAP_CONTENT)
        container.getBounds(previousBounds)
    }

    override fun start(state: ContainerState) = with(state) {
        container.getBounds(currentBounds)
        previousHeight = previousBounds.height()
        currentHeight = currentBounds.height()
        var start = startAnchorY
        var end = endAnchorY
        when {
            startAnchorY == initialAnchorY -> {
                translationY = currentHeight
                end -= currentHeight
            }
            endAnchorY == initialAnchorY -> {
                translationY = currentHeight
                start -= currentHeight
            }
            else -> {
                translationY = 0
                start -= previousHeight
                end -= currentHeight
            }
        }
        inputView.translationY = translationY.toFloat()
        setAnchorY(initialAnchorY, start, end)

        canTransform = previous != null && current != null && previousHeight != currentHeight
        if (canTransform) container.setLeftTopRightBottomCompat(previousBounds)
    }

    override fun update(state: ContainerState) = with(state) {
        var fraction = interpolatedFraction
        if (translationY > 0f) {
            if (startAnchorY == initialAnchorY) fraction = 1 - fraction
            inputView.translationY = translationY * fraction
        }
        if (canTransform) {
            val dy = previousHeight + (currentHeight - previousHeight) * fraction
            container.getBounds(currentBounds) { top = bottom - dy.toInt() }
            container.setLeftTopRightBottomCompat(currentBounds)
        }
    }

    override fun end(state: ContainerState) {
        state.inputView.translationY = 0f
    }

    override fun launch(state: ContainerState, scope: EnforcerScope): Unit = with(state) {
        if (current == null) return
        scope.launch {
            var previousHeight = container.height
            val listener = ViewTreeObserver.OnDrawListener {
                val currentHeight = container.height
                if (previousHeight != currentHeight) {
                    val anchorY = initialAnchorY - inputView.editorOffset
                    state.setEditor(current, current)
                    state.setAnchorY(initialAnchorY, anchorY, anchorY)
                    scope.requestDispatch(state)
                    previousHeight = currentHeight
                }
            }
            try {
                container.viewTreeObserver.addOnDrawListener(listener)
                awaitCancellation()
            } finally {
                container.viewTreeObserver.removeOnDrawListener(listener)
            }
        }
    }
}