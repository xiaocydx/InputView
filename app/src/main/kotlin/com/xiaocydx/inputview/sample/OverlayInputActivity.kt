package com.xiaocydx.inputview.sample

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.WindowInsets
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.isInvisible
import com.xiaocydx.inputview.AnimationInterceptor
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.EditorAnimator
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.addAnimationCallback
import com.xiaocydx.inputview.addEditText
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.notifyHideCurrent
import com.xiaocydx.inputview.sample.databinding.ActivityOverlayInputBinding
import com.xiaocydx.inputview.setWindowFocusInterceptor

/**
 * 覆盖输入的示例代码
 *
 * @author xcc
 * @date 2023/10/15
 */
class OverlayInputActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InputView.init(window, gestureNavBarEdgeToEdge = true)
        setContentView(ActivityOverlayInputBinding.inflate(layoutInflater).init().root)
    }

    private fun ActivityOverlayInputBinding.init() = apply {
        // 1. 当有多个EditText时，选其中一个EditText跟InputView关联即可，
        // 多个EditText的焦点处理逻辑，可以看InputView.editText的注释。
        inputView.editText = editText1

        // 2. 关联InputView的EditText会自动处理水滴状指示器导致动画卡顿问题，
        // 若其它EditText也需要处理，则调用InputView.addEditText()完成添加。
        InputView.addEditText(window, editText2)

        // 3. 运行动画时，修改outside.alpha和outside.isInvisible
        inputView.editorAnimator.addAnimationCallback(
            onStart = { outside.isInvisible = false },
            onEnd = { outside.isInvisible = it.endOffset == 0 },
            onUpdate = {
                // 更改IME高度运行的动画，不需要改变outside.alpha
                val fraction = if (it.startOffset > 0 && it.endOffset > 0) 1f else it.animatedFraction
                outside.alpha = if (it.endOffset > 0) fraction else 1 - fraction
            }
        )

        // 4. 设置window.decorView.hasWindowFocus()的动画拦截器，
        // 点击Dialog的EditText显示IME，不更改Editor且不运行动画。
        inputView.editorAnimator.setWindowFocusInterceptor()
        inputView.setEditorBackgroundColor(0xFFF2F2F2.toInt())

        outside.onClick { inputView.editorAdapter.notifyHideCurrent() }
        textView.onClick { InputDialog(this@OverlayInputActivity).show() }
    }
}

/**
 * 点击[Dialog]的[EditText]显示IME，[InputView]所在的Window视图树会分发[WindowInsets]，
 * 这会导致[Editor]更改为IME，运行IME动画，[EditorAnimator.setWindowFocusInterceptor]
 * 是解决这类问题的便捷函数，若有更多的拦截条件，则自行实现和组合[AnimationInterceptor]。
 */
class InputDialog(context: Context) : AppCompatDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val editText = AppCompatEditText(context)
        editText.hint = "点击显示IME"
        editText.layoutParams(200.dp, 50.dp)
        setContentView(editText)
    }
}