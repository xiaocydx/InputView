package com.xiaocydx.inputview.sample.edit.transform

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import androidx.transition.getBounds
import androidx.transition.setLeftTopRightBottomCompat
import com.xiaocydx.inputview.sample.databinding.VideoCommonTitlebarBinding
import com.xiaocydx.inputview.sample.databinding.VideoTextTitlebarBinding
import com.xiaocydx.inputview.sample.edit.VideoEditor
import com.xiaocydx.inputview.sample.edit.transform.Transformation.State
import com.xiaocydx.inputview.sample.onClick

class PreviewScaleTransformation(private val preview: View) : Transformation<State> {

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

class ContainerHeightTransformation : Transformation<State> {
    private val previousBounds = Rect()
    private val currentBounds = Rect()
    private var canTransform = false
    private var startHeight = 0
    private var endHeight = 0

    override fun prepare(state: State) {
        state.container.getBounds(previousBounds)
    }

    override fun start(state: State) = with(state) {
        state.container.getBounds(currentBounds)
        startHeight = previousBounds.height()
        endHeight = currentBounds.height()
        canTransform = previous != null && current != null && startHeight != endHeight
        if (canTransform) container.setLeftTopRightBottomCompat(previousBounds)
    }

    override fun update(state: State) = with(state) {
        if (!canTransform) return
        val start = startHeight
        val end = endHeight
        val dy = start + (end - start) * state.interpolatedFraction
        container.getBounds(currentBounds) { top = bottom - dy.toInt() }
        container.setLeftTopRightBottomCompat(currentBounds)
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