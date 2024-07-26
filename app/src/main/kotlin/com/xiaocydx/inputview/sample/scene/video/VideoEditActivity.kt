package com.xiaocydx.inputview.sample.scene.video

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.databinding.ActivityVideoEditBinding
import com.xiaocydx.inputview.transform.AnchorScaleChange
import com.xiaocydx.inputview.transform.ContentChangeBounds
import com.xiaocydx.inputview.transform.ContentChangeTranslation
import com.xiaocydx.inputview.transform.ContentMatch
import com.xiaocydx.inputview.transform.EditorMatch
import com.xiaocydx.inputview.transform.addSceneBackground
import com.xiaocydx.inputview.transform.addSceneFadeChange
import com.xiaocydx.inputview.transform.createOverlay
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
            lifecycleOwner = this@VideoEditActivity,
            contentAdapter = VideoTitleAdapter(),
            editorAdapter = VideoEditorAdapter(lifecycle, supportFragmentManager)
        )

        overlay.addToOnBackPressedDispatcher(onBackPressedDispatcher)

        overlay.apply {
            val contentMatch = ContentMatch { _, content -> content is VideoContent }
            val editorMatch = EditorMatch { _, editor -> editor is VideoEditor }
            addTransformer(AnchorScaleChange(preview, contentMatch, editorMatch))
            addTransformer(ContentChangeBounds(contentMatch))
            addTransformer(ContentChangeTranslation(contentMatch))
            addSceneFadeChange(contentMatch, editorMatch)
            addSceneBackground(0xFF1D1D1D.toInt(), contentMatch, editorMatch)
        }

        overlay.attach(window, compat = false)

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