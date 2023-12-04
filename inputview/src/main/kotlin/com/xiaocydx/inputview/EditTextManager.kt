package com.xiaocydx.inputview

import android.graphics.Matrix
import android.view.MotionEvent
import android.widget.EditText
import androidx.core.view.OneShotPreDrawListener
import androidx.core.view.WindowInsetsCompat.Type.ime
import androidx.core.view.isVisible

/**
 * @author xcc
 * @date 2023/12/4
 */
class EditTextManager internal constructor(private val host: EditorHost) {
    private val point = FloatArray(2)
    private val location = IntArray(2)
    private var inverseMatrix: Matrix? = null
    private var window: ViewTreeWindow? = null
    private var pendingShowIme = false
    private var touchedRef: EditTextWeakRef? = null
    private var primaryRef: EditTextWeakRef? = null
    private val handleRefs = mutableMapOf<Int, EditTextWeakRef>()
    private val callback = HideTextSelectHandleOnAnimationStart()
    private var preDrawShowIme: OneShotPreDrawListener? = null

    var primaryEditText: EditText?
        get() = primaryRef?.get()
        set(value) {
            if (value === primaryRef?.get()) return
            primaryRef = value?.let(::EditTextWeakRef)
        }

    init {
        host.addAnimationCallback(callback)
    }

    fun put(editText: EditText) {
        // TODO: 清除弱引用为null的情况
        val key = editText.hashCode()
        val ref = handleRefs[key]
        if (ref?.get() != null) return
        handleRefs[key] = EditTextWeakRef(editText)
    }

    fun remove(editText: EditText) {
        // TODO: 清除弱引用为null的情况
        handleRefs.remove(editText.hashCode())
    }

    internal fun showIme() {
        val editText = requireNotNull(primaryEditText) { "" }
        pendingShowIme = !editText.isAttachedToWindow || window == null
        if (!pendingShowIme) window?.createWindowInsetsController(editText)?.show(ime())
    }

    internal fun hideIme() {
        // TODO: 清除弱引用为null的情况
        removePendingShowIme()
        // TODO: 取消当前获得焦点的edit
        val editText = primaryEditText ?: return
        window?.createWindowInsetsController(editText)?.hide(ime())
    }

    private fun removePendingShowIme() {
        preDrawShowIme?.removeListener()
        preDrawShowIme = null
        pendingShowIme = false
    }

    internal fun onAttachedToWindow(window: ViewTreeWindow) {
        this.window = window
        window.registerManager(this)
        // TODO: 先不加preDraw看下效果
        if (pendingShowIme && primaryEditText != null) showIme()
    }

    internal fun onDetachedFromWindow(window: ViewTreeWindow) {
        this.window = null
        window.unregisterManager(this)
        removePendingShowIme()
    }

    internal fun beforeTouchEvent(ev: MotionEvent) {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            touchedRef = findTouched(ev)
            touchedRef?.beforeTouchEvent(ev)
        }
    }

    internal fun afterTouchEvent(ev: MotionEvent) {
        touchedRef?.afterTouchEvent(ev, canHide = host.current !== host.ime)
    }

    private fun findTouched(ev: MotionEvent): EditTextWeakRef? {
        handleRefs.values.forEach action@{ ref ->
            val editText = ref.get() ?: return@action
            if (editText.isTouched(ev)) return ref
        }
        return null
    }

    private fun EditText.isTouched(ev: MotionEvent): Boolean {
        if (!isVisible) return false
        getLocationOnScreen(location)
        point[0] = ev.rawX - location[0]
        point[1] = ev.rawY - location[1]
        if (!matrix.isIdentity) {
            if (inverseMatrix == null) {
                inverseMatrix = Matrix()
            }
            matrix.invert(inverseMatrix)
            inverseMatrix!!.mapPoints(point)
        }
        return point[0] >= 0 && point[0] < right - left
                && point[1] >= 0 && point[1] < bottom - top
    }

    /**
     * 在实际场景中，交互可能是先选中[EditText]的文本内容，再点击其它地方切换[Editor]，
     * 当动画开始时，隐藏左右水滴状指示器，避免动画运行时不断跨进程通信，进而造成卡顿。
     */
    private inner class HideTextSelectHandleOnAnimationStart : ReplicableAnimationCallback {
        override fun onAnimationStart(state: AnimationState) {
            handleRefs.values.forEach { ref ->
                ref.takeIf { it.hasTextSelectHandleLeftRight() }
                    ?.hideTextSelectHandle(keepFocus = state.isIme(state.current))
            }
        }
    }
}