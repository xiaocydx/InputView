package com.xiaocydx.inputview.sample.scene.figure.editor

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
import com.xiaocydx.cxrv.itemclick.reduce.doOnItemClick
import com.xiaocydx.cxrv.itemclick.reduce.doOnLongItemClick
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
import com.xiaocydx.inputview.sample.scene.figure.FigureEditor.FigureGrid
import com.xiaocydx.inputview.sample.scene.figure.FigureScene
import com.xiaocydx.inputview.sample.scene.figure.FigureViewModel
import com.xiaocydx.insets.insets
import kotlinx.coroutines.Job
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
    ) = FragmentFigureGridBinding.inflate(
        layoutInflater, container, false
    ).apply {
        binding = this
        tvInput.onClick { sharedViewModel.submitScene(FigureScene.InputText) }
        tvDubbing.onClick { sharedViewModel.submitScene(FigureScene.SelectDubbing) }

        figureAdapter = createFigureAdapter()
        figureSelection = figureAdapter.singleSelection(itemKey = Figure::id)
        rvFigure.grid(spanCount = 3).disableItemAnimator()
            .divider { width(6.dp).height(6.dp).edge(Edge.all()) }
            .adapter(figureAdapter).insets().gestureNavBarEdgeToEdge()
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        launchFigureListJob()
        launchFigureGridJob()
    }

    private fun createFigureAdapter() = bindingAdapter(
        uniqueId = Figure::id,
        inflate = ItemFigureGridBinding::inflate
    ) {
        val corners = 8.dp
        onCreateView {
            ivCover.setRoundRectOutlineProvider(corners)
            bgSelected.setRoundRectOutlineProvider(corners)
        }

        val requestManager = Glide.with(this@FigureGridFragment)
        onBindView {
            requestManager.load(it.coverUrl)
                .placeholder(ColorDrawable(0xFF212123.toInt()))
                .transform(MultiTransformation(CenterCrop(), RoundedCorners(corners)))
                .into(ivCover)
            ivSelected.isVisible = figureSelection.isSelected(holder)
            bgSelected.isVisible = figureSelection.isSelected(holder)
        }

        doOnLongItemClick {
            sharedViewModel.submitRemove(it)
            true
        }
        doOnItemClick { sharedViewModel.selectFigure(it) }
    }

    private fun launchFigureListJob() {
        // 收集数字人的列表数据流
        sharedViewModel.figureListFlow
            .onEach(figureAdapter.listCollector)
            .launchRepeatOnLifecycle(viewLifecycle)
    }

    private fun launchFigureGridJob() {
        var selectJob: Job? = null
        sharedViewModel.currentSceneFlow().onEach {
            val isGrid = it?.editor == FigureGrid
            if (isGrid) {
                // Editor更改为FigureGrid，滚动到目标位置
                val figureState = sharedViewModel.figureState.value
                binding.rvFigure.scrollToPosition(figureState.currentPosition)
            }

            if (isGrid && selectJob == null) {
                // Editor更改为FigureGrid，启动selectJob
                selectJob = launchFigureSelectJob()
            } else if (!isGrid && selectJob != null) {
                selectJob?.cancel()
                selectJob = null
            }
        }.launchIn(viewLifecycleScope)
    }

    private fun launchFigureSelectJob() = run {
        sharedViewModel.currentFigureFlow().onEach {
            if (it == null) {
                figureSelection.clearSelected()
            } else {
                figureSelection.select(it)
            }
        }.launchIn(viewLifecycleScope)
    }
}