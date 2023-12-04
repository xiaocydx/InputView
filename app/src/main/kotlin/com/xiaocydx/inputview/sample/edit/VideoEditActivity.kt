@file:Suppress("OPT_IN_USAGE")

package com.xiaocydx.inputview.sample.edit

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.transition.getBounds
import androidx.transition.setLeftTopRightBottomCompat
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest

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
            setEditorBackgroundColor(0xFF1D1D1D.toInt())
        }
        arrayOf(
            tvInput to Text.Input, btnText to Text.Emoji,
            btnVideo to Video, btnAudio to Audio, btnImage to Image
        ).forEach { (view, editor) -> view.onClick { viewModel.show(editor) } }
        // 不排除有其它代码显示IME的可能性，通过EditorChangedListener完成状态同步,
        // 双向同步的过程，EditorAdapter和StateFlow会做差异对比，不会形成循环同步。
        adapter.addEditorChangedListener { _, current -> viewModel.show(current) }
        val commonAction = CommonGroupAction(Video, Audio, Image, show = viewModel::show)
        val textAction = TextGroupAction(Text.Input, Text.Style, Text.Emoji, show = viewModel::show)
        val action = commonAction + textAction
        viewModel.state
            .flowWithLifecycle(lifecycle)
            .distinctUntilChanged()
            .mapLatest { action.toggle(inputView, container, it) }
            .launchIn(lifecycleScope)
    }

    private fun ActivityVideoEditBinding.initAnimation() = apply {
        val animator = FadeEditorAnimator(durationMillis = 300)
        inputView.editorAnimator = animator

        // 1. 在动画运行时拦截触摸事件
        animator.addAnimationCallback(
            onStart = { window.isDispatchTouchEventEnabled = false },
            onEnd = { window.isDispatchTouchEventEnabled = true },
        )

        // 2. 处理titleBar的显示偏移，让titleBar能在root下面
        var titleBarHeight = 0f
        fun AnimationState.canTranslation() = startOffset == 0 || endOffset == 0
        animator.addAnimationCallback(
            onStart = start@{ state ->
                if (!state.canTranslation()) return@start
                titleBarHeight = container.height.toFloat()
                inputView.translationY = titleBarHeight
            },
            onUpdate = update@{ state ->
                if (!state.canTranslation()) return@update
                var fraction = state.interpolatedFraction
                if (state.startOffset == 0) fraction = 1 - fraction
                inputView.translationY = titleBarHeight * fraction
            }
        )

        // 3. 处理titleBar的切换变换，跟EditorAnimator的动画状态保持同步
        var canTransform = false
        var previousBar: View? = null
        var currentBar: View? = null
        val changeBounds = Rect()
        animator.addAnimationCallback(
            onStart = { state ->
                previousBar = currentBar
                currentBar = container.children.firstOrNull { it.isVisible }
                canTransform = state.previous != null && state.current != null
                        && previousBar != null && currentBar != null
                        && previousBar !== currentBar
                if (canTransform) previousBar?.isVisible = true
                if (canTransform && previousBar?.height != currentBar?.height) {
                    // 当动画开始时，需要将边界修正回previous的值，避免产生一帧的抖动
                    container.getBounds(changeBounds) { top = bottom - (previousBar?.height ?: 0) }
                }
            },
            onUpdate = { state ->
                if (canTransform) {
                    previousBar?.alpha = animator.calculateAlpha(state, start = true)
                    currentBar?.alpha = animator.calculateAlpha(state, start = false)
                }
                if (!changeBounds.isEmpty) {
                    val start = previousBar?.height ?: 0
                    val end = currentBar?.height ?: 0
                    val dy = start + (end - start) * state.interpolatedFraction
                    container.getBounds(changeBounds) { top = bottom - dy.toInt() }
                }
            },
            onEnd = {
                previousBar?.alpha = 1f
                currentBar?.alpha = 1f
                if (canTransform) previousBar?.isVisible = false
                canTransform = false
                changeBounds.setEmpty()
            }
        )

        // 4. 处理preview的缩放，通过OnDrawListener兼容手势导航栏高度变更，
        // 显示EditText的光标时，OnDrawListener会一直调用，这不会产生影响。
        root.viewTreeObserver.addOnDrawListener {
            // 此时才应用changeBounds，是为了修正边界，避免动画被layout阶段影响，
            // 执行时序是onStart() -> DrawListener，onUpdate() -> DrawListener。
            changeBounds.takeIf { !it.isEmpty }?.let(container::setLeftTopRightBottomCompat)
            val titleBarTop = container.top + inputView.translationY
            val dy = (preview.bottom - titleBarTop).coerceAtLeast(0f)
            val scale = 1f - dy / preview.height
            preview.apply {
                // 变换属性自带差异对比，不会形成循环OnDrawListener
                scaleX = scale
                scaleY = scale
                pivotX = preview.width.toFloat() / 2
                pivotY = 0f
            }
        }
    }

    private fun ActivityVideoEditBinding.initEdgeToEdge() = EdgeToEdgeHelper {
        // 禁用手势导航栏偏移，自行处理手势导航栏
        inputView.disableGestureNavBarOffset()
        // 设置通用的手势导航栏EdgeToEdge处理逻辑
        space.handleGestureNavBarEdgeToEdgeOnApply()
        preview.doOnApplyWindowInsets { view, insets, initialState ->
            view.updateMargins(top = initialState.params.marginTop + insets.statusBarHeight)
        }
        return@EdgeToEdgeHelper this@initEdgeToEdge
    }
}