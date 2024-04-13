@file:Suppress("PrivatePropertyName")

package com.xiaocydx.inputview.sample.editor_adapter.fragment

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.FragmentEditorAdapter
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.sample.databinding.MessageListBinding
import com.xiaocydx.inputview.sample.basic.message.MessageEditor
import com.xiaocydx.inputview.sample.basic.message.init

/**
 * [FragmentEditorAdapter]的示例代码
 *
 * 当页面重建后，[FragmentEditorAdapter]会使用重建且可恢复状态的Fragment，
 * 关于重建的处理，可以看[FragmentEditorAdapter.onCreateFragment]的注释。
 *
 * @author xcc
 * @date 2023/10/11
 */
class FragmentEditorAdapterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InputView.init(window, gestureNavBarEdgeToEdge = true)
        val binding = MessageListBinding.inflate(layoutInflater)
        binding.tvTitle.setBackgroundColor(0xFF79AA91.toInt())
        val editorAdapter = MessageFragmentEditorAdapter(this)
        setContentView(binding.init(window, editorAdapter).root)
    }

    private class MessageFragmentEditorAdapter(
        fragmentActivity: FragmentActivity
    ) : FragmentEditorAdapter<MessageEditor>(fragmentActivity) {
        override val ime = MessageEditor.IME

        /**
         * 重建的Fragment会恢复[Editor]视图的状态，例如恢复列表滚动位置，
         * 配合重写[getStatefulEditorList]，即可恢复[Editor]的完整状态。
         *
         * 按home键将应用退到后台，输入adb shell am kill com.xiaocydx.inputview.sample命令杀掉进程。
         */
        // override fun getStatefulEditorList(): List<MessageEditor> {
        //     return MessageEditor.values().toList()
        // }

        override fun getEditorKey(editor: MessageEditor) = editor.name

        override fun onCreateFragment(editor: MessageEditor): Fragment? = when (editor) {
            MessageEditor.IME, MessageEditor.VOICE -> null
            MessageEditor.EMOJI -> EmojiFragment()
            MessageEditor.EXTRA -> ExtraFragment()
        }
    }
}