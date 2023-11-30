package com.xiaocydx.inputview.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.addAnimationCallback
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.notifyHideIme
import com.xiaocydx.inputview.sample.databinding.ActivityOverlayInputBinding

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
        inputView.editText = editText
        inputView.editorAnimator.addAnimationCallback(
            onStart = { outside.isInvisible = false },
            onEnd = { outside.isInvisible = it.endOffset == 0 },
            onUpdate = {
                val fraction = it.animatedFraction
                outside.alpha = if (it.endOffset > 0) fraction else 1 - fraction
            }
        )
        inputView.setEditBackgroundColor(0xFFF2F2F2.toInt())
        outside.onClick { inputView.editorAdapter.notifyHideIme() }
    }
}