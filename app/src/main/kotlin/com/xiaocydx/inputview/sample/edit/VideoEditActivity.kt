package com.xiaocydx.inputview.sample.edit

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.xiaocydx.inputview.AnimationState
import com.xiaocydx.inputview.EdgeToEdgeHelper
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
import com.xiaocydx.inputview.sample.isDispatchTouchEventEnabled
import com.xiaocydx.inputview.sample.onClick
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * 通过视频编辑这类复杂的切换场景，演示[InputView]的使用
 *
 * @author xcc
 * @date 2023/12/1
 */
class VideoEditActivity : AppCompatActivity() {
    private val viewModel: VideoEditViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InputView.init(window, statusBarEdgeToEdge = true, gestureNavBarEdgeToEdge = true)
        setContentView(ActivityVideoEditBinding.inflate(layoutInflater)
            .initShowOrHide().initAnimation().initEdgeToEdge().root)
    }

    private fun ActivityVideoEditBinding.initShowOrHide() = apply {
        val adapter = VideoEditorAdapter(this@VideoEditActivity)
        inputView.apply {
            editorAdapter = adapter
            editorMode = EditorMode.ADJUST_PAN
            editorAnimator = FadeEditorAnimator(durationMillis = 300)
            setEditBackgroundColor(0xFF1D1D1D.toInt())
        }

        arrayOf(
            tvInput to Text.Input, btnText to Text.Emoji,
            btnVideo to Video, btnAudio to Audio, btnImage to Image
        ).forEach { (view, editor) -> view.onClick { viewModel.show(editor) } }
        val commonAction = CommonGroupAction(Video, Audio, Image, show = viewModel::show)
        val textAction = TextGroupAction(Text.Input, Text.Style, Text.Emoji, show = viewModel::show)
        val action = commonAction + textAction
        viewModel.state
            .flowWithLifecycle(lifecycle)
            .onEach {
                action.update(inputView, container, it)
                adapter.notifyShowOrHide(it)
            }
            .launchIn(lifecycleScope)
        // 不排除有其它代码显示IME的可能性，通过EditorChangedListener完成状态同步,
        // 双向同步的过程，EditorAdapter和StateFlow会做差异对比，不会形成循环同步。
        adapter.addEditorChangedListener { _, current -> viewModel.show(current) }
    }

    private fun ActivityVideoEditBinding.initAnimation() = apply {
        // 在动画运行时拦截触摸事件
        inputView.editorAnimator.addAnimationCallback(
            onStart = { window.isDispatchTouchEventEnabled = false },
            onEnd = { window.isDispatchTouchEventEnabled = true },
        )

        // 处理titleBar的偏移
        var titleBarHeight = 0f
        fun AnimationState.canTranslation() = startOffset == 0 || endOffset == 0
        inputView.editorAnimator.addAnimationCallback(
            onStart = start@{ state ->
                // 确定endOffset了，才显示titleBar
                container.isVisible = true
                if (!state.canTranslation()) return@start
                titleBarHeight = container.height.toFloat()
                inputView.translationY = titleBarHeight
            },
            onUpdate = update@{ state ->
                if (!state.canTranslation()) return@update
                var fraction = state.animatedFraction
                if (state.startOffset == 0) fraction = 1 - fraction
                inputView.translationY = titleBarHeight * fraction
            },
            onEnd = { container.isVisible = it.endOffset != 0 }
        )

        // 处理preview的缩放，通过PreDraw兼容手势导航栏高度变更
        root.viewTreeObserver.addOnPreDrawListener {
            val titleBarTop = container.top + inputView.translationY
            val dy = (preview.bottom - titleBarTop).coerceAtLeast(0f)
            val scale = 1f - dy / preview.height
            preview.apply {
                // 变换属性自带差异对比，不会形成循环PreDraw
                scaleX = scale
                scaleY = scale
                pivotX = preview.width.toFloat() / 2
                pivotY = 0f
            }
            true
        }
    }

    private fun ActivityVideoEditBinding.initEdgeToEdge() = EdgeToEdgeHelper {
        // 禁用手势导航栏偏移，自行处理手势导航栏
        inputView.disableGestureNavBarOffset()
        preview.doOnApplyWindowInsets { view, insets, initialState ->
            view.updateMargins(top = initialState.params.marginTop + insets.statusBarHeight)
        }
        space.doOnApplyWindowInsets { view, insets, initialState ->
            val supportGestureNavBarEdgeToEdge = insets.supportGestureNavBarEdgeToEdge(view)
            val bottom = if (supportGestureNavBarEdgeToEdge) insets.navigationBarHeight else 0
            view.updateMargins(bottom = initialState.params.marginBottom + bottom)
        }
        return@EdgeToEdgeHelper this@initEdgeToEdge
    }
}