package com.xiaocydx.inputview.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.xiaocydx.inputview.R

/**
 * @author xcc
 * @date 2023/10/12
 */
class ExtraFragment : Fragment(R.layout.message_editor_extra) {
    @Suppress("PrivatePropertyName")
    private val TAG = javaClass.canonicalName
    private val viewModel: EditorViewModel by viewModels()

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