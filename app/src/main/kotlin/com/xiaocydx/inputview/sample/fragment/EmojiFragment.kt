package com.xiaocydx.inputview.sample.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.xiaocydx.inputview.sample.message.EmojiRecyclerView

/**
 * @author xcc
 * @date 2023/10/12
 */
class EmojiFragment : Fragment() {
    @Suppress("PrivatePropertyName")
    private val TAG = javaClass.canonicalName
    private val viewModel: EditorViewModel by viewModels()

    /**
     * 设置`viewModel.viewId`，重建后恢复滚动位置
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = EmojiRecyclerView(requireContext())
        .apply { id = viewModel.viewId }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.e(TAG, "viewModel.count = ${viewModel.count}")
        viewModel.increase()
        viewLifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                Log.e(TAG, "viewLifecycle.currentState = ${source.lifecycle.currentState}")
            }
        })
    }
}