package com.xiaocydx.inputview.sample.editor_animator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updatePadding
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.Insets.Decor
import com.xiaocydx.inputview.addAnimationCallback
import com.xiaocydx.inputview.addEditText
import com.xiaocydx.inputview.animator
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.databinding.ActivityImeAnimatorBinding
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.navigationBars

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
        InputView.init(window, Decor(gestureNavBarEdgeToEdge = true))
        setContentView(ActivityImeAnimatorBinding.inflate(layoutInflater).init().root)
    }

    private fun ActivityImeAnimatorBinding.init() = apply {
        val animator = InputView.animator(etContainer)

        // 在初始化Window时，gestureNavBarEdgeToEdge = true，
        // 第4点是支持手势导航栏EdgeToEdge的代码，避免底部被遮挡。

        // 1. 当有多个EditText时，选其中一个EditText跟ImeAnimator关联即可,
        // 多个EditText的焦点处理逻辑，可以看ImeAnimator.editText的注释。
        animator.editText = editText1

        // 2. 创建animator的EditText会自动处理水滴状指示器导致动画卡顿问题，
        // 若其它EditText也需要处理，则调用InputView.addEditText()完成添加。
        InputView.addEditText(editText2)

        // 3. 点击imageView，隐藏IME
        imageView.onClick(animator::hideIme)

        // 4. 当支持手势导航栏EdgeToEdge时，设置etContainer.paddingBottom
        etContainer.insets().paddings(navigationBars())

        // 5. 显示和隐藏IME，运行动画设置root.paddingBottom
        animator.addAnimationCallback(onUpdate = { state ->
            val bottom = state.currentOffset - etContainer.paddingBottom
            root.updatePadding(bottom = bottom.coerceAtLeast(0))
        })

        // 5. 碰到AnimationInterceptorActivity1的多Window交互问题，仍可以通过动画拦截器解决
        // animator.setWindowFocusInterceptor()
        // imageView.onClick { InputDialog(this@ImeAnimatorActivity).show() }
    }
}