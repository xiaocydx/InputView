package com.xiaocydx.inputview.sample.common

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Outline
import android.os.Build
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.Window
import androidx.annotation.Px
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * [TypedValue.complexToDimensionPixelSize]的舍入逻辑，
 * 用于确保[dp]转换的px值，和xml解析转换的px值一致。
 */
@Px
private fun Float.toRoundingPx(): Int {
    return (if (this >= 0) this + 0.5f else this - 0.5f).toInt()
}

@get:Px
val Int.dp: Int
    get() = toFloat().dp

@get:Px
val Float.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toRoundingPx()

const val matchParent = ViewGroup.LayoutParams.MATCH_PARENT
const val wrapContent = ViewGroup.LayoutParams.WRAP_CONTENT

fun <V : View> V.overScrollNever(): V = apply {
    overScrollMode = View.OVER_SCROLL_NEVER
}

fun <V : View> V.layoutParams(
    width: Int,
    height: Int,
    block: ViewGroup.MarginLayoutParams.() -> Unit = {}
): V = apply {
    if (layoutParams != null) {
        updateLayoutParams {
            this.width = width
            this.height = height
            (this as? ViewGroup.MarginLayoutParams)?.block()
        }
    } else {
        layoutParams = ViewGroup.MarginLayoutParams(width, height).apply(block)
    }
}

inline fun View.onClick(crossinline block: () -> Unit) {
    setOnClickListener { block() }
}

suspend fun View.awaitPreDraw() {
    suspendCancellableCoroutine { cont ->
        doOnPreDraw { cont.resume(Unit) }
    }
}

fun View.setRoundRectOutlineProvider(@Px corners: Int) {
    clipToOutline = true
    outlineProvider = RoundRectOutlineProvider(corners.toFloat())
}

private class RoundRectOutlineProvider(@Px private val corners: Float) : ViewOutlineProvider() {
    override fun getOutline(view: View, outline: Outline) {
        outline.setRoundRect(0, 0, view.width, view.height, corners)
    }
}

/**
 * [ViewGroup.suppressLayout]的兼容函数
 */
fun ViewGroup.suppressLayoutCompat(suppress: Boolean) {
    if (Build.VERSION.SDK_INT >= 29) {
        suppressLayout(suppress)
    } else {
        hiddenSuppressLayout(suppress)
    }
}

/**
 * Android 10以下尝试调用[ViewGroup.suppressLayout]
 *
 * 多UI线程调用[ViewGroup.suppressLayout]，可能会尝试多次，这个影响可以接受。
 */
private var tryHiddenSuppressLayout = true

/**
 * 由于Android 10以下[ViewGroup.suppressLayout]是public的@hide函数，因此不用反射调用
 */
@SuppressLint("NewApi")
private fun ViewGroup.hiddenSuppressLayout(suppress: Boolean) {
    if (tryHiddenSuppressLayout) {
        try {
            suppressLayout(suppress)
        } catch (e: NoSuchMethodError) {
            tryHiddenSuppressLayout = false
        }
    }
}

fun <T : RecyclerView> T.disableItemAnimator() = apply { itemAnimator = null }

inline fun RecyclerView.addOnItemTouchListener(
    crossinline onInterceptTouchEvent: (rv: RecyclerView, ev: MotionEvent) -> Boolean = { _, _ -> false },
    crossinline onTouchEvent: (rv: RecyclerView, ev: MotionEvent) -> Unit = { _, _ -> },
    crossinline onRequestDisallowInterceptTouchEvent: (disallowIntercept: Boolean) -> Unit = {}
) = object : RecyclerView.OnItemTouchListener {
    override fun onInterceptTouchEvent(rv: RecyclerView, ev: MotionEvent): Boolean {
        return onInterceptTouchEvent(rv, ev)
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        onTouchEvent(rv, e)
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        onRequestDisallowInterceptTouchEvent(disallowIntercept)
    }
}.also(::addOnItemTouchListener)

inline fun ViewPager2.registerOnPageChangeCallback(
    crossinline onScrolled: (position: Int, positionOffset: Float, positionOffsetPixels: Int) -> Unit = { _, _, _ -> },
    crossinline onSelected: (position: Int) -> Unit = {},
    crossinline onScrollStateChanged: (state: Int) -> Unit = {}
): ViewPager2.OnPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
    override fun onPageScrolled(
        position: Int,
        positionOffset: Float,
        positionOffsetPixels: Int
    ) = onScrolled(position, positionOffset, positionOffsetPixels)

    override fun onPageSelected(position: Int) = onSelected(position)

    override fun onPageScrollStateChanged(state: Int) = onScrollStateChanged(state)
}.also(::registerOnPageChangeCallback)

var Window.isDispatchTouchEventEnabled: Boolean
    get() = callback is DisableDispatchTouchEvent
    set(value) {
        val callback = callback
        if (value && callback is DisableDispatchTouchEvent) {
            callback.detach()
        } else if (!value && callback !is DisableDispatchTouchEvent) {
            DisableDispatchTouchEvent(this).attach()
        }
    }

private class DisableDispatchTouchEvent(
    private val window: Window,
    private val delegate: Window.Callback = window.callback
) : Window.Callback by delegate {

    fun attach() = apply { window.callback = this }

    fun detach() = apply { window.callback = delegate }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) return false
        return delegate.dispatchTouchEvent(event)
    }
}