package com.xiaocydx.inputview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import com.xiaocydx.inputview.MessageEditor.*
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.withLayoutParams

/**
 * @author xcc
 * @date 2023/1/8
 */
class MessageEditorAdapter : EditorAdapter<MessageEditor>() {
    override val editors: List<MessageEditor> = listOf(IME, VOICE, EMOJI, EXTRA)

    override fun isIme(editor: MessageEditor): Boolean = editor === IME

    @SuppressLint("SetTextI18n")
    override fun onCreateView(parent: ViewGroup, editor: MessageEditor): View? {
        return when (editor) {
            IME, VOICE -> null
            EMOJI -> createTextView(parent.context).apply {
                text = "Emoji Editor"
                withLayoutParams(matchParent, 350.dp)
            }
            EXTRA -> createTextView(parent.context).apply {
                text = "Extra Editor"
                withLayoutParams(matchParent, 150.dp)
            }
        }
    }

    private fun createTextView(
        context: Context
    ) = AppCompatTextView(context).apply {
        gravity = Gravity.CENTER
        textSize = 14.dp.toFloat()
        setTextColor(Color.BLACK)
    }
}

enum class MessageEditor : Editor {
    IME, VOICE, EMOJI, EXTRA
}