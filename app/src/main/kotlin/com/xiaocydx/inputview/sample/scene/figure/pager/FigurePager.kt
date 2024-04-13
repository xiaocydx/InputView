@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.xiaocydx.inputview.sample.scene.figure.pager

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.doOnPreDraw
import androidx.core.view.isInvisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.smoothScroller
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.RequestManager
import com.xiaocydx.cxrv.binding.BindingHolder
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.itemclick.doOnLongItemClick
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.doOnListChanged
import com.xiaocydx.cxrv.list.listCollector
import com.xiaocydx.cxrv.list.onEach
import com.xiaocydx.cxrv.viewpager2.loop.LinearSmoothScrollerProvider
import com.xiaocydx.inputview.sample.common.awaitNextLayout
import com.xiaocydx.inputview.sample.common.dp
import com.xiaocydx.inputview.sample.common.launchRepeatOnLifecycle
import com.xiaocydx.inputview.sample.common.registerOnPageChangeCallback
import com.xiaocydx.inputview.sample.databinding.ItemFigureBinding
import com.xiaocydx.inputview.sample.scene.figure.Figure
import com.xiaocydx.inputview.sample.scene.figure.FigureBounds
import com.xiaocydx.inputview.sample.scene.figure.FigureSnapshot
import com.xiaocydx.inputview.sample.scene.figure.FigureViewModel
import com.xiaocydx.inputview.sample.scene.figure.overlay.FigureEditor
import kotlinx.coroutines.flow.onEach
import java.lang.ref.WeakReference
import kotlin.math.absoluteValue

/**
 * @author xcc
 * @date 2024/4/13
 */
class FigurePager(
    private val lifecycle: Lifecycle,
    private val viewPager2: ViewPager2,
    private val requestManager: RequestManager,
    private val sharedViewModel: FigureViewModel
) {
    private lateinit var figureAdapter: ListAdapter<Figure, *>
    private val rv = viewPager2.getChildAt(0) as RecyclerView
    private val scrollerProvider = LinearSmoothScrollerProvider(
        durationMs = 300, interpolator = AccelerateDecelerateInterpolator()
    )

    fun init() = apply {
        initView()
        initCollect()
    }

    suspend fun awaitSnapshot(): FigureSnapshot {
        if (viewPager2.isLayoutRequested) viewPager2.awaitNextLayout()
        // 按当前位置重新计算一遍变换属性的值，确保figureBounds正确
        viewPager2.requestTransform()
        return FigureSnapshot(
            figure = sharedViewModel.currentFigure,
            figureBounds = getCurrentBinding()?.figureView?.let(FigureBounds::from)
        )
    }

    private fun initView() {
        figureAdapter = bindingAdapter(
            uniqueId = Figure::id,
            inflate = ItemFigureBinding::inflate
        ) {
            onBindView { figureView.setFigure(requestManager, it) }
            doOnItemClick(target = { binding.figureView }) { holder, _ ->
                submitOrScroll(holder, FigureEditor.GRID)
            }
            doOnItemClick(target = { binding.figureView.tvDubbing }) { holder, _ ->
                submitOrScroll(holder, FigureEditor.DUBBING)
            }
            doOnLongItemClick(target = { binding.figureView }) { holder, item ->
                if (!isCurrent(holder)) return@doOnLongItemClick false
                sharedViewModel.submitPendingRemove(item)
                true
            }
            var isRequestTransform = false
            doOnListChanged {
                // 当列表已更改时，在下一帧布局完成后，
                // 调用requestTransform()修正变换属性。
                if (isRequestTransform) return@doOnListChanged
                isRequestTransform = true
                viewPager2.doOnPreDraw {
                    isRequestTransform = false
                    viewPager2.requestTransform()
                }
            }
        }

        viewPager2.apply {
            adapter = figureAdapter
            registerOnPageChangeCallback(onSelected = sharedViewModel::selectPosition)
            setPageTransformer(CompositePageTransformer().apply {
                addTransformer(MarginPageTransformer(20.dp))
                addTransformer(ScaleInTransformer())
                addTransformer(FadeInTransformer(minAlpha = 0.3f))
            })
            rv.clipToPadding = false
            rv.updatePadding(left = 60.dp, right = 60.dp)
        }
    }

    private fun initCollect() {
        sharedViewModel.figureListFlow
            .onEach(figureAdapter.listCollector)
            .launchRepeatOnLifecycle(lifecycle)

        var isInVisible: Boolean? = null
        var currentView: WeakReference<View>? = null
        sharedViewModel.figureState.onEach { state ->
            if (state.pageInvisible.figure != isInVisible) {
                isInVisible = state.pageInvisible.figure
                val view = getCurrentBinding()?.figureView
                val previous = currentView?.get()
                if (view !== previous) {
                    // 改变isInVisible的同时，可能进行滚动，需要将previous恢复为可视
                    previous?.isInvisible = false
                    currentView = view?.let(::WeakReference)
                }
                view?.isInvisible = isInVisible!!
            }
            if (state.currentPosition != viewPager2.currentItem) {
                viewPager2.setCurrentItem(state.currentPosition, false)
            }
        }.launchRepeatOnLifecycle(lifecycle)
    }

    private fun isCurrent(holder: RecyclerView.ViewHolder): Boolean {
        return viewPager2.currentItem == holder.layoutPosition
    }

    @Suppress("UNCHECKED_CAST")
    private fun getCurrentBinding(): ItemFigureBinding? {
        val holder = rv.findViewHolderForAdapterPosition(viewPager2.currentItem)
        return (holder as? BindingHolder<ItemFigureBinding>)?.binding
    }

    private fun submitOrScroll(holder: RecyclerView.ViewHolder, editor: FigureEditor?) {
        if (isCurrent(holder)) {
            sharedViewModel.submitPendingEditor(editor)
            return
        }
        // 超过3页的平滑滚动，ViewPager2会先非平滑到靠近的位置，再进行平滑滚动，
        // 当前需求不用支持平滑滚动到任意位置，因此限制targetPosition在3页以内。
        require((viewPager2.currentItem - holder.layoutPosition).absoluteValue <= 3)
        viewPager2.setCurrentItem(holder.layoutPosition, true)
        val smoothScroller = requireNotNull(rv.layoutManager?.smoothScroller)
        val newSmoothScroller = scrollerProvider.create(rv.context)
        newSmoothScroller.targetPosition = smoothScroller.targetPosition
        rv.layoutManager?.startSmoothScroll(newSmoothScroller)
    }
}