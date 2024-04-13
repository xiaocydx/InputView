package com.xiaocydx.inputview.sample.scene.videoedit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.xiaocydx.inputview.EditorMode
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.FragmentEditorAdapter
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.addAnimationCallback
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.sample.databinding.ActivityVideoEditBinding
import com.xiaocydx.inputview.sample.editor_adapter.fragment.EmojiFragment
import com.xiaocydx.inputview.sample.isDispatchTouchEventEnabled
import com.xiaocydx.inputview.sample.onClick
import com.xiaocydx.inputview.sample.scene.videoedit.VideoEditor.Audio
import com.xiaocydx.inputview.sample.scene.videoedit.VideoEditor.Image
import com.xiaocydx.inputview.sample.scene.videoedit.VideoEditor.Text.Emoji
import com.xiaocydx.inputview.sample.scene.videoedit.VideoEditor.Text.Input
import com.xiaocydx.inputview.sample.scene.videoedit.VideoEditor.Text.Style
import com.xiaocydx.inputview.sample.scene.videoedit.VideoEditor.Video
import com.xiaocydx.inputview.sample.transform.BoundsTransformation
import com.xiaocydx.inputview.sample.transform.OverlayTransformation.ContainerState
import com.xiaocydx.inputview.sample.transform.OverlayTransformationEnforcer
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
        setContentView(ActivityVideoEditBinding.inflate(layoutInflater).init().root)
    }

    private fun ActivityVideoEditBinding.init() = apply {
        // 设置通用的手势导航栏EdgeToEdge处理逻辑
        space.insets().gestureNavBarEdgeToEdge()
        preview.insets().margins(statusBars())

        val animator = FadeEditorAnimator(durationMillis = 300)
        val adapter = VideoEditorAdapter(this@VideoEditActivity)
        inputView.apply {
            editorAnimator = animator
            editorAdapter = adapter
            editorMode = EditorMode.ADJUST_PAN
            setEditorBackgroundColor(0xFF1D1D1D.toInt())
        }
        // 在动画运行时拦截触摸事件
        animator.addAnimationCallback(
            onStart = { window.isDispatchTouchEventEnabled = false },
            onEnd = { window.isDispatchTouchEventEnabled = true },
        )

        val enforcer = OverlayTransformationEnforcer(
            owner = this@VideoEditActivity,
            editorAnimator = animator, editorAdapter = adapter,
            stateProvider = { ContainerState(inputView, container) }
        )
        enforcer.add(BoundsTransformation())
            .add(PreviewTransformation(preview))
            .add(TextGroupTransformation(Input, Style, Emoji, notify = enforcer::notify))
            .add(CommonGroupTransformation(Video, Audio, Image, notify = enforcer::notify))
            .attach(inputView, onBackPressedDispatcher)

        arrayOf(
            tvInput to Input, btnText to Emoji,
            btnVideo to Video, btnAudio to Audio, btnImage to Image
        ).forEach { (view, editor) -> view.onClick { enforcer.notify(editor) } }
    }

    private class VideoEditorAdapter(
        fragmentActivity: FragmentActivity
    ) : FragmentEditorAdapter<VideoEditor>(fragmentActivity) {
        override val ime = Input

        override fun getEditorKey(editor: VideoEditor) = editor.title

        override fun onCreateFragment(editor: VideoEditor): Fragment {
            if (editor == Emoji) return EmojiFragment()
            return CommonFragment.newInstance(editor.title, editor.size)
        }
    }
}