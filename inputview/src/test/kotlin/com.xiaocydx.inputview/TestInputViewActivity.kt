/*
 * Copyright 2023 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaocydx.inputview

import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * @author xcc
 * @date 2023/1/13
 */
internal class TestInputViewActivity : AppCompatActivity() {
    private var _editText: EditText? = null
    lateinit var viewModel: TestInputViewViewModel; private set
    lateinit var inputView: InputView; private set
    lateinit var contentView: FrameLayout; private set
    val editText: EditText
        get() = requireNotNull(_editText)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InputView.init(window)
        viewModel = ViewModelProvider(this, TestInputViewViewModel)[TestInputViewViewModel::class.java]
        inputView = InputView(this)
        contentView = FrameLayout(this)
        _editText = EditText(this)
        inputView.editText = _editText

        contentView.addView(_editText)
        inputView.layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        inputView.addView(contentView, MATCH_PARENT, MATCH_PARENT)
        if (viewModel.canSetInputView) setContentView(inputView)
    }

    fun clearEditText() {
        val editText = _editText ?: return
        val parent = editText.parent as? ViewGroup
        parent?.removeView(editText)
        _editText = null
    }
}

class TestInputViewViewModel : ViewModel() {
    var canSetInputView = true

    companion object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass === TestInputViewViewModel::class.java)
            @Suppress("UNCHECKED_CAST")
            return TestInputViewViewModel() as T
        }
    }
}