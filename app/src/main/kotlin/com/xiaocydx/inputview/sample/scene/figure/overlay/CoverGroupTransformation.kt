package com.xiaocydx.inputview.sample.scene.figure.overlay

import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.RequestManager
import com.xiaocydx.inputview.sample.common.dp
import com.xiaocydx.inputview.sample.scene.figure.Figure
import com.xiaocydx.inputview.sample.scene.figure.ViewBounds
import com.xiaocydx.inputview.sample.scene.figure.overlay.FigureEditor.DUBBING
import com.xiaocydx.inputview.sample.scene.figure.overlay.FigureEditor.GRID
import com.xiaocydx.inputview.sample.scene.figure.pager.FigureView
import com.xiaocydx.inputview.sample.scene.transform.ContainerTransformation
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformation.EnforcerScope
import com.xiaocydx.insets.getRootWindowInsetsCompat
import com.xiaocydx.insets.statusBarHeight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author xcc
 * @date 2024/4/13
 */
class CoverGroupTransformation(
    private val requestManager: RequestManager,
    private val updateCurrent: Flow<Figure?>,
    private val currentFigure: () -> Figure?,
    private val requestSnapshot: (FigureEditor?) -> Unit
) : ContainerTransformation<FigureSnapshotState>(GRID, DUBBING) {
    private var view: FigureView? = null
    private val margins = 20.dp
    private val marginTop = 10.dp
    private var topY = 0
    private var startScale = 1f
    private var endScale = 1f
    private var startTransY = 0f
    private var endTransY = 0f

    override fun getView(state: FigureSnapshotState): View {
        return view ?: FigureView(state.container.context).also { view = it }
    }

    override fun onPrepare(state: FigureSnapshotState) = with(state) {
        val view = view ?: return
        val figure = currentFigure()
        val bounds = snapshot.figureBounds
        if (figure == null || bounds == null) {
            view.isInvisible = true
            return
        }
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = bounds.top
            leftMargin = bounds.left
            width = bounds.width
            height = bounds.height
        }
        view.isVisible = true
        view.children.forEach { it.alpha = 1f }
        view.setFigure(requestManager, figure)
    }

    override fun onStart(state: FigureSnapshotState) {
        val view = view ?: return
        val bounds = state.snapshot.figureBounds ?: return
        // 基于初始状态，计算view变换的起始值和结束值
        topY = view.getRootWindowInsetsCompat()?.statusBarHeight ?: 0
        startScale = calculateScale(state, state.startAnchorY, bounds)
        startTransY = calculateTransY(state, state.startAnchorY, startScale, bounds)
        endScale = calculateScale(state, state.endAnchorY, bounds)
        endTransY = calculateTransY(state, state.endAnchorY, endScale, bounds)
    }

    override fun onUpdate(state: FigureSnapshotState) = with(state) {
        val view = view ?: return
        val childAlpha = when {
            previous != null && current != null -> 0f
            current != null -> 1f - interpolatedFraction
            else -> interpolatedFraction
        }
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            if (child === view.ivCover) continue
            child.alpha = childAlpha
        }
        val scale = startScale + (endScale - startScale) * interpolatedFraction
        view.scaleX = scale
        view.scaleY = scale
        view.translationY = startTransY + (endTransY - startTransY) * interpolatedFraction
    }

    override fun onLaunch(state: FigureSnapshotState, scope: EnforcerScope) {
        updateCurrent.onEach {
            // 当前数字人已变更，请求分发新的快照
            requestSnapshot(requireNotNull(state.current as? FigureEditor))
        }.launchIn(scope)
    }

    private fun calculateScale(
        state: FigureSnapshotState,
        endAnchorY: Int,
        bounds: ViewBounds?
    ) = state.run {
        if (bounds == null || endAnchorY == initialAnchorY) return@run 1f
        val startHeight = bounds.height
        val top = topY + marginTop
        val bottom = endAnchorY - margins
        val endHeight = (bottom - top).coerceAtLeast(0)
        var scale = endHeight.toFloat() / startHeight

        val startWidth = bounds.width
        val endWidth = (container.width - margins * 2).coerceAtLeast(0)
        if (startWidth * scale > endWidth) scale = endWidth.toFloat() / startWidth
        scale
    }

    private fun calculateTransY(
        state: FigureSnapshotState,
        endAnchorY: Int,
        endScale: Float, bounds: ViewBounds?
    ) = state.run {
        if (bounds == null || endAnchorY == initialAnchorY) return@run 0f
        var top = topY + marginTop
        var bottom = endAnchorY - margins
        if (bounds.height * endScale < bottom - top) {
            // 缩放后的高度小于边界高度，去除margin，
            // view中心点平移至最大高度边界的中心点。
            top -= marginTop
            bottom += margins
        }
        val endHeight = (bottom - top).coerceAtLeast(0)
        val startCenterY = (bounds.top + bounds.bottom) / 2
        val endCenterY = top + endHeight / 2
        return endCenterY.toFloat() - startCenterY
    }
}