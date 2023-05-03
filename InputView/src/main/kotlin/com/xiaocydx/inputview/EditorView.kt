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

import android.content.Context
import android.view.View
import android.widget.FrameLayout

/**
 * [InputView]的编辑区，负责管理[Editor]
 *
 * @author xcc
 * @date 2023/1/7
 */
internal class EditorView(context: Context) : FrameLayout(context) {
    private val views = mutableMapOf<Editor, View?>()
    private var editText: EditTextHolder? = null
    private var removePreviousImmediately = true
    var ime: Editor? = null; private set
    var current: Editor? = null; private set
    var changeRecord = ChangeRecord(); private set
    lateinit var adapter: EditorAdapter<*>; private set

    init {
        setAdapter(ImeAdapter())
    }

    fun setAdapter(adapter: EditorAdapter<*>) {
        this.adapter = adapter
        current?.let(::hideChecked)
        if (views.isNotEmpty()) {
            removeAllViews()
            views.clear()
        }
        if (changeRecord.previousChild != null
                || changeRecord.currentChild != null) {
            changeRecord = ChangeRecord()
        }

        ime = checkedAdapter().ime
        current = null
    }

    fun setEditTextHolder(editText: EditTextHolder?) {
        this.editText = editText
    }

    fun setRemovePreviousImmediately(immediately: Boolean) {
        removePreviousImmediately = immediately
        if (removePreviousImmediately) removeChangeRecordPrevious()
    }

    fun showChecked(editor: Editor, controlIme: Boolean = true): Boolean {
        val adapter = checkedAdapter()
        if (current === editor) return false
        val currentChild: View?
        val previous = current
        val previousChild = views[previous]
        if (!views.contains(editor)) {
            currentChild = when {
                editor === ime -> null
                else -> adapter.onCreateView(this, editor)
            }
            views[editor] = currentChild
        } else {
            currentChild = views[editor]
        }
        removePreviousBeforeChange(previousChild)
        currentChild?.let(::addView)
        current = editor
        changeRecord = ChangeRecord(previousChild, currentChild)
        if (previous === ime) {
            handleImeShown(shown = false, controlIme)
        } else if (editor === ime) {
            handleImeShown(shown = true, controlIme)
        }
        adapter.onEditorChanged(previous, current)
        return true
    }

    fun hideChecked(editor: Editor, controlIme: Boolean = true): Boolean {
        val adapter = checkedAdapter()
        if (current === editor) {
            val previousChild = views[editor]
            removePreviousBeforeChange(previousChild)
            current = null
            changeRecord = ChangeRecord(previousChild, currentChild = null)
            if (editor === ime) handleImeShown(shown = false, controlIme)
            adapter.onEditorChanged(editor, current)
            return true
        }
        return false
    }

    private fun removePreviousBeforeChange(previousChild: View?) {
        // 举例说明：Editor1 -> Editor2 -> Editor3
        if (changeRecord.currentChild === previousChild) {
            // Editor1在Editor1 -> Editor2的流程可能没有被立即移除，
            // previousChild是Editor2，确保移除Editor1再添加Editor3
            removeChangeRecordPrevious()
        }
        if (removePreviousImmediately) {
            // previousChild是Editor2，立即移除Editor2再添加Editor3
            previousChild?.let(::removeView)
        }
        requestLayout()
    }

    private fun removeChangeRecordPrevious() {
        val view = changeRecord.previousChild
        if (view?.parent === this) removeView(view)
    }

    fun dispatchImeShown(shown: Boolean): Boolean {
        val ime = ime ?: return false
        return when {
            current !== ime && shown -> showChecked(ime, controlIme = false)
            current === ime && !shown -> hideChecked(ime, controlIme = false)
            else -> false
        }
    }

    private fun handleImeShown(shown: Boolean, controlIme: Boolean) {
        val editText = editText ?: return
        if (shown) {
            editText.requestFocus()
            if (controlIme) editText.showIme()
        } else {
            editText.clearFocus()
            if (controlIme) editText.hideIme()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun checkedAdapter(): EditorAdapter<Editor> {
        return adapter as EditorAdapter<Editor>
    }

    class ChangeRecord(val previousChild: View? = null, val currentChild: View? = null)
}