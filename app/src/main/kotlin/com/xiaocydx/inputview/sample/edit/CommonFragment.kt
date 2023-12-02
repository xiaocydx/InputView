package com.xiaocydx.inputview.sample.edit

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.xiaocydx.inputview.EdgeToEdgeHelper
import com.xiaocydx.inputview.sample.dp
import com.xiaocydx.inputview.sample.layoutParams
import com.xiaocydx.inputview.sample.matchParent

/**
 * 动画结束时，生命周期状态才转换为[RESUMED]
 *
 * @author xcc
 * @date 2023/12/1
 */
class CommonFragment : Fragment() {

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = AppCompatTextView(requireContext()).apply {
        val size = arguments?.getInt(KEY_SIZE) ?: 0
        text = "${arguments?.getString(KEY_TITLE) ?: ""}素材"
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, 18.dp.toFloat())
        layoutParams(matchParent, if (size > 0) size.dp else size)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = EdgeToEdgeHelper {
        view.doOnApplyWindowInsets { view, insets, initialState ->
            val supportGestureNavBarEdgeToEdge = insets.supportGestureNavBarEdgeToEdge(view)
            val bottom = if (supportGestureNavBarEdgeToEdge) insets.navigationBarHeight else 0
            view.updatePadding(bottom = initialState.paddings.bottom + bottom)
        }

        viewLifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            val title = arguments?.getString(KEY_TITLE) ?: ""
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                println("test -> CommonFragment $title state = ${source.lifecycle.currentState}")
            }
        })
    }

    companion object {
        private const val KEY_TITLE = "KET_TITLE"
        private const val KEY_SIZE = "KEY_SIZE"

        fun newInstance(title: String, size: Int) = CommonFragment().apply {
            arguments = bundleOf(KEY_TITLE to title, KEY_SIZE to size)
        }
    }
}