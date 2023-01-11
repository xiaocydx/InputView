package com.xiaocydx.inputview

import android.annotation.SuppressLint
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
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.xiaocydx.inputview.MessageEditor.*
import com.xiaocydx.inputview.databinding.ActivityMessageListBinding
import com.xiaocydx.sample.onClick

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

    private fun ActivityMessageListBinding.init() = apply {
        // 2. 初始化InputView的属性
        val editorAdapter = MessageEditorAdapter()
        inputView.apply {
            editText = etMessage
            editorMode = EditorMode.ADJUST_PAN
            this.editorAdapter = editorAdapter
            // 不运行Editor的过渡动画
            // editorAnimator = NopEditorAnimator()
        }

        // 3. 初始化RecyclerView的属性
        rvMessage.apply {
            layoutManager = LinearLayoutManager(
                context, VERTICAL, /* reverseLayout */true
            )
            adapter = MessageListAdapter(itemCount = 20)
            scrollToPosition(0)
        }

        // 4. 处理输入场景的交互
        handleScroll()
        handleTouch()
        handleToggle(editorAdapter)
    }

    /**
     * 1. `etMessage`的高度更改，`rvMessage`滚动到首位。
     * 2. 显示的[MessageEditor]更改，`rvMessage`滚动到首位。
     */
    private fun ActivityMessageListBinding.handleScroll() {
        val scrollToFirstIfNecessary = fun() {
            val lm = rvMessage.layoutManager as? LinearLayoutManager ?: return
            if (lm.findFirstCompletelyVisibleItemPosition() == 0) return
            rvMessage.scrollToPosition(0)
        }

        etMessage.addOnLayoutChangeListener listener@{ _, left, top, right, bottom,
                                                       _, oldTop, _, oldBottom ->
            if (right - left == 0) return@listener
            val oldHeight = oldBottom - oldTop
            val newHeight = bottom - top
            if (oldHeight != newHeight) scrollToFirstIfNecessary()
        }

        inputView.editorAdapter.addEditorVisibleListener { _, _ -> scrollToFirstIfNecessary() }
    }

    /**
     * 1. 在动画运行中决定是否拦截触摸事件。
     * 2. 触摸RecyclerView隐藏当前[MessageEditor]。
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun ActivityMessageListBinding.handleTouch() {
        val delegate = window.callback
        window.callback = object : Window.Callback by delegate {
            override fun dispatchTouchEvent(e: MotionEvent): Boolean {
                // 实际场景可能需要在动画运行中拦截触摸事件，避免造成较差的交互体验
                // if (e.action == ACTION_DOWN && inputView.editorAnimator.isRunning) return false
                return delegate.dispatchTouchEvent(e)
            }
        }

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
        editorAdapter.addEditorVisibleListener { previous, current ->
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