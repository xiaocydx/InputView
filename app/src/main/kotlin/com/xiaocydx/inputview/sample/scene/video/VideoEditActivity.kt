package com.xiaocydx.inputview.sample.scene.video

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.databinding.ActivityVideoEditBinding
import com.xiaocydx.inputview.sample.scene.video.VideoEditor.Audio
import com.xiaocydx.inputview.sample.scene.video.VideoEditor.Image
import com.xiaocydx.inputview.sample.scene.video.VideoEditor.Text.Emoji
import com.xiaocydx.inputview.sample.scene.video.VideoEditor.Text.Input
import com.xiaocydx.inputview.sample.scene.video.VideoEditor.Video
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
        InputView.init(window, statusBarEdgeToEdge = true, gestureNavBarEdgeToEdge = true)
        setContentView(ActivityVideoEditBinding.inflate(layoutInflater).init().root)
    }

    private fun ActivityVideoEditBinding.init() = apply {
        root.insets().paddings(navigationBars())
        preview.insets().margins(statusBars())
        val overlay = VideoEditOverlay(this@VideoEditActivity)
        arrayOf(
            tvInput to Input, btnText to Emoji,
            btnVideo to Video, btnAudio to Audio, btnImage to Image
        ).forEach { (view, editor) ->
            view.onClick { overlay.notify(editor) }
        }
        // 关联覆盖层，preview依赖覆盖层的锚点做变换
        overlay.attachToWindow(preview)
    }
}