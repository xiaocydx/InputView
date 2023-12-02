package com.xiaocydx.inputview.sample.edit

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.xiaocydx.inputview.FragmentEditorAdapter
import com.xiaocydx.inputview.notifyHideCurrent
import com.xiaocydx.inputview.notifyShow
import com.xiaocydx.inputview.sample.fragment.EmojiFragment

/**
 * @author xcc
 * @date 2023/12/1
 */
class VideoEditorAdapter(
    fragmentActivity: FragmentActivity
) : FragmentEditorAdapter<VideoEditor>(fragmentActivity) {
    override val ime = VideoEditor.Text.Input

    override fun getEditorKey(editor: VideoEditor) = editor.title

    override fun onCreateFragment(editor: VideoEditor): Fragment {
        if (editor == VideoEditor.Text.Emoji) return EmojiFragment()
        return CommonFragment.newInstance(editor.title, editor.size)
    }

    fun notifyShowOrHide(current: VideoEditor?) {
        if (current != null) notifyShow(current) else notifyHideCurrent()
    }
}