@file:Suppress("ConstPropertyName")

package com.xiaocydx.inputview.sample.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.view.WindowInsets
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.sample.R
import com.xiaocydx.inputview.sample.databinding.MessageListBinding
import com.xiaocydx.inputview.sample.message.init

private const val statusBarEdgeToEdge = true
private const val gestureNavBarEdgeToEdge = true

/**
 * [R.style.MessageListDialog]最重要的是`windowIsFloating = false`，这能让Dialog的视图树有[WindowInsets]分发，
 * 例如[BottomSheetDialog]的默认主题包含`windowIsFloating = false`，布局文件是`design_bottom_sheet_dialog.xml`
 * id为`coordinator`和`design_bottom_sheet`的View处理[WindowInsets]，id为`touch_outside`的View实现`canceledOnTouchOutside`。
 */
class MessageListDialog(
    context: Context,
    @StyleRes theme: Int = R.style.MessageListDialog
) : AppCompatDialog(context, theme) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val window = window!!
        val binding = MessageListBinding.inflate(layoutInflater)
        InputView.init(window, statusBarEdgeToEdge, gestureNavBarEdgeToEdge)
        setContentView(binding.init(window).initView(window).root)
    }

    private fun MessageListBinding.initView(window: Window) = apply {
        tvTitle.setBackgroundColor(0xFFD5A7AE.toInt())
        if (!statusBarEdgeToEdge) return@apply
        tvTitle.doOnAttach {
            val rootInsets = ViewCompat.getRootWindowInsets(it) ?: return@doOnAttach
            val statusBars = rootInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            tvTitle.updatePadding(top = statusBars.top)
            tvTitle.updateLayoutParams { height += statusBars.top }
        }
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
    }
}

class MessageListDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MessageListDialog(requireContext(), R.style.MessageListDialog)
    }
}