package com.xiaocydx.inputview.sample.edit

import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.forEach
import androidx.core.view.isVisible
import com.xiaocydx.inputview.AnimationCallback
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.EditorAnimator
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.notifyHideCurrent
import com.xiaocydx.inputview.notifyShow
import com.xiaocydx.inputview.sample.databinding.VideoCommonTitlebarBinding
import com.xiaocydx.inputview.sample.databinding.VideoTextTitlebarBinding
import com.xiaocydx.inputview.sample.edit.VideoEditor.Text
import com.xiaocydx.inputview.sample.onClick
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * @author xcc
 * @date 2023/12/1
 */
abstract class VideoEditorAction {

    /**
     * 通知显示[current]，并切换为[current]对应的`titleBar`
     *
     * 分成两步的原因：调用显示IME的函数后，下一帧不一定能获取到IME高度，可能要等几帧，
     * 在调用显示IME的函数之前，又需要先对[inputView]设置[EditText]，以确保能显示IME，
     * 为了解决这个矛盾，将更新过程拆为两步，[attach]设置[EditText]，以确保能显示IME，
     * [update]更新内容和显示情况，以确保基于[AnimationCallback]实现的动画流畅。
     */
    suspend fun toggle(inputView: InputView, container: ViewGroup, current: VideoEditor?) {
        attach(inputView, container, current)
        val adapter = requireNotNull(inputView.editorAdapter as? VideoEditorAdapter)
        if (current != null) adapter.notifyShow(current) else adapter.notifyHideCurrent()
        inputView.editorAnimator.awaitPrepare()
        update(inputView, container, current)
    }

    /**
     * 在[AnimationCallback.onAnimationPrepare]之前，对[container]添加`titleBar`，
     * 添加`titleBar`后不能显示，当[update]被调用时，才能更新`titleBar`的显示情况。
     */
    abstract fun attach(inputView: InputView, container: ViewGroup, current: VideoEditor?): Boolean

    /**
     * 在[AnimationCallback.onAnimationPrepare]执行时，更新`titleBar`的内容和显示情况
     */
    abstract fun update(inputView: InputView, container: ViewGroup, current: VideoEditor?): Boolean

    private suspend fun EditorAnimator.awaitPrepare() = suspendCancellableCoroutine { cont ->
        val callback = object : AnimationCallback {
            override fun onAnimationPrepare(previous: Editor?, current: Editor?) {
                if (!cont.isActive) return
                removeAnimationCallback(this)
                cont.resume(Unit)
            }
        }
        addAnimationCallback(callback)
        cont.invokeOnCancellation {
            assert(Thread.currentThread() === Looper.getMainLooper().thread)
            removeAnimationCallback(callback)
        }
    }
}

operator fun VideoEditorAction.plus(other: VideoEditorAction): VideoEditorAction = CombinedAction(this, other)

private class CombinedAction(
    private val first: VideoEditorAction,
    private val second: VideoEditorAction
) : VideoEditorAction() {
    override fun attach(inputView: InputView, container: ViewGroup, current: VideoEditor?): Boolean {
        return first.attach(inputView, container, current) || second.attach(inputView, container, current)
    }

    override fun update(inputView: InputView, container: ViewGroup, current: VideoEditor?): Boolean {
        return first.update(inputView, container, current) || second.update(inputView, container, current)
    }
}

abstract class GroupAction<T : VideoEditor>(
    vararg editors: T,
    protected val show: (T?) -> Unit
) : VideoEditorAction() {
    private val editors = editors.toList()

    override fun attach(inputView: InputView, container: ViewGroup, current: VideoEditor?): Boolean {
        if (current == null || !editors.contains(current)) return false
        getEdiText(container)?.let { inputView.editText = it }
        val titleBar = getTitleBar(container)
        if (titleBar.parent !== container) {
            titleBar.isVisible = false
            container.addView(titleBar)
        }
        return true
    }

    override fun update(inputView: InputView, container: ViewGroup, current: VideoEditor?): Boolean {
        if (current == null || !editors.contains(current)) return false
        @Suppress("UNCHECKED_CAST")
        updateTitleBar(container, current as T)
        val titleBar = getTitleBar(container)
        val lastChild = container.getChildAt(container.childCount - 1)
        if (lastChild !== titleBar) titleBar.bringToFront()
        container.forEach { it.isVisible = it === titleBar }
        return true
    }

    protected open fun getEdiText(container: ViewGroup): EditText? = null
    protected abstract fun getTitleBar(container: ViewGroup): View
    protected open fun updateTitleBar(container: ViewGroup, editor: T) = Unit
}

class TextGroupAction(
    vararg editors: Text,
    show: (Text?) -> Unit
) : GroupAction<Text>(*editors, show = show) {
    private var binding: VideoTextTitlebarBinding? = null

    override fun getEdiText(container: ViewGroup) = ensure(container).editText

    override fun getTitleBar(container: ViewGroup) = ensure(container).root

    private fun ensure(parent: ViewGroup) = binding ?: VideoTextTitlebarBinding
        .inflate(LayoutInflater.from(parent.context), parent, false)
        .apply {
            binding = this
            tvEmoji.onClick { show(Text.Emoji) }
            tvStyle.onClick { show(Text.Style) }
            tvConfirm.onClick { show(null) }
        }
}

class CommonGroupAction(
    vararg editors: VideoEditor,
    show: (VideoEditor?) -> Unit
) : GroupAction<VideoEditor>(*editors, show = show) {
    private var binding: VideoCommonTitlebarBinding? = null

    override fun getTitleBar(container: ViewGroup) = ensure(container).root

    override fun updateTitleBar(container: ViewGroup, editor: VideoEditor) {
        ensure(container).tvTitle.text = editor.title
    }

    private fun ensure(parent: ViewGroup) = binding ?: VideoCommonTitlebarBinding
        .inflate(LayoutInflater.from(parent.context), parent, false)
        .apply {
            binding = this
            tvConfirm.onClick { show(null) }
        }
}