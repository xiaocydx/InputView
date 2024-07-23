package com.xiaocydx.inputview.sample.scene.video

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.overlay.PrepareState
import com.xiaocydx.inputview.overlay.Transformer
import com.xiaocydx.inputview.overlay.createOverlay
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.databinding.ActivityVideoEditBinding
import com.xiaocydx.inputview.setWindowFocusInterceptor
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.navigationBars
import com.xiaocydx.insets.statusBars

/**
 * 通过预览编辑的交互案例，演示[InputView]的使用
 *
 * @author xcc
 * @date 2023/12/1
 */
class VideoEditActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityVideoEditBinding.inflate(layoutInflater).init().root)
    }

    private fun ActivityVideoEditBinding.init() = apply {
        root.insets().paddings(navigationBars())
        preview.insets().margins(statusBars())

        val overlay = InputView.createOverlay(
            window = window,
            lifecycle = lifecycle,
            contentAdapter = VideoTitleAdapter(),
            editorAdapter = VideoEditorAdapter(lifecycle, supportFragmentManager)
        )

        overlay.attachToWindow(initCompat = false) {
            it.setEditorBackgroundColor(0xFF1D1D1D.toInt())
            it.editorAnimator = FadeEditorAnimator(durationMillis = 300)
        }

        overlay.setListener { previous, current ->
            println(previous)
        }

        overlay.setConverter { scene, editor ->
            if (scene === VideoScene.Input && editor == null) {
                VideoScene.Image
            } else {
                scene
            }
        }

        overlay.addTransformer(TouchOut { overlay.go(null) })

        arrayOf(
            tvInput to VideoScene.Input,
            btnText to VideoScene.Emoji,
            btnVideo to VideoScene.Video,
            btnAudio to VideoScene.Audio,
            btnImage to VideoScene.Image
        ).forEach { (view, scene) ->
            view.onClick { overlay.go(scene) }
        }
    }
}

class TouchOut(private val hide: () -> Unit) : Transformer {

    override fun onPrepare(state: PrepareState) {
        if (state.current == null) {
            state.contentView.setOnClickListener(null)
        } else {
            state.contentView.onClick(hide)
        }
    }
}