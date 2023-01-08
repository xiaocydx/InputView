package com.xiaocydx.inputview

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw

object Ime : EditorType

class DefaultEditorAdapter : EditorAdapter<Ime>() {
    override val types: List<Ime> = listOf(Ime)

    override fun isImeType(type: Ime): Boolean = type === Ime

    override fun onCreateView(parent: ViewGroup, type: Ime): View? = null
}

class DefaultEditorAnimator private constructor(private val isResize: Boolean) : EditorAnimator() {

    override fun transform(startOffset: Int, endOffset: Int, currentOffset: Int) {
        val inputView = inputView ?: return
        if (isResize) {
            inputView.offsetContentSize(currentOffset)
        } else {
            val previousOffset = inputView.editorOffset
            val diff = previousOffset - currentOffset
            inputView.offsetChildrenLocation(currentOffset, diff)
        }
    }

    companion object {
        fun pan() = DefaultEditorAnimator(isResize = false)

        fun resize() = DefaultEditorAnimator(isResize = true)
    }
}

@SuppressLint("MissingSuperCall")
class NopEditorAnimator private constructor(private val isResize: Boolean) : EditorAnimator() {
    private var isCurrentIme = false

    override fun onVisibleChanged(previous: EditorType?, current: EditorType?) {
        isCurrentIme = isImeType(current)
        if (isCurrentIme) return
        inputView?.doOnPreDraw { transform(getEditorEndOffset()) }
    }

    override fun onImeAnimationStart(endValue: Int) {
        if (!isCurrentIme) return
        transform(endValue)
    }

    override fun onImeAnimationUpdate(currentValue: Int) = Unit

    override fun onImeAnimationEnd() = Unit

    private fun transform(currentOffset: Int) {
        transform(startOffset = 0, endOffset = 0, currentOffset)
    }

    override fun transform(startOffset: Int, endOffset: Int, currentOffset: Int) {
        val inputView = inputView ?: return
        if (isResize) {
            inputView.offsetContentSize(currentOffset)
        } else {
            val previousOffset = inputView.editorOffset
            val diff = previousOffset - currentOffset
            inputView.offsetChildrenLocation(currentOffset, diff)
        }
    }

    companion object {
        fun pan() = NopEditorAnimator(isResize = false)

        fun resize() = NopEditorAnimator(isResize = true)
    }
}