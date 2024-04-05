package com.xiaocydx.inputview.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updatePadding
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.addAnimationCallback
import com.xiaocydx.inputview.addEditText
import com.xiaocydx.inputview.animator
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.sample.databinding.ActivityImeAnimatorBinding
import com.xiaocydx.insets.insets

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
        // 1. 当有多个EditText时，选其中一个EditText创建ImeAnimator即可,
        // 多个EditText的焦点处理逻辑，可以看InputView.animator()的注释。
        val animator = InputView.animator(window, editText1)

        // 2. 创建animator的EditText会自动处理水滴状指示器导致动画卡顿问题，
        // 若其它EditText也需要处理，则调用InputView.addEditText()完成添加。
        InputView.addEditText(window, editText2)

        // 3. 点击imageView，隐藏IME
        imageView.onClick(animator::hideIme)

        // 4. 当支持手势导航栏EdgeToEdge时，设置etContainer.paddingBottom
        etContainer.insets().gestureNavBarEdgeToEdge()

        // 5. 显示和隐藏IME，运行动画设置root.paddingBottom
        animator.addAnimationCallback(onUpdate = { state ->
            val bottom = state.currentOffset - state.navBarOffset
            root.updatePadding(bottom = bottom.coerceAtLeast(0))
        })

        // 5. 碰到OverlayInputActivity的多Window交互问题，仍可以通过动画拦截器解决
        // animator.setWindowFocusInterceptor()
        // imageView.onClick { InputDialog(this@ImeAnimatorActivity).show() }
    }
}