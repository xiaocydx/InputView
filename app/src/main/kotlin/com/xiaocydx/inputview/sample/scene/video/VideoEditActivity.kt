package com.xiaocydx.inputview.sample.scene.video

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.overlay.createOverlay
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.databinding.ActivityVideoEditBinding
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
            lifecycleOwner = this@VideoEditActivity,
            contentAdapter = VideoTitleAdapter(),
            editorAdapter = VideoEditorAdapter(lifecycle, supportFragmentManager)
        )

        overlay.addToOnBackPressedDispatcher(onBackPressedDispatcher)
        overlay.attachToWindow(initCompat = false) {
            it.setEditorBackgroundColor(0xFF1D1D1D.toInt())
            it.editorAnimator = FadeEditorAnimator(durationMillis = 5000)
        }

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