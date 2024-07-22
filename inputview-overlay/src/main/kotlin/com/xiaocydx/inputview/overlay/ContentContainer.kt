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

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.xiaocydx.inputview.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcelable
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import com.xiaocydx.inputview.FragmentManagerCompat

/**
 * @author xcc
 * @date 2024/7/22
 */
internal class ContentContainer(context: Context) : FrameLayout(context) {
    private val views = mutableMapOf<Content, View?>()
    private val compat = FragmentManagerCompat()
    private var lastInsets: WindowInsets? = null
    private var removePreviousImmediately = true
    private var pendingChange: PendingChange? = null
    var current: Content? = null; private set
    lateinit var changeRecord: ChangeRecord; private set
    lateinit var adapter: ContentAdapter<*>; private set

    init {
        id = R.id.tag_content_container_id
        setAdapter(EmptyAdapter())
    }

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

    fun setAdapter(adapter: ContentAdapter<*>) {
        current?.let(::hideChecked)
        this.adapter = adapter
        if (views.isNotEmpty()) {
            removeAllViews()
            views.clear()
        }
        clearPendingChange()
        changeRecord = ChangeRecord()
        current = null
    }

    fun setRemovePreviousImmediately(immediately: Boolean) {
        removePreviousImmediately = immediately
        if (removePreviousImmediately) removeChangeRecordPrevious()
    }

    fun showChecked(content: Content): Boolean {
        if (current === content) return false
        val previous = current
        current = content
        setPendingChange(previous, current)
        requestLayout()
        return true
    }

    fun hideChecked(content: Content): Boolean {
        if (current !== content) return false
        current = null
        setPendingChange(content, current)
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
        clearPendingChange()
        if (previous === current) return false

        val currentChild: View?
        val previousChild = previous?.let(views::get)
        if (current != null && !views.contains(current)) {
            currentChild = checkedAdapter().onCreateView(this, current)
            require(currentChild?.parent == null) { "Content的视图存在parent" }
            views[current] = currentChild
        } else {
            currentChild = current?.let(views::get)
        }

        // 举例说明：Content1 -> Content2 -> Content3
        if (changeRecord.currentChild === previousChild) {
            // Content1在Content1 -> Content2的流程可能没有被立即移除，
            // previousChild是Content2，确保移除Content1再添加Content3
            removeChangeRecordPrevious()
        }
        if (removePreviousImmediately) {
            // previousChild是Content2，立即移除Content2再添加Content3
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

    private fun setPendingChange(previous: Content?, current: Content?) {
        if (pendingChange == null) {
            pendingChange = PendingChange(previous)
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

    @Suppress("UNCHECKED_CAST")
    private fun checkedAdapter() = adapter as ContentAdapter<Content>

    override fun removeViewAt(index: Int) {
        compat.removeViewAt { super.removeViewAt(index) }
    }

    override fun addView(child: View?, index: Int) {
        compat.addView { super.addView(child, index) }
    }

    private data class PendingChange(
        val previous: Content?,
        var immediately: Boolean = false
    )

    data class ChangeRecord(
        val previous: Content? = null,
        val current: Content? = null,
        val previousChild: View? = null,
        val currentChild: View? = null,
        val immediately: Boolean = false
    )
}