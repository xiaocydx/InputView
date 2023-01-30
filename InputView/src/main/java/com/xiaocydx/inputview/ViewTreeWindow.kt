package com.xiaocydx.inputview

import android.app.Activity
import android.app.Dialog
import android.graphics.*
import android.view.*
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
import android.view.animation.Interpolator
import androidx.annotation.CheckResult
import androidx.core.graphics.Insets
import androidx.core.view.*
import androidx.core.view.WindowInsetsCompat.Type.InsetsType
import java.lang.ref.WeakReference

/**
 * 初始化[InputView]所需的配置
 *
 * @param window [Activity.getWindow]或[Dialog.getWindow]。
 *
 * @param statusBarEdgeToEdge 是否启用状态栏边到边。
 * 若启用状态栏边到边，则去除状态栏间距和背景色，[EdgeToEdgeHelper]提供了实现边到边的辅助函数。
 *
 * @param gestureNavBarEdgeToEdge 是否启用手势导航栏边到边。
 * 若启用手势导航栏边到边，则去除手势导航栏间距和背景色，[EdgeToEdgeHelper]提供了实现边到边的辅助函数。
 *
 * @param alwaysConsumeTypeMask 总是消费的[InsetsType]类型集。
 * 例如隐藏导航栏且总是消费导航栏类型，视图树无需实现导航栏间距和手势导航栏边到边：
 * ```
 * val controller = WindowInsetsControllerCompat(window, window.decorView)
 * controller.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
 * controller.hide(WindowInsetsCompat.Type.navigationBars())
 *
 * InputView.init(
 *     window = window,
 *     alwaysConsumeTypeMask = WindowInsetsCompat.Type.navigationBars()
 * ）
 * ```
 *
 * @param dispatchApplyWindowInsetsRoot 直接分发insets的`root`。
 * 若`root != null`，则实际效果等同于：
 * ```
 * val root = dispatchApplyWindowInsetsRoot
 * ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
 *     val applyInsets = insets.consume(alwaysConsumeTypeMask)
 *     ViewCompat.dispatchApplyWindowInsets(root, applyInsets)
 *     WindowInsetsCompat.CONSUMED
 * }
 * ```
 * 不对`window.decorView`到`root`之间的View分发`applyInsets`，
 * 作用是去除中间View的边到边实现，自行实现状态栏和导航栏边到边，
 * 以及确保`root`的insets分发正常和Android 11以下insets动画回调正常，
 * 自行实现边到边虽然有点麻烦，但是会更加灵活，能满足实际场景不同的需求。
 */
fun InputView.Companion.init(
    window: Window,
    statusBarEdgeToEdge: Boolean = false,
    gestureNavBarEdgeToEdge: Boolean = false,
    @InsetsType alwaysConsumeTypeMask: Int = 0,
    dispatchApplyWindowInsetsRoot: View? = null
) {
    ViewTreeWindow(
        window,
        statusBarEdgeToEdge,
        gestureNavBarEdgeToEdge,
        alwaysConsumeTypeMask,
        dispatchApplyWindowInsetsRoot
    ).attach()
}

/**
 * 视图树的[ViewTreeWindow]
 */
private var View.viewTreeWindow: ViewTreeWindow?
    get() = getTag(R.id.tag_view_tree_window) as? ViewTreeWindow
    set(value) {
        setTag(R.id.tag_view_tree_window, value)
    }

/**
 * 查找视图树的[ViewTreeWindow]，若查找不到，则返回`null`
 */
internal fun View.findViewTreeWindow(): ViewTreeWindow? {
    var found: ViewTreeWindow? = viewTreeWindow
    if (found != null) return found
    var parent: ViewParent? = parent
    while (found == null && parent is View) {
        found = parent.viewTreeWindow
        parent = (parent as View).parent
    }
    return found
}

/**
 * 获取视图树的[ViewTreeWindow]，若获取不到，则查找并设置
 */
internal fun View.getOrFindViewTreeWindow(): ViewTreeWindow? {
    return viewTreeWindow ?: findViewTreeWindow()?.also { viewTreeWindow = it }
}

