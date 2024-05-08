package com.xiaocydx.inputview.sample.basic

import android.os.Bundle
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.initCompat
import com.xiaocydx.inputview.sample.basic.message.init
import com.xiaocydx.inputview.sample.databinding.MessageListBinding
import com.xiaocydx.insets.systembar.EdgeToEdge
import com.xiaocydx.insets.systembar.SystemBar
import com.xiaocydx.insets.systembar.systemBarController

/**
 * `InputView.initCompat()`的示例代码
 *
 * [SystemBar]是一套单Activity多Fragment的SystemBar控制方案，
 * [SystemBar]的注入逻辑初始化了window，完成[WindowInsets]的处理，
 * `InputView.initCompat()`用于兼容已有的[WindowInsets]处理方案。
 *
 * @author xcc
 * @date 2023/12/31
 */
class InitCompatActivity : AppCompatActivity(), SystemBar {
    init {
        systemBarController {
            statusBarColor = 0xFFA2B4C0.toInt()
            navigationBarEdgeToEdge = EdgeToEdge.Gesture
            isAppearanceLightStatusBar = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // initCompat()不处理WindowInsets，仅完成window的初始化
        InputView.initCompat(window, gestureNavBarEdgeToEdge = true)
        setContentView(MessageListBinding.inflate(layoutInflater).init(window).root)
    }
}