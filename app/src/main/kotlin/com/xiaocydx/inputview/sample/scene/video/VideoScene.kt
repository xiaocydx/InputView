package com.xiaocydx.inputview.sample.scene.video

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xiaocydx.inputview.sample.databinding.VideoCommonTitlebarBinding
import com.xiaocydx.inputview.sample.databinding.VideoTextTitlebarBinding
import com.xiaocydx.inputview.transform.Content
import com.xiaocydx.inputview.transform.ContentAdapter
import com.xiaocydx.inputview.transform.Overlay
import com.xiaocydx.inputview.transform.Scene

sealed class VideoContent : Content {
    data object Text : VideoContent()
    data object Common : VideoContent()
}

sealed class VideoScene(
    override val content: VideoContent,
    override val editor: VideoEditor
) : Scene<VideoContent, VideoEditor> {
    data object Input : VideoScene(VideoContent.Text, VideoEditor.Text.Input)
    data object Emoji : VideoScene(VideoContent.Text, VideoEditor.Text.Emoji)
    data object Style : VideoScene(VideoContent.Text, VideoEditor.Text.Style)
    data object Video : VideoScene(VideoContent.Common, VideoEditor.Video)
    data object Audio : VideoScene(VideoContent.Common, VideoEditor.Audio)
    data object Image : VideoScene(VideoContent.Common, VideoEditor.Image)
}

class VideoTitleAdapter : ContentAdapter<VideoContent>(), Overlay.Transform {

    override fun onCreateView(parent: ViewGroup, content: VideoContent): View {
        val view = when (content) {
            VideoContent.Common -> {
                val binding = VideoCommonTitlebarBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                binding.root
            }
            VideoContent.Text -> {
                val binding = VideoTextTitlebarBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                binding.root
            }
        }
        return view
    }
}