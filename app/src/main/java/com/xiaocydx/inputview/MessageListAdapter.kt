package com.xiaocydx.inputview

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.inputview.databinding.ItemMessageBinding

/**
 * @author xcc
 * @date 2023/1/8
 */
class MessageListAdapter(private val itemCount: Int) : RecyclerView.Adapter<MessageHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MessageHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MessageHolder, position: Int) {
        holder.binding.tvContent.text = "文本消息-${position + 1}"
    }

    override fun getItemCount(): Int = itemCount
}

class MessageHolder(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)