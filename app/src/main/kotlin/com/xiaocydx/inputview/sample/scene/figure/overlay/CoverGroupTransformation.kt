package com.xiaocydx.inputview.sample.scene.figure.overlay

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.graphics.withMatrix
import com.xiaocydx.inputview.sample.common.dp
import com.xiaocydx.inputview.sample.scene.figure.Figure
import com.xiaocydx.inputview.sample.scene.figure.ViewBounds
import com.xiaocydx.inputview.sample.scene.figure.overlay.FigureEditor.FigureDubbing
import com.xiaocydx.inputview.sample.scene.figure.overlay.FigureEditor.FigureGrid
import com.xiaocydx.inputview.sample.scene.figure.pager.FigureView
import com.xiaocydx.inputview.sample.scene.transform.ContainerTransformation
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformation.EnforcerScope
import com.xiaocydx.insets.getRootWindowInsetsCompat
import com.xiaocydx.insets.statusBarHeight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.lang.ref.WeakReference

/**
 * @author xcc
 * @date 2024/4/13
 */
class CoverGroupTransformation(
    private val updateCurrent: Flow<Figure?>,
    private val requestSnapshot: (FigureEditor?) -> Unit
) : ContainerTransformation<FigureSnapshotState>(FigureGrid, FigureDubbing) {
    private var view: View? = null
    private val drawable = FigureViewDrawable()

    override fun getView(state: FigureSnapshotState): View {
        return view ?: View(state.container.context)
            .apply { overlay.add(drawable) }.also { view = it }
    }

    override fun onPrepare(state: FigureSnapshotState) {
        // 恢复之前的figureView.children.alpha
        drawable.figureView?.get()?.setAnimationAlpha(1f)
        drawable.figureView = state.snapshot.figureView
    }

    override fun onStart(state: FigureSnapshotState) {
        val view = view ?: return
        val topY = view.getRootWindowInsetsCompat()?.statusBarHeight ?: 0
        drawable.setBounds(0, 0, view.width, view.height)
        drawable.calculateStartAndEndValues(topY, state)
        drawable.figureView?.get()?.alpha = 0f
    }

    override fun onUpdate(state: FigureSnapshotState) = with(state) {
        drawable.figureView?.get()?.setAnimationAlpha(when {
            previous != null && current != null -> 0f
            current != null -> 1f - interpolatedFraction
            else -> interpolatedFraction
        })
        drawable.fraction = interpolatedFraction
    }

    override fun onEnd(state: FigureSnapshotState) {
        drawable.figureView?.get()?.alpha = 1f
    }

    override fun onLaunch(state: FigureSnapshotState, scope: EnforcerScope) {
        updateCurrent.onEach {
            // 当前数字人已变更，请求分发新的快照
            requestSnapshot(requireNotNull(state.current as? FigureEditor))
        }.launchIn(scope)
    }

    private class FigureViewDrawable : Drawable() {
        private val matrix = Matrix()
        private val margins = 20.dp
        private val marginTop = 10.dp
        private var topY = 0
        private var startScale = 1f
        private var endScale = 1f
        private var startTransY = 0f
        private var endTransY = 0f
        private var figureBounds = ViewBounds()

        var figureView: WeakReference<FigureView>? = null
            set(value) {
                field = value
                figureBounds = value?.get()
                    ?.let(ViewBounds::from) ?: ViewBounds()
                invalidateSelf()
            }

        var fraction = 0f
            set(value) {
                if (field == value) return
                field = value
                invalidateSelf()
            }

        fun calculateStartAndEndValues(topY: Int, state: FigureSnapshotState) {
            // 基于初始状态，计算view变换的起始值和结束值
            val bounds = figureBounds
            this.topY = topY
            startScale = calculateScale(state, state.startAnchorY, bounds)
            startTransY = calculateTransY(state, state.startAnchorY, startScale, bounds)
            endScale = calculateScale(state, state.endAnchorY, bounds)
            endTransY = calculateTransY(state, state.endAnchorY, endScale, bounds)
        }

        override fun draw(canvas: Canvas) {
            val view = figureView?.get() ?: return
            val x = figureBounds.left.toFloat()
            val y = figureBounds.top.toFloat()
            val width = figureBounds.width.toFloat()
            val height = figureBounds.height.toFloat()
            val scale = startScale + (endScale - startScale) * fraction
            val translationY = startTransY + (endTransY - startTransY) * fraction
            matrix.reset()
            matrix.postTranslate(-width / 2, -height / 2)
            matrix.postScale(scale, scale)
            matrix.postTranslate(width / 2, height / 2)
            matrix.postTranslate(x, y + translationY)
            canvas.withMatrix(matrix, view::draw)
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

        override fun setAlpha(alpha: Int) = Unit
        override fun setColorFilter(colorFilter: ColorFilter?) = Unit
        override fun getOpacity() = PixelFormat.UNKNOWN
    }
}