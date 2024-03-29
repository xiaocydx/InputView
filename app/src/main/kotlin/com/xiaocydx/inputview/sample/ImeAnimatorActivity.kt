package com.xiaocydx.inputview.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updatePadding
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.addAnimationCallback
import com.xiaocydx.inputview.animator
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.sample.databinding.ActivityImeAnimatorBinding
import com.xiaocydx.insets.handleGestureNavBarEdgeToEdgeOnApply

/**
 * `InputView.animator()`的示例代码
 *
 * `InputView.animator()`复用[InputView]的动画实现，不涉及跟动画无关的功能。
 *
 * @author xcc
 * @date 2023/12/1
 */
class ImeAnimatorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InputView.init(window, gestureNavBarEdgeToEdge = true)
        setContentView(ActivityImeAnimatorBinding.inflate(layoutInflater).init().root)
    }

    private fun ActivityImeAnimatorBinding.init() = apply {
        val animator = InputView.animator(window, editText)
        // 1. 点击imageView，隐藏IME
        imageView.onClick(animator::hideIme)
        // 2. 当支持手势导航栏EdgeToEdge时，设置etContainer.paddingBottom
        etContainer.handleGestureNavBarEdgeToEdgeOnApply()
        // 3. 显示和隐藏IME，运行动画设置root.paddingBottom
        animator.addAnimationCallback(onUpdate = { state ->
            val bottom = state.currentOffset - state.navBarOffset
            root.updatePadding(bottom = bottom.coerceAtLeast(0))
        })
    }
}