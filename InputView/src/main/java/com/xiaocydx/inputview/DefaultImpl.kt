package com.xiaocydx.inputview

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
import androidx.core.view.doOnPreDraw

/**
 * [Editor]的IME默认实现
 */
object Ime : Editor

/**
 * [EditorAdapter]的默认实现，支持只需要IME的场景
 */
class DefaultEditorAdapter : EditorAdapter<Ime>() {
    override val editors: List<Ime> = listOf(Ime)

    override fun isIme(editor: Ime): Boolean = editor === Ime

    override fun onCreateView(parent: ViewGroup, editor: Ime): View? = null
}

/**
 * [EditorAnimator]的默认实现
 */
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
        /**
         * 运行动画平移`contentView`，类似[SOFT_INPUT_ADJUST_PAN]
         */
        fun pan() = DefaultEditorAnimator(isResize = false)

        /**
         * 运行动画修改`contentView`的尺寸，类似[SOFT_INPUT_ADJUST_RESIZE]
         */
        fun resize() = DefaultEditorAnimator(isResize = true)
    }
}

/**
 * [EditorAnimator]不运行动画的实现
 */
@SuppressLint("MissingSuperCall")
class NopEditorAnimator private constructor(private val isResize: Boolean) : EditorAnimator() {
    private var isCurrentIme = false

    override fun onVisibleChanged(previous: Editor?, current: Editor?) {
        isCurrentIme = isIme(current)
        if (isCurrentIme) return
        inputView?.doOnPreDraw { transform(getEditorHeight()) }
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
        /**
         * 不运行动画平移`contentView`，类似[SOFT_INPUT_ADJUST_PAN]
         */
        fun pan() = NopEditorAnimator(isResize = false)

        /**
         * 不运行动画修改`contentView`的尺寸，类似[SOFT_INPUT_ADJUST_RESIZE]
         */
        fun resize() = NopEditorAnimator(isResize = true)
    }
}