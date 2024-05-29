@file:Suppress("ConstPropertyName")

package com.xiaocydx.inputview.sample.basic.dialog

import android.content.Context
import android.os.Bundle
import android.view.Window
import android.view.WindowInsets
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDialog
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.sample.R
import com.xiaocydx.inputview.sample.basic.message.init
import com.xiaocydx.inputview.sample.databinding.MessageListBinding
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.statusBars

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
        tvTitle.insets().paddings(statusBars()).dimension(statusBars())
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
    }
}