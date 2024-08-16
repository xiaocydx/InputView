package com.xiaocydx.inputview.sample.scene.video

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.FragmentEditorAdapter
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.databinding.VideoTextBinding
import com.xiaocydx.inputview.sample.databinding.VideoTitleBinding
import com.xiaocydx.inputview.sample.editor_adapter.fragment.EmojiFragment
import com.xiaocydx.inputview.sample.scene.video.VideoContent.Text
import com.xiaocydx.inputview.sample.scene.video.VideoContent.Title
import com.xiaocydx.inputview.sample.scene.video.VideoEditor.Audio
import com.xiaocydx.inputview.sample.scene.video.VideoEditor.Emoji
import com.xiaocydx.inputview.sample.scene.video.VideoEditor.Image
import com.xiaocydx.inputview.sample.scene.video.VideoEditor.Ime
import com.xiaocydx.inputview.sample.scene.video.VideoEditor.Style
import com.xiaocydx.inputview.sample.scene.video.VideoEditor.Video
import com.xiaocydx.inputview.transform.Content
import com.xiaocydx.inputview.transform.ContentAdapter
import com.xiaocydx.inputview.transform.ContentChangeEditText
import com.xiaocydx.inputview.transform.ImperfectState
import com.xiaocydx.inputview.transform.Overlay
import com.xiaocydx.inputview.transform.Scene
import com.xiaocydx.inputview.transform.Transformer
import com.xiaocydx.inputview.transform.isCurrent

enum class VideoScene(
    override val content: VideoContent,
    override val editor: VideoEditor
) : Scene<VideoContent, VideoEditor> {
    InputText(Text, Ime),
    InputEmoji(Text, Emoji),
    SelectStyle(Text, Style),
    SelectVideo(Title, Video),
    SelectAudio(Title, Audio),
    SelectImage(Title, Image),
}

enum class VideoContent : Content {
    Text, Title
}

enum class VideoEditor(val desc: String, val size: Int) : Editor {
    Ime(desc = "文字输入", size = WRAP_CONTENT),
    Emoji(desc = "文字表情", size = WRAP_CONTENT),
    Style(desc = "文字样式", size = 250),
    Video(desc = "视频", size = 300),
    Audio(desc = "音频", size = 250),
    Image(desc = "图片", size = 250),
}

class VideoContentAdapter(
    private val go: (VideoScene?) -> Boolean
) : ContentAdapter<VideoContent>(), Overlay.Transform {

    override fun onCreateView(parent: ViewGroup, content: VideoContent): View {
        val view = when (content) {
            Title -> VideoTitleBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ).run {
                tvConfirm.onClick { go(null) }
                root.transform().add(ContentChangeTitle(tvTitle))
                root
            }

            Text -> VideoTextBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ).run {
                tvConfirm.onClick { go(null) }
                tvEmoji.onClick { go(VideoScene.InputEmoji) }
                tvStyle.onClick { go(VideoScene.SelectStyle) }
                root.transform().add(ContentChangeEditText(editText, Text))
                root
            }
        }
        return view
    }

    private class ContentChangeTitle(private val tvTitle: TextView) : Transformer() {

        override fun match(state: ImperfectState) = state.isCurrent(Title)

        override fun onPrepare(state: ImperfectState) {
            val editor = state.current?.editor as? VideoEditor ?: return
            tvTitle.text = editor.desc
        }
    }
}

class VideoEditorAdapter(
    lifecycle: Lifecycle,
    fragmentManager: FragmentManager
) : FragmentEditorAdapter<VideoEditor>(lifecycle, fragmentManager) {
    override val ime = Ime

    override fun getEditorKey(editor: VideoEditor) = editor.desc

    override fun onCreateFragment(editor: VideoEditor): Fragment {
        if (editor == Emoji) return EmojiFragment()
        return CommonFragment.newInstance(editor.desc, editor.size)
    }
}