internal class ViewTreeWindow(
    private val window: Window,
    private val statusBarEdgeToEdge: Boolean,
    private val gestureNavBarEdgeToEdge: Boolean,
    @InsetsType private val alwaysConsumeTypeMask: Int,
    dispatchApplyWindowInsetsRoot: View?
) {
    private val statusBarType = WindowInsetsCompat.Type.statusBars()
    private val navBarType = WindowInsetsCompat.Type.navigationBars()
    private val imeType = WindowInsetsCompat.Type.ime()
    private val rootRef = dispatchApplyWindowInsetsRoot
        ?.takeIf { it !== window.decorView }?.let(::WeakReference)

    fun attach() {
        window.checkDispatchApplyWindowInsetsCompatibility()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val content = (window.decorView as ViewGroup).children.first { it is ViewGroup }
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            val applyInsets = insets.toApplyInsets()
            val decorInsets = applyInsets.toDecorInsets()
            ViewCompat.onApplyWindowInsets(v, decorInsets)
            content.updateMargins(
                top = decorInsets.statusBarHeight,
                bottom = decorInsets.navigationBarHeight
            )

            rootRef?.get()?.let { root ->
                ViewCompat.dispatchApplyWindowInsets(root, applyInsets)
                WindowInsetsCompat.CONSUMED
            } ?: applyInsets
        }
        window.decorView.viewTreeWindow = this
    }

    /**
     * 检查Android 11以下`ViewRootImpl.dispatchApplyInsets()`的兼容性
     *
     * 以Android 10显示IME为例：
     * 1. IME进程调用`WindowManagerService.setInsetsWindow()`，
     * 进而调用`DisplayPolicy.layoutWindowLw()`计算各项`insets`。
     *
     * 2. `window.attributes.flags`包含[FLAG_FULLSCREEN]，
     * 或`window.attributes.softInputMode`不是[SOFT_INPUT_ADJUST_RESIZE]，
     * `DisplayPolicy.layoutWindowLw()`计算的`contentInsets`不会减去IME的数值。
     *
     * 3. `WindowManagerService`通知应用进程的`ViewRootImpl`重新设置`mPendingContentInsets`的数值，
     * 并申请下一帧布局，下一帧由于`mPendingContentInsets`跟`mAttachInfo.mContentInsets`的数值相等，
     * 因此不调用`ViewRootImpl.dispatchApplyInsets()`。
     */
    private fun Window.checkDispatchApplyWindowInsetsCompatibility() {
        check(decorView.viewTreeWindow == null) { "InputView.init()只能调用一次" }
        check(!isFloating) {
            "InputView需要主题的windowIsFloating = false，" +
                    "否则会导致视图树无法自行处理WindowInsets分发"
        }
        @Suppress("DEPRECATION")
        check(attributes.flags and FLAG_FULLSCREEN == 0) {
            "InputView需要主题的windowFullscreen = false，" +
                    "或window.attributes.flags不包含FLAG_FULLSCREEN，" +
                    "否则会导致Android 11以下显示或隐藏IME不进行WindowInsets分发"
        }
        @Suppress("DEPRECATION")
        window.setSoftInputMode(SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun WindowInsetsCompat.toApplyInsets(): WindowInsetsCompat {
        return if (alwaysConsumeTypeMask > 0) consume(alwaysConsumeTypeMask) else this
    }

    private fun WindowInsetsCompat.toDecorInsets(): WindowInsetsCompat {
        var insets = this
        if (statusBarEdgeToEdge) {
            if (window.statusBarColor != Color.TRANSPARENT) {
                // Color.TRANSPARENT用于兼容部分设备仍然绘制背景色的问题
                window.statusBarColor = Color.TRANSPARENT
            }
            insets = insets.consume(statusBarType)
        }
        if (supportGestureNavBarEdgeToEdge) {
            insets = insets.consume(navBarType)
        }
        return insets
    }

    @CheckResult
    private fun WindowInsetsCompat.consume(typeMask: Int): WindowInsetsCompat {
        return WindowInsetsCompat.Builder(this).setInsets(typeMask, Insets.NONE).build()
    }

    private fun View.updateMargins(
        left: Int = marginLeft,
        top: Int = marginTop,
        right: Int = marginRight,
        bottom: Int = marginBottom
    ) {
        val params = layoutParams as? MarginLayoutParams ?: return
        val changed = left != marginLeft || top != marginTop
                || right != marginTop || bottom != marginBottom
        params.setMargins(left, top, right, bottom)
        if (changed) layoutParams = params
    }

    private val WindowInsetsCompat.isGestureNavigationBar: Boolean
        get() {
            val threshold = (24 * window.decorView.resources.displayMetrics.density).toInt()
            val stableHeight = getInsetsIgnoringVisibility(navBarType).bottom
            return stableHeight <= threshold.coerceAtLeast(66)
        }

    private val WindowInsetsCompat.statusBarHeight: Int
        get() = getInsets(statusBarType).top

    private val WindowInsetsCompat.navigationBarHeight: Int
        get() = getInsets(navBarType).bottom

    val WindowInsetsCompat.imeHeight
        get() = getInsets(imeType).bottom

    val WindowInsetsCompat.imeOffset: Int
        get() {
            if (supportGestureNavBarEdgeToEdge) return imeHeight
            return (imeHeight - navigationBarHeight).coerceAtLeast(0)
        }

    val WindowInsetsCompat.navigationBarOffset: Int
        get() = if (supportGestureNavBarEdgeToEdge) navigationBarHeight else 0

    val WindowInsetsCompat.supportGestureNavBarEdgeToEdge: Boolean
        get() = gestureNavBarEdgeToEdge && isGestureNavigationBar

    /**
     * 传入[view]是为了确保转换出的[WindowInsetsCompat]是正确的结果
     */
    fun WindowInsets.toCompat(view: View): WindowInsetsCompat {
        return WindowInsetsCompat.toWindowInsetsCompat(this, view)
    }

    fun WindowInsetsAnimationCompat.containsImeType(): Boolean {
        return typeMask and imeType == imeType
    }

    fun getRootWindowInsets(): WindowInsetsCompat? {
        return ViewCompat.getRootWindowInsets(window.decorView)
    }

    fun createWindowInsetsController(editText: View): WindowInsetsControllerCompat {
        return WindowInsetsControllerCompat(window, editText)
    }

    fun modifyImeAnimation(durationMillis: Long, interpolator: Interpolator) {
        window.modifyImeAnimationCompat(durationMillis, interpolator)
    }

    fun restoreImeAnimation() {
        window.restoreImeAnimationCompat()
    }
}