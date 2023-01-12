package com.xiaocydx.inputview

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.Window
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.inputview.MessageEditor.*
import com.xiaocydx.inputview.databinding.ActivityMessageListBinding
import com.xiaocydx.sample.onClick
import kotlin.math.absoluteValue

/**
 * [InputView]的消息列表示例代码
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
     * 由于内容区域包含[RecyclerView]，因此使用[EditorMode.ADJUST_PAN]，显示编辑器区域时平移内容区域，
     * 不使用[EditorMode.ADJUST_RESIZE]，原因是更改内容区域尺寸会让[RecyclerView]重新measure和layout，
     * 这在性能表现上不如[EditorMode.ADJUST_PAN]。
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
     * 1. 更改`etMessage`的高度，`rvMessage`滚动到首位。
     * 2. 更改显示的[MessageEditor]，`rvMessage`滚动到首位。
     * 3. 处理`rvMessage`可视区域未铺满时的动画偏移。
     */
    private fun ActivityMessageListBinding.handleScroll() {
        val scrollToFirstIfNecessary = fun() {
            val lm = rvMessage.layoutManager as? LinearLayoutManager ?: return
            if (lm.findFirstCompletelyVisibleItemPosition() == 0) return
            rvMessage.scrollToPosition(0)
        }

        //region 更改etMessage的高度，rvMessage滚动到首位
        etMessage.addOnLayoutChangeListener listener@{ _, left, top, right, bottom,
                                                       _, oldTop, _, oldBottom ->
            if (right - left == 0) return@listener
            val oldHeight = oldBottom - oldTop
            val newHeight = bottom - top
            if (oldHeight != newHeight) scrollToFirstIfNecessary()
        }
        //endregion

        inputView.editorAnimator.addAnimationCallback(object : AnimationCallback {
            //region 更改显示的MessageEditor，rvMessage滚动到首位
            override fun onEditorChanged(previous: Editor?, current: Editor?) {
                scrollToFirstIfNecessary()
            }
            //endregion

            //region 处理rvMessage可视区域未铺满时的动画偏移
            override fun onAnimationStart(state: AnimationState) {
                if (state.startOffset == 0 && state.endOffset != 0
                        && calculateScrollRangeDiff() < 0) {
                    inputBar.background = ColorDrawable(0xFFF2F2F2.toInt())
                }
            }

            override fun onAnimationUpdate(state: AnimationState) {
                // 以显示Editor为例，Editor区域的布局位置向上平移，
                // rvMessage的内容向下平移，当两者相对平移至临界点时，
                // rvMessage的内容跟着Editor向上平移。
                var diff = calculateScrollRangeDiff()
                diff = if (diff < 0) diff.absoluteValue else return
                rvMessage.translationY = (state.currentOffset - state.navBarOffset)
                    .coerceAtLeast(0).coerceAtMost(diff).toFloat()
            }

            override fun onAnimationEnd(state: AnimationState) {
                if (state.endOffset == 0) inputBar.background = null
            }

            private fun calculateScrollRangeDiff(): Int {
                return rvMessage.computeVerticalScrollRange() - rvMessage.height
            }
            //endregion
        })
    }

    /**
     * 1. 在动画运行中决定是否拦截触摸事件。
     * 2. 触摸RecyclerView隐藏当前[MessageEditor]。
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun ActivityMessageListBinding.handleTouch() {
        //region 在动画运行中决定是否拦截触摸事件
        val delegate = window.callback
        window.callback = object : Window.Callback by delegate {
            override fun dispatchTouchEvent(e: MotionEvent): Boolean {
                // 实际场景可能需要在动画运行中拦截触摸事件，避免造成较差的交互体验
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