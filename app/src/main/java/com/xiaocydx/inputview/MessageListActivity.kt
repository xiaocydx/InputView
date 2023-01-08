package com.xiaocydx.inputview

import android.os.Bundle
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        InputView.init(window)
        setContentView(ActivityMessageListBinding.inflate(layoutInflater).initView().root)
    }

    private fun ActivityMessageListBinding.initView() = apply {
        // 1. 初始化InputView的配置
        val editorAdapter = MessageEditorAdapter()
        inputView.apply {
            setEditText(etMessage)
            setEditorAdapter(editorAdapter)
            setEditorAnimator(DefaultEditorAnimator.pan())
        }

        // 2. 初始化RecyclerView的配置
        rvMessage.apply {
            layoutManager = LinearLayoutManager(root.context).apply {
                stackFromEnd = true
                reverseLayout = true
            }
            adapter = MessageListAdapter(itemCount = 20)
            scrollToPosition(0)
        }

        // 3. 关联各场景的交互
        RecyclerViewTouchHideEditor(editorAdapter).attach(rvMessage)
        RecyclerViewScrollToFirst(rvMessage).attach(etMessage, editorAdapter)
        EditorToggleController(tvVoice, ivVoice, ivEmoji, ivExtra, etMessage, editorAdapter).attach()
    }

    /**
     * 触摸RecyclerView隐藏当前[MessageEditor]
     */
    private class RecyclerViewTouchHideEditor(
        private val editorAdapter: MessageEditorAdapter
    ) : RecyclerView.OnItemTouchListener {

        fun attach(rv: RecyclerView) {
            rv.addOnItemTouchListener(this)
        }

        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            if (e.action == ACTION_DOWN && editorAdapter.currentEditor !== VOICE) {
                editorAdapter.notifyHideCurrent()
            }
            return false
        }

        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) = Unit

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit
    }

    /**
     * `etMessage`的高度更改或者显示的[MessageEditor]更改，RecyclerView滚动到首位（反转布局）
     */
    private class RecyclerViewScrollToFirst(
        private val rvMessage: RecyclerView
    ) : View.OnLayoutChangeListener, EditorVisibleListener<Editor> {

        fun attach(etMessage: EditText, editorAdapter: MessageEditorAdapter) {
            etMessage.addOnLayoutChangeListener(this)
            editorAdapter.addEditorVisibleListener(this)
        }

        override fun onLayoutChange(
            v: View,
            left: Int, top: Int, right: Int, bottom: Int,
            oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
        ) {
            if (right - left == 0) return
            val oldHeight = oldBottom - oldTop
            val newHeight = bottom - top
            if (oldHeight != newHeight) scrollToFirstIfNecessary()
        }

        override fun onVisibleChanged(previous: Editor?, current: Editor?) {
            scrollToFirstIfNecessary()
        }

        private fun scrollToFirstIfNecessary() {
            val lm = rvMessage.layoutManager as? LinearLayoutManager ?: return
            if (lm.findFirstCompletelyVisibleItemPosition() == 0) return
            rvMessage.scrollToPosition(0)
        }
    }

    /**
     * [MessageEditor]的视图和图标切换控制器
     */
    private class EditorToggleController(
        private val tvVoice: TextView,
        private val ivVoice: ImageView,
        private val ivEmoji: ImageView,
        private val ivExtra: ImageView,
        private val etMessage: EditText,
        private val editorAdapter: MessageEditorAdapter
    ) : EditorVisibleListener<MessageEditor> {
        private val actions = mutableMapOf<MessageEditor, Action>()

        fun attach() {
            actions[VOICE] = Action(ivVoice, R.mipmap.ic_message_editor_voice)
            actions[EMOJI] = Action(ivEmoji, R.mipmap.ic_message_editor_emoji)
            actions[EXTRA] = Action(ivExtra, R.mipmap.ic_message_editor_extra, isKeep = true)
            // 初始化各个按钮显示的图标
            actions.forEach { it.value.showSelfIcon() }
            ivVoice.onClick { editorAdapter.notifyToggle(VOICE) }
            ivEmoji.onClick { editorAdapter.notifyToggle(EMOJI) }
            ivExtra.onClick { editorAdapter.notifyToggle(EXTRA) }
            editorAdapter.addEditorVisibleListener(this)
        }

        override fun onVisibleChanged(previous: MessageEditor?, current: MessageEditor?) {
            if (previous === VOICE) {
                tvVoice.isVisible = false
                etMessage.isInvisible = false
                if (current === IME) etMessage.requestFocus()
            } else if (current === VOICE) {
                tvVoice.isVisible = true
                etMessage.isInvisible = true
            }
            actions.forEach { it.value.showSelfIcon() }
            current?.let(actions::get)?.takeIf { !it.isKeep }?.showImeIcon()
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
}