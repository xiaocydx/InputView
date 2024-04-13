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
import com.xiaocydx.inputview.sample.databinding.FragmentFigureGridBinding
import com.xiaocydx.inputview.sample.databinding.ItemFigureGridBinding
import com.xiaocydx.inputview.sample.common.dp
import com.xiaocydx.inputview.sample.common.launchRepeatOnLifecycle
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.scene.figure.Figure
import com.xiaocydx.inputview.sample.scene.figure.FigureViewModel
import com.xiaocydx.inputview.sample.common.setRoundRectOutlineProvider
import com.xiaocydx.inputview.sample.common.viewLifecycle
import com.xiaocydx.insets.insets
import kotlinx.coroutines.flow.onEach

/**
 * @author xcc
 * @date 2024/4/13
 */
class FigureGridFragment : Fragment() {
    private lateinit var binding: FragmentFigureGridBinding
    private lateinit var selection: SingleSelection<Figure, String>
    private lateinit var figureAdapter: ListAdapter<Figure, *>
    private val sharedViewModel: FigureViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentFigureGridBinding.inflate(
        layoutInflater, container, false
    ).apply {
        binding = this
        tvDubbing.onClick { sharedViewModel.submitPendingEditor(FigureEditor.DUBBING) }

        val requestManager = Glide.with(this@FigureGridFragment)
        figureAdapter = bindingAdapter(
            uniqueId = Figure::id,
            inflate = ItemFigureGridBinding::inflate
        ) {
            val corners = 8.dp
            selection = singleSelection(itemKey = Figure::id)
            onCreateView {
                ivCover.setRoundRectOutlineProvider(corners)
                ivSelected.setRoundRectOutlineProvider(corners)
                bgSelected.setRoundRectOutlineProvider(corners)
            }
            onBindView {
                requestManager.load(it.coverUrl)
                    .placeholder(ColorDrawable(0xFF212123.toInt()))
                    .transform(MultiTransformation(CenterCrop(), RoundedCorners(corners)))
                    .into(ivCover)
                ivSelected.isVisible = selection.isSelected(holder)
                bgSelected.isVisible = selection.isSelected(holder)
            }
            doOnSimpleLongItemClick {
                sharedViewModel.submitPendingRemove(it)
                true
            }
            doOnSimpleItemClick(sharedViewModel::selectFigure)
        }

        rvFigure.grid(spanCount = 3).adapter(figureAdapter)
            .divider { width(6.dp).height(6.dp).edge(Edge.all()) }
            .apply { itemAnimator = null }
            .insets().gestureNavBarEdgeToEdge()
    }.root

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ): Unit = with(binding) {
        sharedViewModel.figureListFlow
            .onEach(figureAdapter.listCollector)
            .launchRepeatOnLifecycle(viewLifecycle)

        val figureState = sharedViewModel.figureState
        sharedViewModel.currentFigureFlow().onEach { current ->
            if (current == null) {
                selection.clearSelected()
            } else if (selection.select(current)) {
                rvFigure.scrollToPosition(figureState.value.currentPosition)
            }
        }.launchRepeatOnLifecycle(viewLifecycle)
    }
}