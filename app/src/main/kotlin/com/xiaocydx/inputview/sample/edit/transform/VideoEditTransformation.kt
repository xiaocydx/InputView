package com.xiaocydx.inputview.sample.edit.transform

import android.view.LayoutInflater
import android.view.View
import com.xiaocydx.inputview.sample.databinding.VideoCommonTitlebarBinding
import com.xiaocydx.inputview.sample.databinding.VideoTextTitlebarBinding
import com.xiaocydx.inputview.sample.edit.VideoEditor
import com.xiaocydx.inputview.sample.edit.transform.Transformation.State
import com.xiaocydx.inputview.sample.onClick

class PreviewTransformation(private val preview: View) : Transformation<State> {

    override fun update(state: State) {
        val dy = (preview.bottom - state.currentAnchorY).coerceAtLeast(0)
        val scale = 1f - dy.toFloat() / preview.height
        preview.apply {
            scaleX = scale
            scaleY = scale
            pivotX = preview.width.toFloat() / 2
            pivotY = 0f
        }
    }
}

class TextGroupTransformation(
    vararg editor: VideoEditor.Text,
    private val notify: (VideoEditor?) -> Unit
) : GroupTransformation<State>(*editor) {
    private var binding: VideoTextTitlebarBinding? = null

    override fun getView(state: State): View {
        if (binding == null) {
            binding = VideoTextTitlebarBinding.inflate(
                LayoutInflater.from(state.container.context),
                state.container, false
            ).apply {
                tvEmoji.onClick { notify(VideoEditor.Text.Emoji) }
                tvStyle.onClick { notify(VideoEditor.Text.Style) }
                tvConfirm.onClick { notify(null) }
            }
        }
        return binding!!.root
    }

    override fun onPrepare(state: State) = with(state) {
        if (state.current == VideoEditor.Text.Input
                && inputView.editText !== binding?.editText) {
            inputView.editText = binding?.editText
            inputView.editText?.requestFocus()
        }
    }

    override fun onEnd(state: State) {
        if (!isCurrent(state)) state.inputView.editText = null
    }
}

class CommonGroupTransformation(
    vararg editor: VideoEditor,
    private val notify: (VideoEditor?) -> Unit
) : GroupTransformation<State>(*editor) {
    private var binding: VideoCommonTitlebarBinding? = null

    override fun getView(state: State): View {
        if (binding == null) {
            binding = VideoCommonTitlebarBinding.inflate(
                LayoutInflater.from(state.container.context),
                state.container, false
            ).apply {
                tvConfirm.onClick { notify(null) }
            }
        }
        return binding!!.root
    }

    override fun onPrepare(state: State) {
        if (!isCurrent(state)) return
        val current = state.current as? VideoEditor
        binding?.tvTitle?.text = current?.title ?: ""
    }
}