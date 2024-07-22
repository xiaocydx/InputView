package com.xiaocydx.inputview.sample.scene.video

import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.overlay.Content
import com.xiaocydx.inputview.overlay.ContentAdapter
import com.xiaocydx.inputview.overlay.OverlayEnforcer
import com.xiaocydx.inputview.overlay.OverlayScene
import com.xiaocydx.inputview.overlay.OverlayTransformer
import com.xiaocydx.inputview.overlay.PrepareState
import com.xiaocydx.inputview.overlay.TransformState
import com.xiaocydx.inputview.sample.common.onClick

/**
 * @author xcc
 * @date 2024/7/22
 */
class VideoEditOverlayV2(
    private val window: Window,
    private val lifecycleOwner: LifecycleOwner,
    private val fragmentManager: FragmentManager
) {
    private val overlayEnforcer = OverlayEnforcer(
        window = window,
        lifecycleOwner = lifecycleOwner,
        contentAdapter = VideoTitleAdapter(),
        editorAdapter = VideoEditorAdapter(lifecycleOwner.lifecycle, fragmentManager)
    )

    fun notify(scene: VideoScene?) {
        overlayEnforcer.notify(scene)
    }

    fun attachToWindow() = apply {
        overlayEnforcer.attachToWindow {
            it.editorAnimator = FadeEditorAnimator(durationMillis = 300)
            it.setEditorBackgroundColor(0xFF1D1D1D.toInt())
        }
        overlayEnforcer.addTransformer(Test { notify(null) })
    }
}

class Test(val notify: () -> Unit) : OverlayTransformer {

    override fun onEnd(state: TransformState) {
        if (state.current != null) {
            state.container.onClick { notify() }
        }  else {
            state.container.setOnClickListener(null)
        }
    }
}

sealed class VideoTitle : Content {
    data object Text : VideoTitle()
    data object Common : VideoTitle()
}

sealed class VideoScene(
    override val content: VideoTitle,
    override val editor: VideoEditor
) : OverlayScene<VideoTitle, VideoEditor> {
    data object Input : VideoScene(VideoTitle.Text, VideoEditor.Text.Input)
    data object Emoji : VideoScene(VideoTitle.Text, VideoEditor.Text.Emoji)
    data object Style : VideoScene(VideoTitle.Text, VideoEditor.Text.Style)
    data object Video : VideoScene(VideoTitle.Common, VideoEditor.Video)
    data object Audio : VideoScene(VideoTitle.Common, VideoEditor.Audio)
    data object Image : VideoScene(VideoTitle.Common, VideoEditor.Image)
}

class VideoTitleAdapter : ContentAdapter<VideoTitle>() {
    override fun onCreateView(parent: ViewGroup, content: VideoTitle) = null
}