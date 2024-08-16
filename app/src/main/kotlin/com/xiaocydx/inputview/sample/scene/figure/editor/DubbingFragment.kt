package com.xiaocydx.inputview.sample.scene.figure.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.optimizeNextFrameScroll
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.concat.Concat
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.itemselect.SingleSelection
import com.xiaocydx.cxrv.itemselect.select
import com.xiaocydx.cxrv.itemselect.singleSelection
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.paging.LoadHeaderAdapter
import com.xiaocydx.cxrv.paging.LoadType
import com.xiaocydx.cxrv.paging.PagingEvent.LoadDataSuccess
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.inputview.sample.common.awaitTargetState
import com.xiaocydx.inputview.sample.common.disableItemAnimator
import com.xiaocydx.inputview.sample.common.dp
import com.xiaocydx.inputview.sample.common.launchRepeatOnLifecycle
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.common.setRoundRectOutlineProvider
import com.xiaocydx.inputview.sample.common.viewLifecycle
import com.xiaocydx.inputview.sample.common.viewLifecycleScope
import com.xiaocydx.inputview.sample.databinding.FragmentDubbingBinding
import com.xiaocydx.inputview.sample.databinding.HeaderDubbingLoadingBinding
import com.xiaocydx.inputview.sample.databinding.ItemDubbingBinding
import com.xiaocydx.inputview.sample.scene.figure.Dubbing
import com.xiaocydx.inputview.sample.scene.figure.FigureEditor.FigureDubbing
import com.xiaocydx.inputview.sample.scene.figure.FigureScene
import com.xiaocydx.inputview.sample.scene.figure.FigureViewModel
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.navigationBars
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author xcc
 * @date 2024/4/13
 */
class DubbingFragment : Fragment() {
    private lateinit var binding: FragmentDubbingBinding
    private lateinit var dubbingAdapter: ListAdapter<Dubbing, *>
    private lateinit var dubbingSelection: SingleSelection<Dubbing, String>
    private val sharedViewModel: FigureViewModel by activityViewModels()
    private val dubbingViewModel: DubbingViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentDubbingBinding.inflate(
        layoutInflater, container, false
    ).apply {
        binding = this
        root.insets().paddings(navigationBars())
        tvInput.onClick { sharedViewModel.submitScene(FigureScene.InputText) }
        tvFigure.onClick { sharedViewModel.submitScene(FigureScene.SelectFigure) }
        ivConfirm.onClick { sharedViewModel.confirmDubbing(dubbingSelection.selectedItem()) }

        dubbingAdapter = createDubbingAdapter()
        dubbingSelection = dubbingAdapter.singleSelection(itemKey = Dubbing::id)
        val header = LoadHeaderAdapter(dubbingAdapter) {
            loadingView { HeaderDubbingLoadingBinding.inflate(layoutInflater).root }
        }
        rvDubbing.linear(HORIZONTAL).disableItemAnimator()
            .divider { width(6.dp).height(6.dp).edge(Edge.all()) }
            .adapter(Concat.header(header).content(dubbingAdapter).concat())
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        launchDubbingPagingJob()
        launchDubbingSelectJob()
    }

    private fun createDubbingAdapter() = bindingAdapter(
        uniqueId = Dubbing::id,
        inflate = ItemDubbingBinding::inflate
    ) {
        val corners = 8.dp
        onCreateView {
            root.setRoundRectOutlineProvider(corners)
            bgSelected.setRoundRectOutlineProvider(corners)
        }

        onBindView {
            tvName.text = it.name
            ivSelected.isVisible = dubbingSelection.isSelected(holder)
            bgSelected.isVisible = dubbingSelection.isSelected(holder)
        }

        doOnSimpleItemClick { dubbingSelection.select(it) }
    }

    private fun launchDubbingPagingJob() {
        // Editor动画结束时，Lifecycle状态才会转换为RESUMED，
        // 刷新加载完成，挂起等待Lifecycle状态转换为RESUMED，
        // 避免动画运行时，其中一帧创建大量View造成动画卡顿。
        dubbingAdapter.pagingCollector.addHandleEventListener { _, event ->
            if (event is LoadDataSuccess && event.loadType == LoadType.REFRESH) {
                viewLifecycle.awaitTargetState(Lifecycle.State.RESUMED)
            }
        }
        // 收集配音的分页数据流
        dubbingViewModel.dubbingPagingFlow
            .onEach(dubbingAdapter.pagingCollector)
            .launchRepeatOnLifecycle(viewLifecycle)
    }

    private fun launchDubbingSelectJob() {
        // 当Editor更改为FigureDubbing时，
        // 选中当前数字人的配音，并滚动到目标位置。
        sharedViewModel.currentSceneFlow()
            .filter { it?.editor == FigureDubbing }
            .onEach {
                val current = sharedViewModel.currentFigure
                val targetPosition = if (current == null) {
                    dubbingSelection.clearSelected()
                    0
                } else {
                    dubbingSelection.select(current.dubbing)
                    dubbingViewModel.findTargetPosition(current.dubbing)
                }
                binding.rvDubbing.scrollToCenter(targetPosition)
            }
            .launchIn(viewLifecycleScope)
    }

    private fun RecyclerView.scrollToCenter(position: Int) {
        // 非平滑滚动到中间位置的简易实现
        scrollToPosition(position)
        doOnPreDraw {
            val itemView = findViewHolderForLayoutPosition(position)?.itemView
            itemView ?: return@doOnPreDraw
            val rvCenterX = (right + left) / 2
            val itemCenterX = (itemView.right + itemView.left) / 2
            scrollBy(itemCenterX - rvCenterX, 0)
        }
        optimizeNextFrameScroll()
    }
}