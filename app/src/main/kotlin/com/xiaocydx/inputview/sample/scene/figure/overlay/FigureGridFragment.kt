package com.xiaocydx.inputview.sample.scene.figure.overlay

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.itemclick.doOnSimpleLongItemClick
import com.xiaocydx.cxrv.itemselect.SingleSelection
import com.xiaocydx.cxrv.itemselect.select
import com.xiaocydx.cxrv.itemselect.singleSelection
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.grid
import com.xiaocydx.cxrv.list.listCollector
import com.xiaocydx.cxrv.list.onEach
import com.xiaocydx.inputview.sample.common.disableItemAnimator
import com.xiaocydx.inputview.sample.common.dp
import com.xiaocydx.inputview.sample.common.launchRepeatOnLifecycle
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.common.setRoundRectOutlineProvider
import com.xiaocydx.inputview.sample.common.viewLifecycle
import com.xiaocydx.inputview.sample.common.viewLifecycleScope
import com.xiaocydx.inputview.sample.databinding.FragmentFigureGridBinding
import com.xiaocydx.inputview.sample.databinding.ItemFigureGridBinding
import com.xiaocydx.inputview.sample.scene.figure.Figure
import com.xiaocydx.inputview.sample.scene.figure.FigureViewModel
import com.xiaocydx.inputview.sample.scene.figure.overlay.FigureEditor.DUBBING
import com.xiaocydx.inputview.sample.scene.figure.overlay.FigureEditor.GRID
import com.xiaocydx.inputview.sample.scene.figure.overlay.FigureEditor.INPUT
import com.xiaocydx.insets.insets
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author xcc
 * @date 2024/4/13
 */
class FigureGridFragment : Fragment() {
    private lateinit var binding: FragmentFigureGridBinding
    private lateinit var figureAdapter: ListAdapter<Figure, *>
    private lateinit var figureSelection: SingleSelection<Figure, String>
    private val sharedViewModel: FigureViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentFigureGridBinding.inflate(
        layoutInflater, container, false
    ).apply {
        binding = this
        tvInput.onClick { sharedViewModel.submitPendingEditor(INPUT) }
        tvDubbing.onClick { sharedViewModel.submitPendingEditor(DUBBING) }

        val requestManager = Glide.with(this@FigureGridFragment)
        figureAdapter = bindingAdapter(
            uniqueId = Figure::id,
            inflate = ItemFigureGridBinding::inflate
        ) {
            val corners = 8.dp
            figureSelection = singleSelection(itemKey = Figure::id)
            onCreateView {
                ivCover.setRoundRectOutlineProvider(corners)
                bgSelected.setRoundRectOutlineProvider(corners)
            }
            onBindView {
                requestManager.load(it.coverUrl)
                    .placeholder(ColorDrawable(0xFF212123.toInt()))
                    .transform(MultiTransformation(CenterCrop(), RoundedCorners(corners)))
                    .into(ivCover)
                ivSelected.isVisible = figureSelection.isSelected(holder)
                bgSelected.isVisible = figureSelection.isSelected(holder)
            }
            doOnSimpleLongItemClick {
                sharedViewModel.submitPendingRemove(it)
                true
            }
            doOnSimpleItemClick(sharedViewModel::selectFigure)
        }

        rvFigure.grid(spanCount = 3).disableItemAnimator()
            .divider { width(6.dp).height(6.dp).edge(Edge.all()) }
            .adapter(figureAdapter).insets().gestureNavBarEdgeToEdge()
    }.root

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ): Unit = with(binding) {
        // 收集数字人的列表数据流
        sharedViewModel.figureListFlow
            .onEach(figureAdapter.listCollector)
            .launchRepeatOnLifecycle(viewLifecycle)

        // 当前数字人改变，选中当前数字人
        sharedViewModel.currentFigureFlow()
            .onEach { current ->
                if (current == null) {
                    figureSelection.clearSelected()
                } else {
                    figureSelection.select(current)
                }
            }
            .launchRepeatOnLifecycle(viewLifecycle)

        // 当Editor更改为FigureEditor.GRID时，
        // 选中当前数字人，并滚动到目标位置。
        sharedViewModel.currentEditorFlow()
            .filter { it == GRID }
            .onEach {
                val figureState = sharedViewModel.figureState.value
                rvFigure.scrollToPosition(figureState.currentPosition)
            }
            .launchIn(viewLifecycleScope)
    }
}