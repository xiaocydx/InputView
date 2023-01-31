package com.xiaocydx.inputview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    override fun onCreateView(parent: ViewGroup, editor: MessageEditor): View? = when (editor) {
        IME, VOICE -> null
        // EMOJI -> GestureNavBarEdgeToEdgeRecyclerView(parent.context)
        EMOJI -> createView(R.layout.message_editor_emoji, parent)
        EXTRA -> createView(R.layout.message_editor_extra, parent)
    }

    private fun createView(@LayoutRes resource: Int, parent: ViewGroup): View {
        return LayoutInflater.from(parent.context).inflate(resource, parent, false)
    }
}

enum class MessageEditor : Editor {
    IME, VOICE, EMOJI, EXTRA
}

/**
 * 实现[RecyclerView]的手势导航栏边到边示例代码
 */
private class GestureNavBarEdgeToEdgeRecyclerView(context: Context) : RecyclerView(context) {

    init {
        withLayoutParams(matchParent, 350.dp)
        adapter = object : Adapter<ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                val view = View(parent.context).apply {
                    setBackgroundColor(0xFF8DAA95.toInt())
                    withLayoutParams(matchParent, 100.dp) { setMargins(2.dp) }
                }
                return object : ViewHolder(view) {}
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int) = Unit

            override fun getItemCount(): Int = 20
        }
        layoutManager = GridLayoutManager(context, 3)
        setupGestureNavBarEdgeToEdge()
    }

    private fun setupGestureNavBarEdgeToEdge() = EdgeToEdgeHelper {
        // layoutParams.height初始高度是350.dp
        doOnApplyWindowInsets { view, insets, initialState ->
            val navigationBarHeight = insets.navigationBarHeight
            val supportGestureNavBarEdgeToEdge = insets.supportGestureNavBarEdgeToEdge(view)

            // 1. 若支持手势导航栏边到边，则增加高度，否则保持初始高度
            val height = when {
                !supportGestureNavBarEdgeToEdge -> initialState.params.height
                else -> navigationBarHeight + initialState.params.height
            }
            if (view.layoutParams.height != height) {
                view.updateLayoutParams { this.height = height }
            }

            // 2. 若支持手势导航栏边到边，则增加paddingBottom，否则保持初始paddingBottom
            view.updatePadding(bottom = when {
                !supportGestureNavBarEdgeToEdge -> initialState.paddings.bottom
                else -> navigationBarHeight + initialState.paddings.bottom
            })

            // 3. 示例代码view是RecyclerView：
            // 由于支持手势导航栏边到边会增加paddingBottom，因此将clipToPadding设为false，
            // 使得RecyclerView滚动时，能将内容绘制在paddingBottom区域，当滚动到底部时，
            // 留出paddingBottom区域，内容不会被手势导航栏遮挡。
            (view as? ViewGroup)?.clipToPadding = !supportGestureNavBarEdgeToEdge
        }
    }
}