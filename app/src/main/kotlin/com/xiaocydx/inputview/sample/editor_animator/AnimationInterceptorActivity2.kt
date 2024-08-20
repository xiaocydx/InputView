package com.xiaocydx.inputview.sample.editor_animator

import android.os.Bundle
import android.view.Gravity
import android.view.animation.Interpolator
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.inputview.AnimationInterceptor
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.Insets.Decor
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.sample.basic.message.MessageEditor.Emoji
import com.xiaocydx.inputview.sample.basic.message.MessageEditor.Ime
import com.xiaocydx.inputview.sample.basic.message.init
import com.xiaocydx.inputview.sample.common.gravity
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
        InputView.init(window, Decor(gestureNavBarEdgeToEdge = true))
        setContentView(MessageListBinding.inflate(layoutInflater).init(window).intercept().root)
    }

    private fun MessageListBinding.intercept() = apply {
        inputView.editorAnimator.setAnimationInterceptor(object : AnimationInterceptor {
            override fun onInterceptDurationMillis(previous: Editor?, current: Editor?, durationMillis: Long): Long {
                if (previous != Ime && current == Ime) {
                    showSnackbar("previous != Ime，current = Ime\n动画时长调整为1000ms")
                    return 1000L
                }
                if (previous == Ime && current != Ime) {
                    showSnackbar("previous = Ime，current != Ime\n动画时长调整为100ms")
                    return 100L
                }
                if (current == Emoji) {
                    showSnackbar("current = Emoji\n动画时长调整为500ms")
                    return 500L
                }
                return super.onInterceptDurationMillis(previous, current, durationMillis)
            }

            override fun onInterceptInterpolator(previous: Editor?, current: Editor?, interpolator: Interpolator): Interpolator {
                return super.onInterceptInterpolator(previous, current, interpolator)
            }
        })
    }

    private fun showSnackbar(message: String) {
        window.snackbar()
            .gravity(Gravity.CENTER)
            .setText(message).show()
    }
}