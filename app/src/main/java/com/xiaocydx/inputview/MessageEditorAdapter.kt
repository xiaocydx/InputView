package com.xiaocydx.inputview

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.view.updatePadding
import com.xiaocydx.inputview.MessageEditor.*

/**
 * @author xcc
 * @date 2023/1/8
 */
class MessageEditorAdapter : EditorAdapter<MessageEditor>(), EditorHelper {
    override val editors: List<MessageEditor> = listOf(IME, VOICE, EMOJI, EXTRA)

    override fun isIme(editor: MessageEditor): Boolean = editor === IME

    @SuppressLint("SetTextI18n")
    override fun onCreateView(parent: ViewGroup, editor: MessageEditor): View? = when (editor) {
        IME, VOICE -> null
        EMOJI -> createView(R.layout.message_editor_emoji, parent)
        EXTRA -> createView(R.layout.message_editor_extra, parent)
    }?.apply {
        // setupWindowInsetsHandler()
    }

    private fun createView(@LayoutRes resource: Int, parent: ViewGroup): View {
        return LayoutInflater.from(parent.context).inflate(resource, parent, false)
    }

    private fun View.setupWindowInsetsHandler() = apply {
        (this as? ViewGroup)?.clipToPadding = false
        doOnApplyWindowInsetsCompat { view, insets, initialState ->
            view.updatePadding(bottom = when {
                !view.supportGestureNavBarEdgeToEdge(insets) -> initialState.paddings.bottom
                else -> insets.getNavigationBarHeight() + initialState.paddings.bottom
            })
        }
    }
}

enum class MessageEditor : Editor {
    IME, VOICE, EMOJI, EXTRA
}