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
import androidx.appcompat.app.AppCompatActivity

/**
 * @author xcc
 * @date 2024/4/6
 */
internal class TestImeAnimatorActivity : AppCompatActivity() {
    private var _editText: EditText? = null
    lateinit var animator: ImeAnimator; private set
    val editText: EditText
        get() = requireNotNull(_editText)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InputView.init(window)
        _editText = EditText(this)
        animator = InputView.animator(window, _editText!!)
        setContentView(_editText, LayoutParams(MATCH_PARENT, MATCH_PARENT))
    }

    fun clearEditText() {
        val editText = _editText ?: return
        val parent = editText.parent as? ViewGroup
        parent?.removeView(editText)
        _editText
    }
}