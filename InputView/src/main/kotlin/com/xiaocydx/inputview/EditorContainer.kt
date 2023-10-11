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
import android.os.Parcelable
import android.view.View
import android.widget.FrameLayout

/**
 * [InputView]的编辑区，负责管理[Editor]
 *
 * @author xcc
 * @date 2023/1/7
 */
internal class EditorContainer(context: Context) : FrameLayout(context) {
    private val views = mutableMapOf<Editor, View?>()
    private var editText: EditTextHolder? = null
    private var removePreviousImmediately = true
    private var pendingPrevious: Editor? = NopEditor
    var ime: Editor? = null; private set
    var current: Editor? = null; private set
    var changeRecord = ChangeRecord(); private set
    lateinit var adapter: EditorAdapter<*>; private set

    init {
        id = R.id.tag_editor_container_id
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
        clearPendingPrevious()
    }

    fun setEditTextHolder(editText: EditTextHolder?) {
        this.editText = editText
    }

    fun setRemovePreviousImmediately(immediately: Boolean) {
        removePreviousImmediately = immediately
        if (removePreviousImmediately) removeChangeRecordPrevious()
    }

    fun showChecked(editor: Editor, controlIme: Boolean = true): Boolean {
        if (current === editor) return false
        val previous = current
        current = editor
        setPendingPrevious(previous)
        if (previous === ime) {
            handleImeShown(shown = false, controlIme)
        } else if (current === ime) {
            handleImeShown(shown = true, controlIme)
        }
        checkedAdapter().onEditorChanged(previous, current)
        requestLayout()
        return true
    }

    fun hideChecked(editor: Editor, controlIme: Boolean = true): Boolean {
        if (current !== editor) return false
        current = null
        setPendingPrevious(editor)
        if (editor === ime) handleImeShown(shown = false, controlIme)
        checkedAdapter().onEditorChanged(editor, current)
        requestLayout()
        return true
    }

    fun consumePendingChange(): Boolean {
        if (!hasPendingPrevious()) return false
        val current = current
        val previous = pendingPrevious
        clearPendingPrevious()
        if (current === previous) return false

        val currentChild: View?
        val previousChild = previous?.let(views::get)
        var isCurrentAdded = false
        if (current != null && !views.contains(current)) {
            val result = when {
                current === ime -> null
                else -> checkedAdapter().createView(this, current)
            }
            isCurrentAdded = result?.isAdded == true
            currentChild = result?.view
            require(isCurrentAdded || currentChild?.parent == null) { "Editor的视图存在parent" }
            views[current] = currentChild
        } else {
            currentChild = current?.let(views::get)
        }

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
        if (!isCurrentAdded) currentChild?.let(::addView)

        changeRecord = ChangeRecord(previous, current, previousChild, currentChild)
        return true
    }

    private fun hasPendingPrevious() = pendingPrevious !== NopEditor

    private fun setPendingPrevious(previous: Editor?) {
        if (!hasPendingPrevious()) pendingPrevious = previous
    }

    private fun clearPendingPrevious() {
        pendingPrevious = NopEditor
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
    private fun checkedAdapter() = adapter as EditorAdapter<Editor>

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
        // 移除Fragment重建流程添加的child，通过consumePendingChange()重新添加回来
        if (childCount > 0) removeAllViews()
    }

    data class ChangeRecord(
        val previous: Editor? = null,
        val current: Editor? = null,
        val previousChild: View? = null,
        val currentChild: View? = null
    )

    private companion object NopEditor : Editor
}