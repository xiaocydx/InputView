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
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.ClassLoaderCreator
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.customview.view.AbsSavedState
import com.xiaocydx.inputview.EditorContainer.SavedState.CREATOR.NO_EDITOR_INDEX

/**
 * [InputView]的编辑区，负责管理[Editor]
 *
 * @author xcc
 * @date 2023/1/7
 */
internal class EditorContainer(context: Context) : FrameLayout(context) {
    private val views = mutableMapOf<Editor, View?>()
    private val compat = FragmentManagerCompat()
    private var lastInsets: WindowInsets? = null
    private var handler: ImeFocusHandler? = null
    private var removePreviousImmediately = true
    private var pendingChange: PendingChange? = null
    private var pendingSavedState: SavedState? = null
    private var pendingRestoreAction: (() -> Unit)? = null
    private var dispatchingChanged: DispatchingChanged? = null
    var ime: Editor? = null; private set
    var current: Editor? = null; private set
    var changeRecord = ChangeRecord(); private set
    lateinit var adapter: EditorAdapter<*>; private set

    init {
        id = R.id.tag_editor_container_id
        setAdapter(ImeAdapter())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?) = true

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        // Android 9.0以下的WindowInsets可变（compat模块已兼容）
        lastInsets = insets
        return super.onApplyWindowInsets(insets)
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>?) {
        dispatchFreezeSelfOnly(container)
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>?) {
        dispatchThawSelfOnly(container)
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        // current = null也需要保存SavedState，确保重建后的视图初始化阶段显示Editor不运行动画
        if (current == null) return SavedState(superState, NO_EDITOR_INDEX)
        val editorIndex = checkedAdapter().getStatefulEditorList().indexOfFirst { it === current }
        return SavedState(superState, editorIndex)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        var superState = state
        if (state is SavedState) {
            pendingSavedState = state
            superState = state.superState
        }
        super.onRestoreInstanceState(superState)
        pendingRestoreAction?.invoke()
    }

    fun setPendingRestoreAction(action: (() -> Unit)? = null) {
        if (action != null && pendingSavedState != null) return action()
        pendingRestoreAction = action
    }

    fun peekPendingRestoreEditor(): Editor? {
        if (pendingChange != null) return null
        val editorIndex = pendingSavedState?.editorIndex ?: return null
        return checkedAdapter().getStatefulEditorList().getOrNull(editorIndex)
    }

    fun consumePendingSavedState() {
        if (pendingSavedState != null) {
            // pendingChange可能是恢复restoreEditor的变更，
            // 或者是重建后，视图初始化阶段显示Editor的变更。
            // 注意：显示IME的变更仍然运行动画，因为无法控制IME的调度逻辑，
            // 在onSaveInstanceState()之前，可能分发隐藏IME的WindowInsets，
            // 在重建恢复restoreEditor后，也可能分发隐藏IME的WindowInsets。
            pendingChange?.immediately = current !== ime
        }
        pendingSavedState = null
        pendingRestoreAction = null
    }

    fun setAdapter(adapter: EditorAdapter<*>) {
        current?.let(::hideChecked)
        this.adapter = adapter
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

    fun setImeFocusHandler(handler: ImeFocusHandler?) {
        // 设置新的handler，不做requestFocus()补偿处理
        this.handler = handler
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
        if (previous === ime) {
            handleImeShown(shown = false, controlIme)
        } else if (current === ime) {
            handleImeShown(shown = true, controlIme)
        }
        dispatchChanged(previous)
        requestLayout()
        return true
    }

    fun hideChecked(editor: Editor, controlIme: Boolean = true): Boolean {
        if (current !== editor) return false
        current = null
        setPendingChange(editor, current)
        if (editor === ime) handleImeShown(shown = false, controlIme)
        dispatchChanged(editor)
        requestLayout()
        return true
    }

    fun hasPendingChange(): Boolean {
        return pendingChange != null
    }

    fun consumePendingChange(): Boolean {
        if (pendingChange == null) return false
        val previous = pendingChange!!.previous
        val current = current
        val immediately = pendingChange!!.immediately
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
        changeRecord = ChangeRecord(previous, current, previousChild, currentChild, immediately)
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
        pendingChange!!.immediately = false
    }

    private fun clearPendingChange() {
        pendingChange = null
    }

    private fun removeChangeRecordPrevious() {
        val view = changeRecord.previousChild
        if (view?.parent === this) removeView(view)
    }

    private fun dispatchChanged(previous: Editor?) {
        var dispatching = dispatchingChanged
        if (dispatching != null) {
            // 分发过程调用showChecked()或hideChecked()，
            // 记录下这一次的previous，无效正在分发的过程，
            // 基于记录的previous重新分发。
            dispatching.previous = previous
            dispatching.invalidated = true
            return
        }
        dispatching = DispatchingChanged(previous)
        dispatchingChanged = dispatching
        do {
            dispatching.invalidated = false
            if (dispatching.previous === current) break
            checkedAdapter().dispatchChanged(dispatching.previous, current, dispatching)
        } while (dispatching.invalidated)
        dispatchingChanged = null
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
        val handler = handler ?: return
        if (shown) {
            if (controlIme) {
                handler.requestFocus()
                handler.showIme()
            } else {
                handler.requestCurrentFocus()
            }
        } else {
            handler.clearCurrentFocus()
            if (controlIme) handler.hideIme()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun checkedAdapter() = adapter as EditorAdapter<Editor>

    override fun removeViewAt(index: Int) {
        compat.removeViewAt { super.removeViewAt(index) }
    }

    override fun addView(child: View?, index: Int) {
        compat.addView { super.addView(child, index) }
    }

    private data class PendingChange(
        val previous: Editor?,
        var waitDispatchImeShown: Boolean = false,
        var immediately: Boolean = false
    )

    private data class DispatchingChanged(
        var previous: Editor?,
        var invalidated: Boolean = false,
    ) : DispatchInvalidated {
        override fun invoke() = invalidated
    }

    data class ChangeRecord(
        val previous: Editor? = null,
        val current: Editor? = null,
        val previousChild: View? = null,
        val currentChild: View? = null,
        val immediately: Boolean = false
    )

    class SavedState : AbsSavedState {
        var editorIndex = NO_EDITOR_INDEX
            private set

        constructor(
            source: Parcel,
            loader: ClassLoader?
        ) : super(source, loader) {
            editorIndex = source.readInt().coerceAtLeast(NO_EDITOR_INDEX)
        }

        constructor(
            superState: Parcelable?,
            editorIndex: Int
        ) : super(superState ?: EMPTY_STATE) {
            this.editorIndex = editorIndex.coerceAtLeast(NO_EDITOR_INDEX)
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(editorIndex)
        }

        companion object CREATOR : ClassLoaderCreator<SavedState?> {
            const val NO_EDITOR_INDEX = -1

            override fun createFromParcel(source: Parcel, loader: ClassLoader?): SavedState {
                return SavedState(source, loader)
            }

            override fun createFromParcel(source: Parcel): SavedState {
                return SavedState(source, loader = null)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }
}