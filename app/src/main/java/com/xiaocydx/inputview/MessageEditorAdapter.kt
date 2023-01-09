package com.xiaocydx.inputview

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xiaocydx.inputview.MessageEditor.*

/**
 * @author xcc
 * @date 2023/1/8
 */
class MessageEditorAdapter : EditorAdapter<MessageEditor>() {
    override val editors: List<MessageEditor> = listOf(IME, VOICE, EMOJI, EXTRA)

    override fun isIme(editor: MessageEditor): Boolean = editor === IME

    @SuppressLint("SetTextI18n")
    override fun onCreateView(parent: ViewGroup, editor: MessageEditor): View? = when (editor) {
        IME, VOICE -> null
        EMOJI -> LayoutInflater.from(parent.context)
            .inflate(R.layout.message_editor_emoji, parent, false)
        EXTRA -> LayoutInflater.from(parent.context)
            .inflate(R.layout.message_editor_extra, parent, false)
    }
}

enum class MessageEditor : Editor {
    IME, VOICE, EMOJI, EXTRA
}