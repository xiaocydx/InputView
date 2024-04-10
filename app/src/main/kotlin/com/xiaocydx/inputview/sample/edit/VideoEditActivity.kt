@file:Suppress("OPT_IN_USAGE")

package com.xiaocydx.inputview.sample.edit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.inputview.EditorMode
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.addAnimationCallback
import com.xiaocydx.inputview.disableGestureNavBarOffset
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.sample.databinding.ActivityVideoEditBinding
import com.xiaocydx.inputview.sample.edit.VideoEditor.Audio
import com.xiaocydx.inputview.sample.edit.VideoEditor.Image
import com.xiaocydx.inputview.sample.edit.VideoEditor.Text
import com.xiaocydx.inputview.sample.edit.VideoEditor.Video
import com.xiaocydx.inputview.sample.edit.transform.CommonGroupTransformation
import com.xiaocydx.inputview.sample.edit.transform.ContainerHeightTransformation
import com.xiaocydx.inputview.sample.edit.transform.PreviewScaleTransformation
import com.xiaocydx.inputview.sample.edit.transform.TextGroupTransformation
import com.xiaocydx.inputview.sample.edit.transform.TransformationEnforcer
import com.xiaocydx.inputview.sample.edit.transform.plus
import com.xiaocydx.inputview.sample.isDispatchTouchEventEnabled
import com.xiaocydx.inputview.sample.onClick
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.statusBars

/**
 * 通过视频编辑这类复杂的切换场景，演示[InputView]的使用
 *
 * @author xcc
 * @date 2023/12/1
 */
class VideoEditActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InputView.init(window, statusBarEdgeToEdge = true, gestureNavBarEdgeToEdge = true)
        setContentView(ActivityVideoEditBinding.inflate(layoutInflater).initAnimation().initEdgeToEdge().root)
    }

    private fun ActivityVideoEditBinding.initAnimation() = apply {
        val adapter = VideoEditorAdapter(this@VideoEditActivity)
        inputView.apply {
            editorAdapter = adapter
            editorMode = EditorMode.ADJUST_PAN
            setEditorBackgroundColor(0xFF1D1D1D.toInt())
        }

        val animator = FadeEditorAnimator(durationMillis = 3000)
        inputView.editorAnimator = animator
        // 在动画运行时拦截触摸事件
        animator.addAnimationCallback(
            onStart = { window.isDispatchTouchEventEnabled = false },
            onEnd = { window.isDispatchTouchEventEnabled = true },
        )

        val enforcer = TransformationEnforcer(lifecycle, inputView, container)
        val previewScale = PreviewScaleTransformation(preview)
        val containerHeight = ContainerHeightTransformation()
        val textGroup = TextGroupTransformation(Text.Input, Text.Style, Text.Emoji, show = enforcer::begin)
        val commonGroup = CommonGroupTransformation(Video, Audio, Image, show = enforcer::begin)
        enforcer.attach(previewScale + containerHeight + textGroup + commonGroup)
        arrayOf(
            tvInput to Text.Input, btnText to Text.Emoji,
            btnVideo to Video, btnAudio to Audio, btnImage to Image
        ).forEach { (view, editor) -> view.onClick { enforcer.begin(editor) } }
    }

    private fun ActivityVideoEditBinding.initEdgeToEdge() = apply {
        // 禁用手势导航栏偏移，自行处理手势导航栏
        inputView.disableGestureNavBarOffset()
        // 设置通用的手势导航栏EdgeToEdge处理逻辑
        space.insets().gestureNavBarEdgeToEdge()
        preview.insets().margins(statusBars())
    }
}