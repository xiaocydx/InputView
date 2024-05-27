package com.xiaocydx.inputview.sample.scene.viewpager2

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle.State.RESUMED
import com.xiaocydx.inputview.AnimationInterceptor
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.sample.basic.message.MessageEditor.IME
import com.xiaocydx.inputview.sample.basic.message.init
import com.xiaocydx.inputview.sample.common.viewLifecycle
import com.xiaocydx.inputview.sample.databinding.MessageListBinding
import com.xiaocydx.inputview.sample.editor_adapter.StatefulMessageEditorAdapter

/**
 * @author xcc
 * @date 2024/5/27
 */
class MessageListFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Fragment重建时，恢复之前显示的Editor
        val adapter = StatefulMessageEditorAdapter()
        return MessageListBinding.inflate(layoutInflater, container, false)
            .init(window = requireActivity().window, editorAdapter = adapter).intercept().root
    }

    @SuppressLint("SetTextI18n")
    private fun MessageListBinding.intercept() = apply {
        val num = arguments?.getInt(KEY_NUM) ?: 0
        tvTitle.text = "消息列表-$num"
        inputView.editorAnimator.setAnimationInterceptor(object : AnimationInterceptor {
            override fun onInterceptChange(current: Editor?, next: Editor?): Boolean {
                // 显示IME时，若当前生命周期状态不是RESUMED，则不允许Editor更改为IME
                return next === IME && viewLifecycle.currentState !== RESUMED
            }
        })
    }

    companion object {
        private const val KEY_NUM = "KEY_NUM"

        fun newInstance(num: Int) = MessageListFragment()
            .apply { arguments = bundleOf(KEY_NUM to num) }
    }
}