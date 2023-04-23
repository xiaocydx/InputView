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

import android.app.Activity
import android.app.Dialog
import android.graphics.*
import android.view.*
import android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
import android.view.animation.Interpolator
import androidx.core.view.*
import androidx.core.view.WindowInsetsCompat.Type.*
import com.xiaocydx.inputview.compat.isDispatchApplyInsetsFullscreenCompatEnabled
import com.xiaocydx.inputview.compat.modifyImeAnimationCompat
import com.xiaocydx.inputview.compat.restoreImeAnimationCompat
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
    ViewTreeWindow(window, gestureNavBarEdgeToEdge).attachToDecorView(
        statusBarEdgeToEdge, alwaysConsumeTypeMask, dispatchApplyWindowInsetsRoot
    )
}

/**
 * 初始化[InputView]所需的配置
 *
 * 该函数用于兼容已有的insets分发处理方案，例如单Activity多Fragment结构。
 *
 * **注意**：[window]需要通过[Window.checkDispatchApplyInsetsCompatibility]的检查。
 *
 * @param window [Activity.getWindow]或[Dialog.getWindow]。
 * @param inputView [Activity]或[Dialog]视图树的[InputView]。
 * @param gestureNavBarEdgeToEdge 是否启用手势导航栏边到边。
 * 若启用手势导航栏边到边，则去除手势导航栏间距，[EdgeToEdgeHelper]提供了实现边到边的辅助函数。
 */
fun InputView.Companion.init(
    window: Window,
    inputView: InputView,
    gestureNavBarEdgeToEdge: Boolean = false,
) {
    ViewTreeWindow(window, gestureNavBarEdgeToEdge).attachToInputView(inputView)
}

/**
 * 检查Android 11以下`ViewRootImpl.dispatchApplyInsets()`的兼容性
 *
 * 以Android 10显示IME为例：
 * 1. IME进程调用`WindowManagerService.setInsetsWindow()`，
 * 进而调用`DisplayPolicy.layoutWindowLw()`计算各项`insets`。
 *
 * 2. `window.attributes.flags`包含[FLAG_FULLSCREEN]，
 * 或`window.attributes.softInputMode`不包含[SOFT_INPUT_ADJUST_RESIZE]，
 * `DisplayPolicy.layoutWindowLw()`计算的`contentInsets`不会包含IME的数值。
 *
 * 3. `WindowManagerService`通知应用进程的`ViewRootImpl`重新设置`mPendingContentInsets`的数值，
 * 并申请下一帧布局，下一帧由于`mPendingContentInsets`跟`mAttachInfo.mContentInsets`的数值相等，
 * 因此不调用`ViewRootImpl.dispatchApplyInsets()`。
 */
