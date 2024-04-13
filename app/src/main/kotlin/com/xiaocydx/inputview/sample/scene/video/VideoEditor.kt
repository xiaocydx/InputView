package com.xiaocydx.inputview.sample.scene.video

import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.FragmentEditorAdapter
import com.xiaocydx.inputview.sample.editor_adapter.fragment.EmojiFragment

/**
 * @author xcc
 * @date 2023/12/1
 */
sealed class VideoEditor(val title: String, val size: Int) : Editor {
    sealed class Text(title: String, size: Int) : VideoEditor(title, size) {
        data object Input : Text(title = "文字输入", size = WRAP_CONTENT)
        data object Emoji : Text(title = "文字表情", size = WRAP_CONTENT)
        data object Style : Text(title = "文字样式", size = 250)
    }

    data object Video : VideoEditor(title = "视频", size = 300)
    data object Audio : VideoEditor(title = "音频", size = 250)
    data object Image : VideoEditor(title = "图片", size = 250)
}

class VideoEditorAdapter(
    fragmentActivity: FragmentActivity
) : FragmentEditorAdapter<VideoEditor>(fragmentActivity) {
    override val ime = VideoEditor.Text.Input

    override fun getEditorKey(editor: VideoEditor) = editor.title

    override fun onCreateFragment(editor: VideoEditor): Fragment {
        if (editor == VideoEditor.Text.Emoji) return EmojiFragment()
        return CommonFragment.newInstance(editor.title, editor.size)
    }
}