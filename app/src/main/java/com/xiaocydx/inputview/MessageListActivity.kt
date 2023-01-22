package com.xiaocydx.inputview

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.Window
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.inputview.MessageEditor.*
import com.xiaocydx.inputview.databinding.ActivityMessageListBinding
import com.xiaocydx.sample.onClick

/**
 * [InputView]的消息列表示例代码
 *
 * **注意**：需要确保`androidx.appcompat`的版本足够高，因为高版本修复了[WindowInsetsCompat]常见的问题，
 * 例如高版本修复了应用退至后台，再重新显示，调用[WindowInsetsControllerCompat.show]显示IME无效的问题。
 *
 * @author xcc
 * @date 2023/1/8
 */
class MessageListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. 初始化InputView所需的配置
        InputView.init(window, gestureNavBarEdgeToEdge = true)
        setContentView(ActivityMessageListBinding.inflate(layoutInflater).init().root)
    }

    /**
     * 内容区域包含[RecyclerView]，建议使用[EditorMode.ADJUST_PAN]，显示编辑器区域时平移内容区域，
     * 使用[EditorMode.ADJUST_RESIZE]，更改内容区域尺寸时会让[RecyclerView]重新measure和layout，
     * 在性能表现上不如[EditorMode.ADJUST_PAN]。
     *
     * 示例代码反转了[RecyclerView]的布局，并从顶部开始填充子View，此时使用[EditorMode.ADJUST_PAN]，
     * 需要对[RecyclerView]处理编辑器区域的动画偏移，[handleScroll]演示了如何处理动画偏移，
     * 可以调小[MessageListAdapter]的`itemCount`，观察处理后的效果。
     */
    private fun ActivityMessageListBinding.init() = apply {
        // 2. 初始化InputView的属性
        val editorAdapter = MessageEditorAdapter()
        inputView.apply {
            editText = etMessage
            editorMode = EditorMode.ADJUST_PAN
            this.editorAdapter = editorAdapter
            // 调整Editor的过渡动画时长，需要看durationMillis的注释说明
            // editorAnimator = FadeEditorAnimator(durationMillis = 500)
            // 不运行Editor的过渡动画，仅记录动画状态，分发动画回调
            // editorAnimator = NopEditorAnimator()
        }

        // 3. 初始化RecyclerView的属性
        rvMessage.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
                reverseLayout = true
            }
            adapter = MessageListAdapter(itemCount = 20)
            scrollToPosition(0)
        }

        // 4. 处理输入场景的交互
        handleScroll()
        handleTouch()
        handleToggle(editorAdapter)
    }

    /**
     * 1. 更改`rvMessage`的高度，`rvMessage`滚动到首位。
     * 2. 更改显示的[MessageEditor]，`rvMessage`滚动到首位。
     * 3. 处理`rvMessage`可视区域未铺满时的动画偏移。
     */
    private fun ActivityMessageListBinding.handleScroll() {
        val scrollToFirst = { rvMessage.scrollToPosition(0) }

        //region 更改rvMessage的高度，rvMessage滚动到首位
        rvMessage.addOnLayoutChangeListener listener@{ _, left, top, right, bottom,
                                                       _, oldTop, _, oldBottom ->
            if (right - left == 0) return@listener
            val oldHeight = oldBottom - oldTop
            val newHeight = bottom - top
            if (oldHeight != newHeight) scrollToFirst()
        }
        //endregion

        //region 更改显示的MessageEditor，rvMessage滚动到首位
        inputView.editorAdapter.addEditorChangedListener { _, _ ->
            if (inputView.editorMode === EditorMode.ADJUST_PAN) scrollToFirst()
        }
        //endregion

        //region 处理rvMessage可视区域未铺满时的动画偏移
        val initialMode = inputView.editorMode
        inputView.editorAnimator.addAnimationCallback(object : AnimationCallback {
            override fun onAnimationStart(state: AnimationState) {
                if (state.startOffset == 0 && state.endOffset != 0
                        && initialMode !== EditorMode.ADJUST_RESIZE
                        && calculateVerticalScrollRangeDiff() < 0) {
                    // 将editorMode动态调整为EditorMode.ADJUST_RESIZE相比于其它处理方式,
                    // 能确保EditorAnimator动画运行中、结束后，添加消息的item动画正常运行。
                    inputView.editorMode = EditorMode.ADJUST_RESIZE
                }
            }

            override fun onAnimationEnd(state: AnimationState) {
                if (state.endOffset == 0) inputView.editorMode = initialMode
            }

            private fun calculateVerticalScrollRangeDiff(): Int {
                return rvMessage.computeVerticalScrollRange() - rvMessage.height
            }
        })
        //endregion
    }

    /**
     * 1. 在动画运行时决定是否拦截触摸事件。
     * 2. 触摸RecyclerView隐藏当前[MessageEditor]。
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun ActivityMessageListBinding.handleTouch() {
        //region 在动画运行中决定是否拦截触摸事件
        val delegate = window.callback
        window.callback = object : Window.Callback by delegate {
            override fun dispatchTouchEvent(e: MotionEvent): Boolean {
                // 实际场景可能需要在动画运行时拦截触摸事件，避免造成较差的交互体验
                // if (e.action == ACTION_DOWN && inputView.editorAnimator.isRunning) return false
                return delegate.dispatchTouchEvent(e)
            }
        }
        //endregion

        //region 触摸RecyclerView隐藏当前MessageEditor
        rvMessage.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.action == ACTION_DOWN && inputView.editorAdapter.current !== VOICE) {
                    inputView.editorAdapter.notifyHideCurrent()
                }
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) = Unit

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit
        })
        //endregion
    }

    /**
     * 处理[MessageEditor]的视图和图标切换
     */
    private fun ActivityMessageListBinding.handleToggle(editorAdapter: MessageEditorAdapter) {
        val actions = mutableMapOf<MessageEditor, Action>()
        actions[VOICE] = Action(ivVoice, R.mipmap.ic_message_editor_voice)
        actions[EMOJI] = Action(ivEmoji, R.mipmap.ic_message_editor_emoji)
        actions[EXTRA] = Action(ivExtra, R.mipmap.ic_message_editor_extra, isKeep = true)
        // 初始化各个按钮显示的图标
        actions.forEach { it.value.showSelfIcon() }
        ivVoice.onClick { editorAdapter.notifyToggle(VOICE) }
        ivEmoji.onClick { editorAdapter.notifyToggle(EMOJI) }
        ivExtra.onClick { editorAdapter.notifyToggle(EXTRA) }
        editorAdapter.addEditorChangedListener { previous, current ->
            if (previous === VOICE) {
                tvVoice.isVisible = false
                etMessage.isVisible = true
                if (current === IME) etMessage.requestFocus()
            } else if (current === VOICE) {
                tvVoice.isVisible = true
                etMessage.isVisible = false
            }
            actions.forEach { it.value.showSelfIcon() }
            current?.let(actions::get)?.takeIf { !it.isKeep }?.showImeIcon()
        }
    }

    private class Action(
        private val view: ImageView,
        @DrawableRes val iconResId: Int,
        @DrawableRes val imeResId: Int = R.mipmap.ic_message_editor_ime,
        val isKeep: Boolean = false
    ) {
        private var currentResId = 0

        fun showSelfIcon() = showIcon(iconResId)

        fun showImeIcon() = showIcon(imeResId)

        private fun showIcon(resId: Int) {
            if (currentResId == resId) return
            currentResId = resId
            view.setImageResource(resId)
        }
    }
}