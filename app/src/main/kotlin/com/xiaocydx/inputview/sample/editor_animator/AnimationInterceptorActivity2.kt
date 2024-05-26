package com.xiaocydx.inputview.sample.editor_animator

import android.os.Bundle
import android.view.animation.Interpolator
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.inputview.AnimationInterceptor
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.sample.basic.message.MessageEditor.EMOJI
import com.xiaocydx.inputview.sample.basic.message.MessageEditor.IME
import com.xiaocydx.inputview.sample.basic.message.init
import com.xiaocydx.inputview.sample.common.snackbar
import com.xiaocydx.inputview.sample.databinding.MessageListBinding

/**
 * [AnimationInterceptor]的示例代码，实现动画时长和插值器的差异化
 *
 * @author xcc
 * @date 2024/5/26
 */
class AnimationInterceptorActivity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InputView.init(window, gestureNavBarEdgeToEdge = true)
        setContentView(MessageListBinding.inflate(layoutInflater).init(window).apply {
            inputView.editorAnimator.setAnimationInterceptor(object : AnimationInterceptor {
                override fun onInterceptDurationMillis(previous: Editor?, current: Editor?, durationMillis: Long): Long {
                    if (previous != IME && current == IME) {
                        window.snackbar().setText("previous != IME，current = IME，动画时长调整为1000ms").show()
                        return 1000L
                    }
                    if (previous == IME && current != IME) {
                        window.snackbar().setText("previous = IME，current != IME，动画时长调整为100ms").show()
                        return 100L
                    }
                    if (current == EMOJI) {
                        window.snackbar().setText("current = EMOJI，动画时长调整为500ms").show()
                        return 500L
                    }
                    return super.onInterceptDurationMillis(previous, current, durationMillis)
                }

                override fun onInterceptInterpolator(previous: Editor?, current: Editor?, interpolator: Interpolator): Interpolator {
                    return super.onInterceptInterpolator(previous, current, interpolator)
                }
            })
        }.root)
    }
}