package com.xiaocydx.inputview.sample.scene.video

import android.view.LayoutInflater
import android.view.View
import com.xiaocydx.inputview.sample.databinding.VideoCommonTitlebarBinding
import com.xiaocydx.inputview.sample.databinding.VideoTextTitlebarBinding
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.scene.transform.ContainerTransformation
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformation
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformation.ContainerState
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformation.State
import java.lang.ref.WeakReference

class PreviewTransformation(
    preview: View
) : OverlayTransformation<State> {
    private val previewRef = WeakReference(preview)
    private val point = IntArray(2)
    private var bottom = 0

    override fun start(state: State) {
        val preview = previewRef.get() ?: return
        preview.getLocationOnScreen(point)
        bottom = point[1] + preview.height
    }

    override fun update(state: State) {
        val preview = previewRef.get() ?: return
        val dy = (bottom - state.currentAnchorY).coerceAtLeast(0)
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
) : ContainerTransformation<ContainerState>(*editor) {
    private var binding: VideoTextTitlebarBinding? = null

    override fun getView(state: ContainerState): View {
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

    override fun onPrepare(state: ContainerState) = with(state) {
        if (state.current == VideoEditor.Text.Input
                && inputView.editText !== binding?.editText) {
            inputView.editText = binding?.editText
            inputView.editText?.requestFocus()
        }
    }

    override fun onEnd(state: ContainerState) {
        if (!isCurrent(state)) state.inputView.editText = null
    }
}

class CommonGroupTransformation(
    vararg editor: VideoEditor,
    private val notify: (VideoEditor?) -> Unit
) : ContainerTransformation<ContainerState>(*editor) {
    private var binding: VideoCommonTitlebarBinding? = null

    override fun getView(state: ContainerState): View {
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

    override fun onPrepare(state: ContainerState) {
        if (!isCurrent(state)) return
        val current = state.current as? VideoEditor
        binding?.tvTitle?.text = current?.title ?: ""
    }
}