private fun Window.checkDispatchApplyInsetsCompatibility() {
    check(!isFloating) {
        "InputView需要主题的windowIsFloating = false，" +
                "否则会导致视图树没有WindowInsets分发"
    }
    @Suppress("DEPRECATION")
    check(isDispatchApplyInsetsFullscreenCompatEnabled
            || (attributes.flags and FLAG_FULLSCREEN == 0)) {
        "InputView需要主题的windowFullscreen = false，" +
                "或window.attributes.flags不包含FLAG_FULLSCREEN，" +
                "否则会导致Android 11以下显示或隐藏IME不进行WindowInsets分发，" +
                "可以尝试Window.enableDispatchApplyInsetsFullscreenCompat()的兼容方案"
    }
    @Suppress("DEPRECATION")
    check(attributes.softInputMode and SOFT_INPUT_ADJUST_RESIZE != 0) {
        "InputView需要window.attributes.softInputMode包含SOFT_INPUT_ADJUST_RESIZE，" +
                "否则会导致Android 11以下显示或隐藏IME不进行WindowInsets分发，" +
                "可以调用Window.setSoftInputMode()设置SOFT_INPUT_ADJUST_RESIZE"
    }
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
    val gestureNavBarEdgeToEdge: Boolean
) : EdgeToEdgeHelper {
    private val decorView = window.decorView as ViewGroup
    private val initialized: Boolean
        get() = decorView.viewTreeWindow != null

    fun attachToDecorView(
        statusBarEdgeToEdge: Boolean,
        @InsetsType alwaysConsumeTypeMask: Int,
        dispatchApplyWindowInsetsRoot: View?
    ) {
        check(!initialized) { "InputView.init()只能调用一次" }
        @Suppress("DEPRECATION")
        window.setSoftInputMode(SOFT_INPUT_ADJUST_RESIZE)
        window.checkDispatchApplyInsetsCompatibility()

        val contentRef = decorView.children
            .firstOrNull { it is ViewGroup }?.let(::WeakReference)
        val rootRef = dispatchApplyWindowInsetsRoot
            ?.takeIf { it !== decorView }?.let(::WeakReference)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(decorView) { v, insets ->
            window.checkDispatchApplyInsetsCompatibility()
            val applyInsets = insets.consume(alwaysConsumeTypeMask)
            val decorInsets = applyInsets.toDecorInsets(statusBarEdgeToEdge)
            ViewCompat.onApplyWindowInsets(v, decorInsets)
            contentRef?.get()?.updateMargins(
                top = decorInsets.statusBarHeight,
                bottom = decorInsets.navigationBarHeight
            )

            rootRef?.get()?.let { root ->
                ViewCompat.dispatchApplyWindowInsets(root, applyInsets)
                WindowInsetsCompat.CONSUMED
            } ?: applyInsets
        }
        decorView.viewTreeWindow = this
    }

    fun attachToInputView(inputView: InputView) {
        check(!initialized) { "InputView.init()只能调用一次" }
        window.checkDispatchApplyInsetsCompatibility()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(inputView) { v, insets ->
            window.checkDispatchApplyInsetsCompatibility()
            v.updateMargins(bottom = when {
                insets.supportGestureNavBarEdgeToEdge(v) -> 0
                else -> insets.navigationBarHeight
            })
            insets
        }
        decorView.viewTreeWindow = this
        inputView.viewTreeWindow = this
    }

    private fun WindowInsetsCompat.toDecorInsets(statusBarEdgeToEdge: Boolean): WindowInsetsCompat {
        var insets = this
        if (statusBarEdgeToEdge) {
            if (window.statusBarColor != Color.TRANSPARENT) {
                // Color.TRANSPARENT用于兼容部分设备仍然绘制背景色的问题
                window.statusBarColor = Color.TRANSPARENT
            }
            insets = insets.consume(statusBars())
        }
        if (supportGestureNavBarEdgeToEdge(decorView)) {
            insets = insets.consume(navigationBars())
        }
        return insets
    }

    val WindowInsetsCompat.imeHeight
        get() = getInsets(ime()).bottom

    val WindowInsetsCompat.imeOffset: Int
        get() {
            if (supportGestureNavBarEdgeToEdge(decorView)) return imeHeight
            return (imeHeight - navigationBarHeight).coerceAtLeast(0)
        }

    val WindowInsetsCompat.navigationBarOffset: Int
        get() = if (supportGestureNavBarEdgeToEdge(decorView)) navigationBarHeight else 0

    override fun WindowInsetsCompat.supportGestureNavBarEdgeToEdge(view: View): Boolean {
        return gestureNavBarEdgeToEdge && isGestureNavigationBar(view)
    }

    /**
     * 传入[view]是为了确保转换出的[WindowInsetsCompat]是正确的结果
     */
    fun WindowInsets.toCompat(view: View): WindowInsetsCompat {
        return WindowInsetsCompat.toWindowInsetsCompat(this, view)
    }

    fun WindowInsetsAnimationCompat.containsImeType(): Boolean {
        return typeMask and ime() == ime()
    }

    fun getRootWindowInsets(): WindowInsetsCompat? {
        return ViewCompat.getRootWindowInsets(decorView)
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