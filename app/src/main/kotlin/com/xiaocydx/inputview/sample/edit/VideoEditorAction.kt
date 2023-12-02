package com.xiaocydx.inputview.sample.edit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.current
import com.xiaocydx.inputview.sample.databinding.VideoCommonTitlebarBinding
import com.xiaocydx.inputview.sample.databinding.VideoTextTitlebarBinding
import com.xiaocydx.inputview.sample.edit.VideoEditor.Text
import com.xiaocydx.inputview.sample.onClick

/**
 * @author xcc
 * @date 2023/12/1
 */
fun interface VideoEditorAction {
    fun update(inputView: InputView, container: ViewGroup, editor: VideoEditor?): Boolean
}

operator fun VideoEditorAction.plus(other: VideoEditorAction): VideoEditorAction = CombinedAction(this, other)

private class CombinedAction(
    private val first: VideoEditorAction,
    private val second: VideoEditorAction
) : VideoEditorAction {
    override fun update(inputView: InputView, container: ViewGroup, editor: VideoEditor?): Boolean {
        return first.update(inputView, container, editor) || second.update(inputView, container, editor)
    }
}

abstract class GroupAction<T : VideoEditor>(
    vararg editors: T,
    protected val show: (T?) -> Unit
) : VideoEditorAction {
    private val editors = editors.toList()

    @Suppress("UNCHECKED_CAST")
    override fun update(inputView: InputView, container: ViewGroup, editor: VideoEditor?): Boolean {
        assert(container.childCount <= 1)
        if (editor == null || !editors.contains(editor)) return false
        val titleBar = updateTitleBar(container, editor as T)
        if (container.getChildAt(0) !== titleBar) {
            if (inputView.editorAdapter.current != null) {
                val transition = Fade().apply {
                    interpolator = inputView.editorAnimator.interpolator
                    duration = inputView.editorAnimator.durationMillis
                }
                TransitionManager.beginDelayedTransition(container, transition)
            }
            container.removeAllViews()
            container.addView(titleBar)
        }
        val editText = updateEditText(inputView, editor)
        if (editText != null) inputView.editText = editText
        return true
    }

    protected abstract fun updateTitleBar(container: ViewGroup, editor: T): View

    protected open fun updateEditText(inputView: InputView, editor: T): EditText? = null
}

class TextGroupAction(
    vararg editors: Text,
    show: (Text?) -> Unit
) : GroupAction<Text>(*editors, show = show) {
    private var binding: VideoTextTitlebarBinding? = null

    override fun updateTitleBar(container: ViewGroup, editor: Text) = ensure(container).root

    override fun updateEditText(inputView: InputView, editor: Text) = ensure(inputView).editText

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

    override fun updateTitleBar(container: ViewGroup, editor: VideoEditor): View {
        return ensure(container).apply { tvTitle.text = editor.title }.root
    }

    private fun ensure(parent: ViewGroup) = binding ?: VideoCommonTitlebarBinding
        .inflate(LayoutInflater.from(parent.context), parent, false)
        .apply {
            binding = this
            tvConfirm.onClick { show(null) }
        }
}