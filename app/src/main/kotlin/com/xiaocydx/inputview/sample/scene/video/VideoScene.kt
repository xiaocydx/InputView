package com.xiaocydx.inputview.sample.scene.video

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
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
import com.xiaocydx.inputview.transform.Content
import com.xiaocydx.inputview.transform.ContentAdapter
import com.xiaocydx.inputview.transform.ImperfectState
import com.xiaocydx.inputview.transform.Overlay
import com.xiaocydx.inputview.transform.Scene
import com.xiaocydx.inputview.transform.Transformer
import com.xiaocydx.inputview.transform.isCurrent
import com.xiaocydx.inputview.transform.isPrevious

sealed class VideoScene(
    override val content: VideoContent,
    override val editor: VideoEditor
) : Scene<VideoContent, VideoEditor> {
    data object InputText : VideoScene(VideoContent.Text, VideoEditor.Ime)
    data object InputEmoji : VideoScene(VideoContent.Text, VideoEditor.Emoji)
    data object SelectStyle : VideoScene(VideoContent.Text, VideoEditor.Style)
    data object SelectVideo : VideoScene(VideoContent.Title, VideoEditor.Video)
    data object SelectAudio : VideoScene(VideoContent.Title, VideoEditor.Audio)
    data object SelectImage : VideoScene(VideoContent.Title, VideoEditor.Image)
}

sealed class VideoContent : Content {
    data object Text : VideoContent()
    data object Title : VideoContent()
}

sealed class VideoEditor(val desc: String, val size: Int) : Editor {
    data object Ime : VideoEditor(desc = "文字输入", size = WRAP_CONTENT)
    data object Emoji : VideoEditor(desc = "文字表情", size = WRAP_CONTENT)
    data object Style : VideoEditor(desc = "文字样式", size = 250)
    data object Video : VideoEditor(desc = "视频", size = 300)
    data object Audio : VideoEditor(desc = "音频", size = 250)
    data object Image : VideoEditor(desc = "图片", size = 250)
}

class VideoEditorAdapter(
    lifecycle: Lifecycle,
    fragmentManager: FragmentManager
) : FragmentEditorAdapter<VideoEditor>(lifecycle, fragmentManager) {
    override val ime = VideoEditor.Ime

    override fun getEditorKey(editor: VideoEditor) = editor.desc

    override fun onCreateFragment(editor: VideoEditor): Fragment {
        if (editor == VideoEditor.Emoji) return EmojiFragment()
        return CommonFragment.newInstance(editor.desc, editor.size)
    }
}

class VideoContentAdapter : ContentAdapter<VideoContent>(), Overlay.Transform {
    var go: ((VideoScene?) -> Boolean)? = null

    override fun onCreateView(parent: ViewGroup, content: VideoContent): View {
        val view = when (content) {
            VideoContent.Title -> VideoTitleBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ).run {
                tvConfirm.onClick { go?.invoke(null) }
                root.transform().addTransformer(TitleChange(tvTitle))
                root
            }

            VideoContent.Text -> VideoTextBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ).run {
                tvConfirm.onClick { go?.invoke(null) }
                tvEmoji.onClick { go?.invoke(VideoScene.InputEmoji) }
                tvStyle.onClick { go?.invoke(VideoScene.SelectStyle) }
                root.transform().addTransformer(EditTextChange(editText))
                root
            }
        }
        return view
    }

    private class TitleChange(private val tvTitle: TextView) : Transformer() {

        override fun match(state: ImperfectState): Boolean {
            return state.isCurrent(VideoContent.Title)
        }

        override fun onPrepare(state: ImperfectState) {
            val editor = state.current?.editor as? VideoEditor ?: return
            tvTitle.text = editor.desc
        }
    }

    private class EditTextChange(private val editText: EditText) : Transformer() {
        private val editor = VideoEditor.Ime

        override fun match(state: ImperfectState) = with(state) {
            isPrevious(editor) || isCurrent(editor)
        }

        override fun onPrepare(state: ImperfectState) = with(state) {
            when {
                isPrevious(editor) -> inputView.editText = null
                isCurrent(editor) -> inputView.editText = editText
            }
            if (isCurrent(editor)) editText.requestFocus()
        }
    }
}