@file:Suppress("CanBeParameter")

package com.xiaocydx.inputview.sample.scene.figure.overlay

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import androidx.activity.OnBackPressedDispatcher
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.RequestManager
import com.xiaocydx.inputview.AnimationState
import com.xiaocydx.inputview.EditorMode
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.addAnimationCallback
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.sample.common.doOnTargetState
import com.xiaocydx.inputview.sample.common.isDispatchTouchEventEnabled
import com.xiaocydx.inputview.sample.common.matchParent
import com.xiaocydx.inputview.sample.scene.figure.FigureSnapshot
import com.xiaocydx.inputview.sample.scene.figure.FigureViewModel
import com.xiaocydx.inputview.sample.scene.transform.OverlayStateExtraHolder
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
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val fragmentManager: FragmentManager,
    private val requestManager: RequestManager,
    private val sharedViewModel: FigureViewModel
) {
    private val inputView = InputView(context)
    private val container = FrameLayout(context)
    private val animator = FadeEditorAnimator(durationMillis = 300)
    private val adapter = FigureEditAdapter(lifecycleOwner.lifecycle, fragmentManager)
    private val snapshotHolder = OverlayStateExtraHolder(FigureSnapshot())
    private val transformationEnforcer = OverlayTransformationEnforcer(
        lifecycleOwner = lifecycleOwner,
        editorAnimator = animator, editorAdapter = adapter,
        stateProvider = { FigureSnapshotState(inputView, container, snapshotHolder) }
    )

    fun attachToWindow(
        window: Window,
        dispatcher: OnBackPressedDispatcher? = null
    ) = apply {
        val first = InputView.init(
            window = window,
            statusBarEdgeToEdge = true,
            gestureNavBarEdgeToEdge = true
        )
        if (!first) return@apply
        lifecycleOwner.lifecycle.doOnTargetState(CREATED) {
            val contentParent = window.findViewById<ViewGroup>(android.R.id.content)
            contentParent.addView(initRoot(window), matchParent, matchParent)
            initEnforcer(dispatcher)
            initCollect()
        }
        lifecycleOwner.lifecycle.doOnTargetState(DESTROYED) {
            sharedViewModel.submitPendingEditor(null)
        }
    }

    private fun initRoot(window: Window): View {
        inputView.apply {
            editorAnimator = animator
            editorAdapter = adapter
            editorMode = EditorMode.ADJUST_PAN
            setEditorBackgroundColor(0xFF1D1D1D.toInt())
        }
        // 在动画运行时拦截触摸事件
        animator.addAnimationCallback(
            onStart = {
                setPageInvisible(start = true, it)
                window.isDispatchTouchEventEnabled = false
            },
            onEnd = {
                setPageInvisible(start = false, it)
                window.isDispatchTouchEventEnabled = true
            },
        )
        val root = FrameLayout(context)
        root.addView(container, matchParent, matchParent)
        root.addView(inputView, matchParent, matchParent)
        return root
    }

    private fun initEnforcer(dispatcher: OnBackPressedDispatcher?) {
        transformationEnforcer
            .add(snapshotHolder)
            .add(BackgroundTransformation(
                showEditor = sharedViewModel::submitPendingEditor
            ))
            .add(CoverGroupTransformation(
                requestManager = requestManager,
                updateCurrent = sharedViewModel.currentFigureFlow(),
                currentFigure = sharedViewModel::currentFigure,
                requestSnapshot = { sharedViewModel.submitPendingEditor(it, request = true) }
            ))
            .add(TextGroupTransformation(
                showEditor = sharedViewModel::submitPendingEditor,
                currentText = { sharedViewModel.figureState.value.currentText },
                confirmText = sharedViewModel::confirmText
            ))
            .attach(inputView, dispatcher)
    }

    private fun initCollect() {
        adapter.addEditorChangedListener { _, current ->
            // 同步当前Editor，例如隐藏IME
            sharedViewModel.submitPendingEditor(current)
        }
        sharedViewModel.figureState
            .mapNotNull { it.pendingBegin }
            .onEach {
                snapshotHolder.setValue(it.snapshot, it.request)
                val current = transformationEnforcer.notify(it.editor)
                sharedViewModel.consumePendingSnapshot(current)
            }
            .launchIn(lifecycleOwner.lifecycleScope)
    }

    private fun setPageInvisible(start: Boolean, state: AnimationState) = with(state) {
        if (previous != null && current != null) return
        val pageInvisible = sharedViewModel.figureState.value.pageInvisible
        sharedViewModel.setPageInvisible(when (previous ?: current) {
            FigureEditor.INPUT, FigureEditor.EMOJI -> pageInvisible.copy(text = start)
            FigureEditor.GRID, FigureEditor.DUBBING -> pageInvisible.copy(figure = start)
            else -> return
        })
    }
}

class FigureSnapshotState(
    inputView: InputView,
    container: ViewGroup,
    private val holder: OverlayStateExtraHolder<FigureSnapshot>
) : ContainerState(inputView, container) {

    val snapshot: FigureSnapshot
        get() = holder.value
}