package com.xiaocydx.inputview.sample.message

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.inputview.EdgeToEdgeHelper
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.EditorAdapter
import com.xiaocydx.inputview.sample.R
import com.xiaocydx.inputview.sample.dp
import com.xiaocydx.inputview.sample.layoutParams
import com.xiaocydx.inputview.sample.matchParent
import com.xiaocydx.inputview.sample.message.MessageEditor.EMOJI
import com.xiaocydx.inputview.sample.message.MessageEditor.EXTRA
import com.xiaocydx.inputview.sample.message.MessageEditor.IME
import com.xiaocydx.inputview.sample.message.MessageEditor.VOICE
import com.xiaocydx.inputview.sample.wrapContent

/**
 * @author xcc
 * @date 2023/1/8
 */
class MessageEditorAdapter : EditorAdapter<MessageEditor>() {
    override val ime: MessageEditor = IME

    override fun onCreateView(parent: ViewGroup, editor: MessageEditor): View? = when (editor) {
        IME, VOICE -> null
        EMOJI -> EmojiRecyclerView(parent.context)
        EXTRA -> createView(R.layout.message_editor_extra, parent)
    }

    private fun createView(@LayoutRes resource: Int, parent: ViewGroup): View {
        return LayoutInflater.from(parent.context).inflate(resource, parent, false)
    }
}

enum class MessageEditor : Editor {
    IME, VOICE, EMOJI, EXTRA
}

class EmojiRecyclerView(context: Context) : RecyclerView(context) {

    init {
        val spanCount = 8
        adapter = EmojiAdapter(spanCount)
        layoutManager = GridLayoutManager(context, spanCount)
        recycledViewPool.setMaxRecycledViews(0, 16)
        layoutParams(matchParent, 350.dp)
        EdgeToEdgeHelper { handleGestureNavBarEdgeToEdgeOnApply() }
    }

    private class EmojiAdapter(spanCount: Int) : Adapter<ViewHolder>() {
        private val resIds = intArrayOf(
            R.mipmap.ic_message_emoji_1, R.mipmap.ic_message_emoji_2,
            R.mipmap.ic_message_emoji_3, R.mipmap.ic_message_emoji_4,
        )
        private val margin = 8.dp
        private val items = (0..147).map { resIds[it % resIds.size] }
        private val lastSpanGroupRange: IntRange
        private val ViewHolder.isInLastSpanGroup: Boolean
            get() = bindingAdapterPosition in lastSpanGroupRange

        init {
            var remainder = items.size % spanCount
            if (remainder == 0) remainder = spanCount
            val start = items.size - remainder
            val end = items.lastIndex + (spanCount - remainder)
            lastSpanGroupRange = start..end
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = AppCompatImageView(parent.context).apply {
                layoutParams(matchParent, wrapContent)
            }
            return object : ViewHolder(view) {}
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.itemView.updateLayoutParams<MarginLayoutParams> {
                setMargins(margin)
                if (holder.isInLastSpanGroup) bottomMargin = 0
            }
            (holder.itemView as ImageView).setImageResource(items[position])
        }

        override fun getItemCount(): Int = items.size
    }
}