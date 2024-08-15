package com.xiaocydx.inputview.sample.scene.figure.content

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.xiaocydx.inputview.sample.common.dp
import com.xiaocydx.inputview.sample.common.layoutParams
import com.xiaocydx.inputview.sample.common.matchParent
import com.xiaocydx.inputview.sample.common.viewLifecycleScope
import com.xiaocydx.inputview.sample.scene.figure.FigureContent.Cover
import com.xiaocydx.inputview.sample.scene.figure.FigureViewModel
import com.xiaocydx.inputview.sample.scene.figure.PendingView.Request
import com.xiaocydx.inputview.sample.scene.figure.ViewDrawable
import com.xiaocydx.inputview.sample.scene.figure.pager.FigureView
import com.xiaocydx.inputview.transform.Overlay
import com.xiaocydx.insets.doOnApplyWindowInsets
import com.xiaocydx.insets.statusBarHeight
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author xcc
 * @date 2024/8/15
 */
class CoverFragment : Fragment(), Overlay.Transform {
    private val sharedViewModel: FigureViewModel by activityViewModels()
    private val viewDrawable = ViewDrawable()
    private val coverEnterReturn = CoverEnterReturn(viewDrawable)
    private val coverFitCenterChange = CoverFitCenterChange(viewDrawable)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = View(requireContext()).layoutParams(matchParent, matchParent)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewDrawable.addToHost(view)
        view.transform().add(coverEnterReturn)
        view.transform().add(coverFitCenterChange)
        view.doOnApplyWindowInsets { _, insets, _ ->
            viewDrawable.setMargins(
                top = insets.statusBarHeight + 10.dp,
                bottom = 20.dp, horizontal = 20.dp
            )
        }
        launchCollectCurrentScene()
    }

    private fun launchCollectCurrentScene() {
        var figureJob: Job? = null
        sharedViewModel.currentSceneFlow().onEach {
            val isCover = it?.content == Cover
            if (isCover && figureJob == null) {
                figureJob = launchCollectCurrentFigure()
            } else if (!isCover && figureJob != null) {
                figureJob?.cancel()
                figureJob = null
            }
        }.launchIn(viewLifecycleScope)
    }

    private fun launchCollectCurrentFigure() = run {
        sharedViewModel.currentFigureFlow().onEach {
            val ref = sharedViewModel.requestView(Request.Figure)
            val previous = viewDrawable.setTarget(ref) as? FigureView
            // 恢复之前的figureView.children.alpha
            previous?.setAnimationAlpha(1f)
            // 基于当前的figureView，请求执行变换操作
            coverFitCenterChange.requestTransform()
        }.launchIn(viewLifecycleScope)
    }
}