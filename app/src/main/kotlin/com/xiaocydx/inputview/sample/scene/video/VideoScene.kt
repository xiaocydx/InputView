package com.xiaocydx.inputview.sample.scene.video

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.FragmentEditorAdapter
import com.xiaocydx.inputview.sample.databinding.VideoCommonTitlebarBinding
import com.xiaocydx.inputview.sample.databinding.VideoTextTitlebarBinding
import com.xiaocydx.inputview.sample.editor_adapter.fragment.EmojiFragment
import com.xiaocydx.inputview.transform.Content
import com.xiaocydx.inputview.transform.ContentAdapter
import com.xiaocydx.inputview.transform.Overlay
import com.xiaocydx.inputview.transform.Scene

sealed class VideoScene(
    override val content: VideoContent,
    override val editor: VideoEditor
) : Scene<VideoContent, VideoEditor> {
    data object Input : VideoScene(VideoContent.Text, VideoEditor.Input)
    data object Emoji : VideoScene(VideoContent.Text, VideoEditor.Emoji)
    data object Style : VideoScene(VideoContent.Text, VideoEditor.Style)
    data object Video : VideoScene(VideoContent.Common, VideoEditor.Video)
    data object Audio : VideoScene(VideoContent.Common, VideoEditor.Audio)
    data object Image : VideoScene(VideoContent.Common, VideoEditor.Image)
}

sealed class VideoEditor(val title: String, val size: Int) : Editor {
    data object Input : VideoEditor(title = "文字输入", size = ViewGroup.LayoutParams.WRAP_CONTENT)
    data object Emoji : VideoEditor(title = "文字表情", size = ViewGroup.LayoutParams.WRAP_CONTENT)
    data object Style : VideoEditor(title = "文字样式", size = 250)
    data object Video : VideoEditor(title = "视频", size = 300)
    data object Audio : VideoEditor(title = "音频", size = 250)
    data object Image : VideoEditor(title = "图片", size = 250)
}

class VideoEditorAdapter(
    lifecycle: Lifecycle,
    fragmentManager: FragmentManager
) : FragmentEditorAdapter<VideoEditor>(lifecycle, fragmentManager) {
    override val ime = VideoEditor.Input

    override fun getEditorKey(editor: VideoEditor) = editor.title

    override fun onCreateFragment(editor: VideoEditor): Fragment {
        if (editor == VideoEditor.Emoji) return EmojiFragment()
        return CommonFragment.newInstance(editor.title, editor.size)
    }
}

sealed class VideoContent : Content {
    data object Text : VideoContent()
    data object Common : VideoContent()
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