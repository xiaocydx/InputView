package com.xiaocydx.inputview

import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.app.AppCompatActivity

/**
 * @author xcc
 * @date 2023/1/13
 */
class TestActivity : AppCompatActivity() {
    lateinit var content: View
    lateinit var inputView: InputView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InputView.init(this)
        content = View(this)
        inputView = InputView(this).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
        inputView.addView(content, MATCH_PARENT, MATCH_PARENT)
        setContentView(inputView)
    }
}