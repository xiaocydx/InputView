package com.xiaocydx.inputview.sample.scene.video

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xiaocydx.inputview.overlay.Content
import com.xiaocydx.inputview.overlay.ContentAdapter
import com.xiaocydx.inputview.overlay.Overlay
import com.xiaocydx.inputview.overlay.PrepareState
import com.xiaocydx.inputview.overlay.TransformState
import com.xiaocydx.inputview.overlay.Transformer
import com.xiaocydx.inputview.sample.common.dp
import com.xiaocydx.inputview.sample.common.layoutParams
import com.xiaocydx.inputview.sample.common.matchParent
import com.xiaocydx.inputview.sample.databinding.VideoCommonTitlebarBinding

sealed class VideoTitle : Content {
    data object Text : VideoTitle()
    data object Common : VideoTitle()
}

sealed class VideoScene(
    override val content: VideoTitle,
    override val editor: VideoEditor
) : Overlay.Scene<VideoTitle, VideoEditor> {
    data object Input : VideoScene(VideoTitle.Text, VideoEditor.Text.Input)
    data object Emoji : VideoScene(VideoTitle.Text, VideoEditor.Text.Emoji)
    data object Style : VideoScene(VideoTitle.Text, VideoEditor.Text.Style)
    data object Video : VideoScene(VideoTitle.Common, VideoEditor.Video)
    data object Audio : VideoScene(VideoTitle.Common, VideoEditor.Audio)
    data object Image : VideoScene(VideoTitle.Common, VideoEditor.Image)
}

class VideoTitleAdapter : ContentAdapter<VideoTitle>(), Overlay.Transform {

    override fun onCreateView(parent: ViewGroup, content: VideoTitle) = when (content) {
        VideoTitle.Common -> {
            val binding = VideoCommonTitlebarBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            binding.root.transform().addTransformer(HH())
            binding.root
        }
        VideoTitle.Text -> {
            val view = View(parent.context)
            view.layoutParams(matchParent, 300.dp)
            view.setBackgroundColor(Color.RED)
            view
        }
    }
}

class HH : Transformer {

    override fun onPrepare(state: PrepareState) {
        state
    }

    override fun onStart(state: TransformState) {
        state
    }

    override fun onEnd(state: TransformState) {
        state
    }
}