@file:Suppress("CanBeParameter")

package com.xiaocydx.inputview.sample.scene.video

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import androidx.activity.OnBackPressedDispatcher
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.xiaocydx.inputview.EditorMode
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.addAnimationCallback
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.sample.common.doOnTargetState
import com.xiaocydx.inputview.sample.common.isDispatchTouchEventEnabled
import com.xiaocydx.inputview.sample.common.matchParent
import com.xiaocydx.inputview.sample.common.wrapContent
import com.xiaocydx.inputview.sample.scene.transform.BoundsTransformation
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformation.ContainerState
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformationEnforcer

/**
 * @author xcc
 * @date 2024/4/13
 */
class VideoEditOverlay(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val fragmentManager: FragmentManager
) {
    private val inputView = InputView(context)
    private val container = FrameLayout(context)
    private val animator = FadeEditorAnimator(durationMillis = 300)
    private val adapter = VideoEditorAdapter(lifecycleOwner.lifecycle, fragmentManager)
    private val transformationEnforcer = OverlayTransformationEnforcer(
        lifecycleOwner = lifecycleOwner,
        editorAnimator = animator, editorAdapter = adapter,
        stateProvider = { ContainerState(inputView, container) }
    )

    fun notify(editor: VideoEditor?) {
        transformationEnforcer.notify(editor)
    }

    fun attachToWindow(
        window: Window,
        preview: View,
        dispatcher: OnBackPressedDispatcher? = null
    ) = apply {
        val first = InputView.init(
            window = window,
            statusBarEdgeToEdge = true,
            gestureNavBarEdgeToEdge = true
        )
        if (!first) return@apply
        lifecycleOwner.lifecycle.doOnTargetState(Lifecycle.State.CREATED) {
            val contentParent = window.findViewById<ViewGroup>(android.R.id.content)
            contentParent.addView(initRoot(window), matchParent, matchParent)
            initEnforcer(preview, dispatcher)
        }
    }

    private fun initRoot(window: Window): View {
        inputView.apply {
            editorAnimator = animator
            editorAdapter = adapter
            editorMode = EditorMode.ADJUST_PAN
            addView(container, matchParent, wrapContent)
            setEditorBackgroundColor(0xFF1D1D1D.toInt())
        }
        container.setBackgroundColor(0xFF1D1D1D.toInt())
        // 在动画运行时拦截触摸事件
        animator.addAnimationCallback(
            onStart = { window.isDispatchTouchEventEnabled = false },
            onEnd = { window.isDispatchTouchEventEnabled = true },
        )
        return inputView
    }

    private fun initEnforcer(preview: View, dispatcher: OnBackPressedDispatcher?) {
        transformationEnforcer
            .add(BoundsTransformation())
            .add(PreviewTransformation(preview))
            .add(TextGroupTransformation(transformationEnforcer::notify))
            .add(CommonGroupTransformation(transformationEnforcer::notify))
            .attach(inputView, dispatcher)
    }
}