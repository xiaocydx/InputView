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

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting

/**
 * [InputView]的编辑区，负责管理[Editor]
 *
 * @author xcc
 * @date 2023/1/7
 */
internal class EditorContainer(context: Context) : FrameLayout(context) {
    private val views = mutableMapOf<Editor, View?>()
    private var lastInsets: WindowInsets? = null
    private var editText: EditTextHolder? = null
    private var isCheckControlImeEnabled = true
    private var removePreviousImmediately = true
    private var pendingChange: PendingChange? = null
    var ime: Editor? = null; private set
    var current: Editor? = null; private set
    var changeRecord = ChangeRecord(); private set
    lateinit var adapter: EditorAdapter<*>; private set

    init {
        id = R.id.tag_editor_container_id
        setAdapter(ImeAdapter())
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        // Android 9.0以下的WindowInsets可变（Reflect模块已兼容）
        lastInsets = insets
        return super.onApplyWindowInsets(insets)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?) = true

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
        clearPendingChange()
    }

    fun setEditTextHolder(editText: EditTextHolder?) {
        this.editText = editText
    }

    @VisibleForTesting
    fun setCheckControlImeEnabled(isEnabled: Boolean) {
        isCheckControlImeEnabled = isEnabled
    }

    fun setRemovePreviousImmediately(immediately: Boolean) {
        removePreviousImmediately = immediately
        if (removePreviousImmediately) removeChangeRecordPrevious()
    }

    fun showChecked(editor: Editor, controlIme: Boolean = true): Boolean {
        if (current === editor) return false
        val previous = current
        current = editor
        setPendingChange(previous, current)
        var requestFocus = false
        val prevEditText = editText
        if (previous === ime) {
            handleImeShown(shown = false, controlIme)
        } else if (current === ime) {
            requestFocus = true
            handleImeShown(shown = true, controlIme)
        }
        checkedAdapter().onEditorChanged(previous, current)
        if (requestFocus && prevEditText !== editText) {
            // onEditorChanged()的分发过程重新设置了editText，
            // 此时对ediText补偿requestFocus()，确保获得焦点。
            handleImeShown(shown = true, controlIme = false)
        }
        requestLayout()
        return true
    }

    fun hideChecked(editor: Editor, controlIme: Boolean = true): Boolean {
        if (current !== editor) return false
        current = null
        setPendingChange(editor, current)
        if (editor === ime) handleImeShown(shown = false, controlIme)
        checkedAdapter().onEditorChanged(editor, current)
        requestLayout()
        return true
    }

    fun hasPendingChange(): Boolean = pendingChange != null

    fun consumePendingChange(): Boolean {
        if (pendingChange == null) return false
        val previous = pendingChange?.previous
        val current = current
        if (pendingChange!!.waitDispatchImeShown) {
            // previous和current都是ime，属于无效等待情况
            if (previous === current) clearPendingChange()
            return false
        }

        clearPendingChange()
        if (previous === current) return false

        val currentChild: View?
        val previousChild = previous?.let(views::get)
        if (current != null && !views.contains(current)) {
            currentChild = when {
                current === ime -> null
                else -> checkedAdapter().onCreateView(this, current)
            }
            require(currentChild?.parent == null) { "Editor的视图存在parent" }
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
        currentChild?.let(::addView)
        if (lastInsets != null && currentChild != null) {
            // 布局阶段，child申请的WindowInsets分发在下一帧进行,
            // 此时对child补偿WindowInsets分发，确保布局阶段之后，
            // 创建动画的过程能捕获到child正确的尺寸。
            currentChild.dispatchApplyWindowInsets(lastInsets)
        }
        changeRecord = ChangeRecord(previous, current, previousChild, currentChild)
        return true
    }

    private fun setPendingChange(previous: Editor?, current: Editor?) {
        if (pendingChange == null) {
            pendingChange = PendingChange(previous)
            pendingChange!!.waitDispatchImeShown = previous === ime
        }
        if (pendingChange!!.previous !== ime) {
            // pendingChange.previous不需要wait，最后的current决定是否wait
            pendingChange?.waitDispatchImeShown = current === ime
        }
    }

    private fun clearPendingChange() {
        pendingChange = null
    }

    private fun removeChangeRecordPrevious() {
        val view = changeRecord.previousChild
        if (view?.parent === this) removeView(view)
    }

    fun dispatchImeShown(shown: Boolean): Boolean {
        val ime = ime
        val changed = when {
            ime == null -> false
            current !== ime && shown -> showChecked(ime, controlIme = false)
            current === ime && !shown -> hideChecked(ime, controlIme = false)
            else -> false
        }
        if (pendingChange?.waitDispatchImeShown == true) {
            pendingChange?.waitDispatchImeShown = false
            requestLayout()
        }
        return changed
    }

    private fun handleImeShown(shown: Boolean, controlIme: Boolean) {
        val editText = editText
        require(!isCheckControlImeEnabled || !controlIme || editText != null) {
            "未对InputView设置EditText，无法主动${if (shown) "显示" else "隐藏"}IME"
        }
        editText ?: return
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

    private data class PendingChange(
        val previous: Editor?,
        var waitDispatchImeShown: Boolean = false
    )

    data class ChangeRecord(
        val previous: Editor? = null,
        val current: Editor? = null,
        val previousChild: View? = null,
        val currentChild: View? = null
    )
}