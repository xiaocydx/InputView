package com.xiaocydx.inputview.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.sample.databinding.MessageListBinding
import com.xiaocydx.inputview.sample.message.init

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