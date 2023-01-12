package com.xiaocydx.inputview

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.inputview.databinding.ItemMessageBinding
import com.xiaocydx.sample.onClick

/**
 * @author xcc
 * @date 2023/1/8
 */
class MessageListAdapter(itemCount: Int) : RecyclerView.Adapter<MessageHolder>() {
    private val list: MutableList<String> = when {
        itemCount <= 0 -> mutableListOf()
        else -> (1..itemCount).map { "文本消息-${it}" }.toMutableList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MessageHolder(binding.apply {
            ivAvatar.onClick {
                // list.removeAt(holder.bindingAdapterPosition)
                // notifyItemRemoved(holder.bindingAdapterPosition)
            }
        })
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MessageHolder, position: Int) {
        holder.binding.tvContent.text = list[position]
    }

    override fun getItemCount(): Int = list.size
}

class MessageHolder(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)