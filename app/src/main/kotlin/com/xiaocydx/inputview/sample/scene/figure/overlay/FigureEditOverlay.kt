package com.xiaocydx.inputview.sample.scene.figure.overlay

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.RequestManager
import com.xiaocydx.inputview.AnimationState
import com.xiaocydx.inputview.EditorMode
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.addAnimationCallback
import com.xiaocydx.inputview.sample.common.doOnTargetState
import com.xiaocydx.inputview.sample.common.isDispatchTouchEventEnabled
import com.xiaocydx.inputview.sample.common.matchParent
import com.xiaocydx.inputview.sample.scene.figure.FigureSnapshot
import com.xiaocydx.inputview.sample.scene.figure.FigureViewModel
import com.xiaocydx.inputview.sample.scene.transform.OverlayExtraStateHolder
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformation.ContainerState
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformationEnforcer
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

/**
 * @author xcc
 * @date 2024/4/13
 */
class FigureEditOverlay(
    private val activity: FragmentActivity,
    private val requestManager: RequestManager,
    private val sharedViewModel: FigureViewModel
) {
    private val root = FrameLayout(activity)
    private val inputView = InputView(activity)
    private val container = FrameLayout(activity)
    private val animator = FadeEditorAnimator(durationMillis = 300)
    private val adapter = FigureEditAdapter(activity)
    private val snapshotHolder = OverlayExtraStateHolder(FigureSnapshot())
    private val transformationEnforcer = OverlayTransformationEnforcer(
        owner = activity, editorAnimator = animator, editorAdapter = adapter,
        stateProvider = { FigureContainerState(inputView, container, snapshotHolder) }
    )

    init {
        inputView.apply {
            editorAnimator = animator
            editorAdapter = adapter
            editorMode = EditorMode.ADJUST_PAN
            setEditorBackgroundColor(0xFF1D1D1D.toInt())
        }
        // TODO: 去除
        inputView.addView(View(activity))
        root.addView(container, matchParent, matchParent)
        root.addView(inputView, matchParent, matchParent)
    }

    fun attachToWindow() = apply {
        activity.lifecycle.doOnTargetState(Lifecycle.State.CREATED) {
            val contentParent = activity.findViewById<ViewGroup>(android.R.id.content)
            contentParent.addView(root, matchParent, matchParent)
            initEnforcer()
            initCollect()
        }
    }

    private fun initEnforcer() {
        // 在动画运行时拦截触摸事件
        animator.addAnimationCallback(
            onStart = {
                setPageInvisible(start = true, it)
                activity.window.isDispatchTouchEventEnabled = false
            },
            onEnd = {
                setPageInvisible(start = false, it)
                activity.window.isDispatchTouchEventEnabled = true
            },
        )
        transformationEnforcer.add(snapshotHolder)
            .add(BackgroundTransformation(sharedViewModel::submitPendingEditor))
            .add(CoverGroupTransformation(requestManager, sharedViewModel))
            .attach(inputView, activity.onBackPressedDispatcher)
    }

    private fun initCollect() {
        sharedViewModel.figureState
            .mapNotNull { it.pendingBegin }
            .onEach {
                snapshotHolder.value = it.snapshot
                val current = transformationEnforcer.notify(it.editor)
                sharedViewModel.consumePendingSnapshot(current)
            }
            .launchIn(activity.lifecycleScope)
    }

    private fun setPageInvisible(start: Boolean, state: AnimationState) = with(state) {
        if (previous != null && current != null) return
        val pageInvisible = sharedViewModel.figureState.value.pageInvisible
        sharedViewModel.setPageInvisible(when (previous ?: current) {
            FigureEditor.TEXT -> pageInvisible.copy(text = start)
            FigureEditor.GRID, FigureEditor.DUBBING -> pageInvisible.copy(figure = start)
            else -> return
        })
    }
}

class FigureContainerState(
    inputView: InputView,
    container: ViewGroup,
    private val holder: OverlayExtraStateHolder<FigureSnapshot>
) : ContainerState(inputView, container) {
    val snapshot: FigureSnapshot
        get() = holder.value
}