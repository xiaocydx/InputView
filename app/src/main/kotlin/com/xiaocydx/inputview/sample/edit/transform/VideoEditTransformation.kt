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

class PreviewScaleTransformation(private val preview: View) : Transformation {

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

class ContainerHeightTransformation : Transformation {
    private val previousBounds = Rect()
    private val currentBounds = Rect()
    private var canTransform = false
    private var startHeight = 0
    private var endHeight = 0

    override fun attach(state: State) {
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
    private val show: (VideoEditor?) -> Unit
) : GroupTransformation(*editor) {
    private var binding: VideoTextTitlebarBinding? = null

    override fun getView(state: State): View {
        if (binding == null) {
            binding = VideoTextTitlebarBinding.inflate(
                LayoutInflater.from(state.container.context),
                state.container, false
            ).apply {
                tvEmoji.onClick { show(VideoEditor.Text.Emoji) }
                tvStyle.onClick { show(VideoEditor.Text.Style) }
                tvConfirm.onClick { show(null) }
            }
        }
        return binding!!.root
    }

    override fun onAttach(state: State) {
        val binding = binding ?: return
        state.inputView.editText = binding.editText
    }
}

class CommonGroupTransformation(
    vararg editor: VideoEditor,
    private val show: (VideoEditor?) -> Unit
) : GroupTransformation(*editor) {
    private var binding: VideoCommonTitlebarBinding? = null

    override fun getView(state: State): View {
        if (binding == null) {
            binding = VideoCommonTitlebarBinding.inflate(
                LayoutInflater.from(state.container.context),
                state.container, false
            ).apply {
                tvConfirm.onClick { show(null) }
            }
        }
        return binding!!.root
    }

    override fun onAttach(state: State) {
        if (!isCurrent(state)) return
        binding?.tvTitle?.text = state.current?.title ?: ""
    }
}