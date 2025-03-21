package com.xiaocydx.inputview.sample.basic.message

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent.ACTION_DOWN
import android.view.Window
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.inputview.EditorAdapter
import com.xiaocydx.inputview.EditorMode
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.Insets.Decor
import com.xiaocydx.inputview.NopEditorAnimator
import com.xiaocydx.inputview.addAnimationCallback
import com.xiaocydx.inputview.current
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.linearEditorOffset
import com.xiaocydx.inputview.notifyHideCurrent
import com.xiaocydx.inputview.notifyToggle
import com.xiaocydx.inputview.sample.R
import com.xiaocydx.inputview.sample.basic.message.MessageEditor.Emoji
import com.xiaocydx.inputview.sample.basic.message.MessageEditor.Extra
import com.xiaocydx.inputview.sample.basic.message.MessageEditor.Voice
import com.xiaocydx.inputview.sample.common.addOnItemTouchListener
import com.xiaocydx.inputview.sample.common.isDispatchTouchEventEnabled
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.databinding.MessageListBinding

/**
 * 消息列表的示例代码
 *
 * @author xcc
 * @date 2023/1/8
 */
class MessageListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InputView.init(window, Decor(gestureNavBarEdgeToEdge = true))
        setContentView(MessageListBinding.inflate(layoutInflater).init(window).root)
    }
}

/**
 * 内容区域包含[RecyclerView]，建议使用[EditorMode.ADJUST_PAN]，显示编辑器区域时平移内容区域，
 * 使用[EditorMode.ADJUST_RESIZE]，更改内容区域尺寸时会让[RecyclerView]重新measure和layout，
 * 在性能表现上不如[EditorMode.ADJUST_PAN]。
 *
 * 示例代码使用[EditorMode.ADJUST_PAN]，[initScroll]演示了如何处理动画偏移，
 * 可以调小[MessageListAdapter]的`itemCount`，观察处理动画偏移后的运行结果。
 */
fun MessageListBinding.init(
    window: Window,
    editorAdapter: EditorAdapter<MessageEditor> = MessageEditorAdapter(),
    canRunAnimation: Boolean = true,
) = apply {
    // 1. 初始化InputView的属性
    inputView.apply {
        editText = etMessage
        editorMode = EditorMode.ADJUST_PAN
        this.editorAdapter = editorAdapter
        setEditorBackgroundColor(0xFFF2F2F2.toInt())
        if (canRunAnimation) {
            // 调整Editor的动画时长，需要看durationMillis的注释说明
            editorAnimator = FadeEditorAnimator(durationMillis = 200L)
            // 两个非IME的Editor切换，调整为线性更新编辑区的偏移值
            editorAnimator.linearEditorOffset()
        } else {
            // 不运行Editor的动画，仅记录动画状态，分发动画回调
            editorAnimator = NopEditorAnimator()
        }
    }

    // 2. 初始化RecyclerView的属性
    rvMessage.apply {
        layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true
            reverseLayout = true
        }
        adapter = MessageListAdapter(itemCount = 20)
        scrollToPosition(0)
    }

    // 3. 初始化输入场景的交互处理
    initScroll()
    initTouch(window)
    initToggle(editorAdapter)
}

/**
 * 1. 更改`rvMessage`的高度，`rvMessage`滚动到首位。
 * 2. 更改显示的[MessageEditor]，`rvMessage`滚动到首位。
 * 3. 处理`rvMessage`可视区域未铺满时的动画偏移。
 */
private fun MessageListBinding.initScroll() {
    val scrollToFirst = { rvMessage.scrollToPosition(0) }

    // 更改rvMessage的高度，rvMessage滚动到首位
    rvMessage.addOnLayoutChangeListener { v, _, _, _, _,
                                          _, oldTop, _, oldBottom ->
        val oldHeight = oldBottom - oldTop
        if (oldHeight > 0 && oldHeight != v.height) {
            // 视图树的PFLAG_FORCE_LAYOUT都移除后，再申请下一帧重新布局
            rvMessage.doOnPreDraw { scrollToFirst() }
        }
    }

    // 更改显示的MessageEditor，rvMessage滚动到首位
    inputView.editorAdapter.addEditorChangedListener { _, _ -> scrollToFirst() }

    // 处理rvMessage可视区域未铺满时的动画偏移
    val initialMode = inputView.editorMode
    val isExceedVisibleRange = { rvMessage.computeVerticalScrollRange() > rvMessage.height }
    inputView.editorAnimator.addAnimationCallback(
        onPrepare = { _, _ ->
            if (inputView.editorMode !== EditorMode.ADJUST_RESIZE && !isExceedVisibleRange()) {
                // 将editorMode动态调整为EditorMode.ADJUST_RESIZE相比于其它处理方式,
                // 能确保EditorAnimator动画运行中、结束后，添加消息的item动画正常运行。
                inputView.editorMode = EditorMode.ADJUST_RESIZE
            }
        },
        onEnd = { state -> if (state.endOffset == 0) inputView.editorMode = initialMode }
    )
}

/**
 * 1. 触摸RecyclerView隐藏当前[MessageEditor]。
 * 2. 实际场景可能需要在动画运行时拦截触摸事件。
 */
@SuppressLint("ClickableViewAccessibility")
private fun MessageListBinding.initTouch(
    window: Window,
    canInterceptTouchEvent: Boolean = false
) {
    // 触摸RecyclerView隐藏当前MessageEditor
    rvMessage.addOnItemTouchListener(onInterceptTouchEvent = { _, ev ->
        if (ev.action == ACTION_DOWN && inputView.editorAdapter.current !== Voice) {
            inputView.editorAdapter.notifyHideCurrent()
        }
        false
    })

    // 实际场景可能需要在动画运行时拦截触摸事件
    if (canInterceptTouchEvent) {
        inputView.editorAnimator.addAnimationCallback(
            onStart = { window.isDispatchTouchEventEnabled = false },
            onEnd = { window.isDispatchTouchEventEnabled = true }
        )
    }
}

/**
 * 处理[MessageEditor]的视图和图标切换
 */
private fun MessageListBinding.initToggle(editorAdapter: EditorAdapter<MessageEditor>) {
    val actions = mutableMapOf<MessageEditor, Action>()
    actions[Voice] = Action(ivVoice, R.drawable.ic_message_editor_voice)
    actions[Emoji] = Action(ivEmoji, R.drawable.ic_message_editor_emoji)
    actions[Extra] = Action(ivExtra, R.drawable.ic_message_editor_extra, isKeep = true)
    // 初始化各个按钮显示的图标
    actions.forEach { it.value.showSelfIcon() }

    editorAdapter.addEditorChangedListener { _, current ->
        tvVoice.isVisible = current === Voice
        actions.forEach { it.value.showSelfIcon() }
        current?.let(actions::get)?.takeIf { !it.isKeep }?.showImeIcon()
    }

    ivVoice.onClick { editorAdapter.notifyToggle(Voice) }
    ivEmoji.onClick { editorAdapter.notifyToggle(Emoji) }
    ivExtra.onClick { editorAdapter.notifyToggle(Extra) }
}

private class Action(
    private val view: ImageView,
    @DrawableRes val iconResId: Int,
    @DrawableRes val imeResId: Int = R.drawable.ic_message_editor_ime,
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