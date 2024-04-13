package com.xiaocydx.inputview.sample.editor_adapter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.inputview.AnimationCallback
import com.xiaocydx.inputview.AnimationInterceptor
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.EditorAdapter
import com.xiaocydx.inputview.EditorChangedListener
import com.xiaocydx.inputview.FragmentEditorAdapter
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.notifyShow
import com.xiaocydx.inputview.sample.basic.message.MessageEditor
import com.xiaocydx.inputview.sample.basic.message.MessageEditorAdapter
import com.xiaocydx.inputview.sample.basic.message.init
import com.xiaocydx.inputview.sample.databinding.MessageListBinding

/**
 * [InputView]和[EditorAdapter]的Stateful功能
 *
 * @author xcc
 * @date 2024/4/13
 */
class StatefulActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InputView.init(window, gestureNavBarEdgeToEdge = true)
        val adapter = StatefulMessageEditorAdapter()
        setContentView(MessageListBinding.inflate(layoutInflater).init(window, adapter).root)
        // 即使不重写getStatefulEditorList()，在页面重建期间通知显示Editor，也不会运行动画
        // adapter.notifyShow(MessageEditor.EMOJI)
    }

    private class StatefulMessageEditorAdapter : MessageEditorAdapter() {

        /**
         * 在保存和恢复显示的[Editor]时，会调用该函数获取可保存显示状态的[Editor]集合，
         * 恢复显示的[Editor]，不会运行动画，仅记录动画状态，分发动画回调，在恢复的过程中，
         * 会调用[EditorChangedListener]、[AnimationCallback]、[AnimationInterceptor]。
         *
         * **注意**：
         * 1. 重写该函数只会恢复显示的[Editor]，不会恢复[Editor]视图的状态，
         * 若需要恢复[Editor]视图的状态，则可以使用[FragmentEditorAdapter]。
         * 2. 即使不重写该函数，在页面重建期间通知显示[Editor]，也不会运行动画，
         * 可以类比Fragment首次创建有过渡动画，重建的Fragment不会运行过渡动画。
         *
         * 按home键将应用退到后台，输入adb shell am kill com.xiaocydx.inputview.sample命令杀掉进程。
         */
        override fun getStatefulEditorList(): List<MessageEditor> {
            return MessageEditor.values().toList()
        }
    }
}