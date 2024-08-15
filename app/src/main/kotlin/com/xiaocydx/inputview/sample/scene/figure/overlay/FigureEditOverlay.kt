@file:Suppress("CanBeParameter")

package com.xiaocydx.inputview.sample.scene.figure.overlay

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.LifecycleOwner
import com.xiaocydx.inputview.EditorMode
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.addAnimationCallback
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.sample.common.doOnTargetState
import com.xiaocydx.inputview.sample.common.isDispatchTouchEventEnabled
import com.xiaocydx.inputview.sample.common.matchParent
import com.xiaocydx.inputview.sample.scene.figure.FigureEditAdapter
import com.xiaocydx.inputview.sample.scene.figure.FigureViewModel

/**
 * @author xcc
 * @date 2024/4/13
 */
class FigureEditOverlay(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val fragmentManager: FragmentManager,
    private val sharedViewModel: FigureViewModel
) {
    private val inputView = InputView(context)
    private val container = FrameLayout(context)
    private val animator = FadeEditorAnimator(durationMillis = 300)
    private val adapter = FigureEditAdapter(lifecycleOwner.lifecycle, fragmentManager)

    fun attachToWindow(window: Window) = apply {
        val first = InputView.init(
            window = window,
            statusBarEdgeToEdge = true,
            gestureNavBarEdgeToEdge = true
        )
        if (!first) return@apply
        lifecycleOwner.lifecycle.doOnTargetState(CREATED) {
            val contentParent = window.findViewById<ViewGroup>(android.R.id.content)
            contentParent.addView(initRoot(window), matchParent, matchParent)
        }
        lifecycleOwner.lifecycle.doOnTargetState(DESTROYED) {
            sharedViewModel.submitPendingScene(null)
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
            onStart = { window.isDispatchTouchEventEnabled = false },
            onEnd = { window.isDispatchTouchEventEnabled = true },
        )
        // 同步当前Editor，例如隐藏IME
        adapter.addEditorChangedListener { previous, current ->
            // if (previous === FigureEditor.Ime && current == null
            //         && sharedViewModel.figureState.value.pendingScene == null) {
            //     // 非主动隐藏IME，重定向为INPUT_IDLE，实现不退出文字输入的效果
            //     transformationEnforcer.notify(FigureEditor.ImeIdle)
            // } else {
            //     sharedViewModel.submitPendingEditor(current)
            // }
        }
        val root = FrameLayout(context)
        root.addView(container, matchParent, matchParent)
        root.addView(inputView, matchParent, matchParent)
        return root
    }
}