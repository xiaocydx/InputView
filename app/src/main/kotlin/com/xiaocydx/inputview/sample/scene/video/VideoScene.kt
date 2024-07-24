package com.xiaocydx.inputview.sample.scene.video

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xiaocydx.inputview.overlay.Content
import com.xiaocydx.inputview.overlay.ContentAdapter
import com.xiaocydx.inputview.overlay.Overlay
import com.xiaocydx.inputview.sample.databinding.VideoCommonTitlebarBinding
import com.xiaocydx.inputview.sample.databinding.VideoTextTitlebarBinding
import com.xiaocydx.inputview.sample.scene.video.transformer.ContentBoundsChange
import com.xiaocydx.inputview.sample.scene.video.transformer.ContentFadeChange
import com.xiaocydx.inputview.sample.scene.video.transformer.ContentTranslation

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

    override fun onCreateView(parent: ViewGroup, content: VideoTitle): View {
        val view = when (content) {
            VideoTitle.Common -> {
                val binding = VideoCommonTitlebarBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                binding.root
            }
            VideoTitle.Text -> {
                val binding = VideoTextTitlebarBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                binding.root
            }
        }
        view.transform().addTransformer(ContentTranslation(content))
        view.transform().addTransformer(ContentBoundsChange(content))
        view.transform().addTransformer(ContentFadeChange(content, children = true))
        return view
    }
}