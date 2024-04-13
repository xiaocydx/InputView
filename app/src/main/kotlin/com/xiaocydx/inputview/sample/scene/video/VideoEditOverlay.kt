package com.xiaocydx.inputview.sample.scene.video

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.xiaocydx.inputview.EditorMode
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.addAnimationCallback
import com.xiaocydx.inputview.sample.common.doOnTargetState
import com.xiaocydx.inputview.sample.common.isDispatchTouchEventEnabled
import com.xiaocydx.inputview.sample.common.matchParent
import com.xiaocydx.inputview.sample.common.wrapContent
import com.xiaocydx.inputview.sample.scene.transform.BoundsTransformation
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformation.ContainerState
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformationEnforcer
import com.xiaocydx.inputview.sample.scene.video.VideoEditor.Audio
import com.xiaocydx.inputview.sample.scene.video.VideoEditor.Image
import com.xiaocydx.inputview.sample.scene.video.VideoEditor.Text.Emoji
import com.xiaocydx.inputview.sample.scene.video.VideoEditor.Text.Input
import com.xiaocydx.inputview.sample.scene.video.VideoEditor.Text.Style
import com.xiaocydx.inputview.sample.scene.video.VideoEditor.Video

/**
 * @author xcc
 * @date 2024/4/13
 */
class VideoEditOverlay(private val activity: FragmentActivity) {
    private val inputView = InputView(activity)
    private val container = FrameLayout(activity)
    private val animator = FadeEditorAnimator(durationMillis = 300)
    private val adapter = VideoEditorAdapter(activity)
    private val transformationEnforcer = OverlayTransformationEnforcer(
        owner = activity, editorAnimator = animator, editorAdapter = adapter,
        stateProvider = { ContainerState(inputView, container) }
    )

    init {
        inputView.apply {
            editorAnimator = animator
            editorAdapter = adapter
            editorMode = EditorMode.ADJUST_PAN
            addView(container, matchParent, wrapContent)
            setEditorBackgroundColor(0xFF1D1D1D.toInt())
        }
        container.setBackgroundColor(0xFF1D1D1D.toInt())
    }

    fun notify(editor: VideoEditor?) {
        transformationEnforcer.notify(editor)
    }

    fun attachToWindow(preview: View) = apply {
        activity.lifecycle.doOnTargetState(Lifecycle.State.CREATED) {
            val contentParent = activity.findViewById<ViewGroup>(android.R.id.content)
            contentParent.addView(inputView, matchParent, matchParent)
            initEnforcer(preview)
        }
    }

    private fun initEnforcer(preview: View) {
        // 在动画运行时拦截触摸事件
        animator.addAnimationCallback(
            onStart = { activity.window.isDispatchTouchEventEnabled = false },
            onEnd = { activity.window.isDispatchTouchEventEnabled = true },
        )
        transformationEnforcer
            .add(BoundsTransformation())
            .add(PreviewTransformation(preview))
            .add(TextGroupTransformation(Input, Style, Emoji, notify = transformationEnforcer::notify))
            .add(CommonGroupTransformation(Video, Audio, Image, notify = transformationEnforcer::notify))
            .attach(inputView, activity.onBackPressedDispatcher)
    }
}