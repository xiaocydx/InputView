package com.xiaocydx.inputview.sample.scene.video

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.databinding.ActivityVideoEditBinding
import com.xiaocydx.inputview.transform.ChangeScale
import com.xiaocydx.inputview.transform.ContentChangeBounds
import com.xiaocydx.inputview.transform.ContentChangeTranslation
import com.xiaocydx.inputview.transform.Overlay
import com.xiaocydx.inputview.transform.addSceneBackground
import com.xiaocydx.inputview.transform.addSceneFadeChange
import com.xiaocydx.inputview.transform.createOverlay
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.navigationBars
import com.xiaocydx.insets.statusBars

/**
 * 通过预览编辑的交互案例，演示[createOverlay]的使用
 *
 * @author xcc
 * @date 2023/12/1
 */
class VideoEditActivity : AppCompatActivity() {
    private lateinit var overlay: Overlay<VideoScene>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InputView.init(window, statusBarEdgeToEdge = true, gestureNavBarEdgeToEdge = true)
        setContentView(ActivityVideoEditBinding.inflate(layoutInflater).init().root)
    }

    private fun ActivityVideoEditBinding.init() = apply {
        root.insets().paddings(navigationBars())
        preview.insets().margins(statusBars())

        overlay = InputView.createOverlay(
            sceneList = VideoScene.entries,
            lifecycleOwner = this@VideoEditActivity,
            contentAdapter = VideoContentAdapter { overlay.go(it) },
            editorAdapter = VideoEditorAdapter(lifecycle, supportFragmentManager)
        ) {
            add(ChangeScale(preview))
            add(ContentChangeBounds())
            add(ContentChangeTranslation())
            addSceneFadeChange()
            addSceneBackground(0xFF1D1D1D.toInt())
            addToOnBackPressedDispatcher(onBackPressedDispatcher)
            attach(window)
        }

        arrayOf(
            tvInput to VideoScene.InputText,
            btnText to VideoScene.InputEmoji,
            btnVideo to VideoScene.SelectVideo,
            btnAudio to VideoScene.SelectAudio,
            btnImage to VideoScene.SelectImage
        ).forEach { (view, scene) ->
            view.onClick { overlay.go(scene) }
        }
    }
}