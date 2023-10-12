@file:Suppress("PrivatePropertyName")

package com.xiaocydx.inputview.fragment

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.xiaocydx.inputview.FragmentEditorAdapter
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.MessageEditor
import com.xiaocydx.inputview.databinding.MessageListBinding
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.notifyShow

/**
 * @author xcc
 * @date 2023/10/11
 */
class FragmentEditorAdapterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InputView.init(window, gestureNavBarEdgeToEdge = true)
        val binding = MessageListBinding.inflate(layoutInflater)
        val editorAdapter = MessageFragmentEditorAdapter(this)
        setContentView(binding.init(window, editorAdapter).root)
        if (savedInstanceState != null) {
            editorAdapter.notifyShow(MessageEditor.IME)
        }
    }

    private class MessageFragmentEditorAdapter(
        fragmentActivity: FragmentActivity
    ) : FragmentEditorAdapter<MessageEditor>(fragmentActivity) {
        override val ime = MessageEditor.IME

        override fun getEditorKey(editor: MessageEditor) = editor.name

        override fun onCreateFragment(editor: MessageEditor): Fragment? = when (editor) {
            MessageEditor.IME, MessageEditor.VOICE -> null
            MessageEditor.EMOJI -> EmojiFragment()
            MessageEditor.EXTRA -> ExtraFragment()
        }
    }
}