package com.xiaocydx.inputview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.inputview.databinding.MessageListBinding
import com.xiaocydx.inputview.message.init

/**
 * 消息列表的示例代码
 *
 * @author xcc
 * @date 2023/1/8
 */
class MessageListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InputView.init(window, gestureNavBarEdgeToEdge = true)
        setContentView(MessageListBinding.inflate(layoutInflater).init(window).root)
    }
}