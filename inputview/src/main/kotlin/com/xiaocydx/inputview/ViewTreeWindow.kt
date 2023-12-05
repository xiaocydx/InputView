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
import android.widget.EditText
import androidx.core.view.*
import androidx.core.view.WindowInsetsCompat.Type.*
import com.xiaocydx.inputview.compat.*
import java.lang.ref.WeakReference

/**
 * 初始化[InputView]所需的配置
 *
 * @param window [Activity.getWindow]或[Dialog.getWindow]。
 * @param statusBarEdgeToEdge 是否启用状态栏EdgeToEdge。
 * 若启用状态栏EdgeToEdge，则去除状态栏间距和背景色，[EdgeToEdgeHelper]提供实现EdgeToEdge的辅助函数。
 * @param gestureNavBarEdgeToEdge 是否启用手势导航栏EdgeToEdge。
 * 若启用手势导航栏EdgeToEdge，则去除手势导航栏间距和背景色，[EdgeToEdgeHelper]提供实现EdgeToEdge的辅助函数。
 */
fun InputView.Companion.init(
    window: Window,
    statusBarEdgeToEdge: Boolean = false,
    gestureNavBarEdgeToEdge: Boolean = false
) {
    ViewTreeWindow(window, gestureNavBarEdgeToEdge).attach()
        .setOnApplyWindowInsetsListener(statusBarEdgeToEdge)
}

/**
 * 初始化[InputView]所需的配置
 *
 * 该函数用于兼容已有的[WindowInsets]分发处理方案。
 *
 * @param window [Activity.getWindow]或[Dialog.getWindow]。
 * @param gestureNavBarEdgeToEdge 是否启用手势导航栏EdgeToEdge。
 * 若启用手势导航栏EdgeToEdge，则去除手势导航栏间距，[EdgeToEdgeHelper]提供了实现EdgeToEdge的辅助函数。
 */
fun InputView.Companion.initCompat(
    window: Window,
    gestureNavBarEdgeToEdge: Boolean = false,
) {
    ViewTreeWindow(window, gestureNavBarEdgeToEdge).attach()
}

/**
 * 添加需要处理水滴状指示器的[editText]，再次添加[editText]返回`false`，
 * 内部实现通过弱引用的方式持有[editText]，因此不必担心会有内存泄漏问题，
 * 调用该函数之前，需要先调用[init]或[initCompat]完成初始化。
 *
 * 水滴状指示器的处理逻辑，可以看[EditTextManager.EditTextHandle]的实现。
 */
fun InputView.Companion.addEditText(window: Window, editText: EditText): Boolean {
    return window.decorView.requireViewTreeWindow().addEditText(editText)
}

/**
 * 移除[addEditText]添加的[editText]，再次移除[editText]返回`false`，
 * 调用该函数之前，需要先调用[init]或[initCompat]完成初始化。
 */
fun InputView.Companion.removeEditText(window: Window, editText: EditText): Boolean {
    return window.decorView.requireViewTreeWindow().removeEditText(editText)
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
private fun Window.checkDispatchApplyInsetsCompatibility() = ReflectCompat {
    check(!isFloating) {
        "InputView需要主题的windowIsFloating = false，" +
                "否则会导致视图树没有WindowInsets分发"
    }
    @Suppress("DEPRECATION")
    check(isFullscreenCompatEnabled || (attributes.flags and FLAG_FULLSCREEN == 0)) {
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

internal fun View.requireViewTreeWindow() = requireNotNull(findViewTreeWindow()) {
    "需要先调用InputView.init()或InputView.initCompat()完成初始化"
}

internal class ViewTreeWindow(
    private val window: Window,
    val gestureNavBarEdgeToEdge: Boolean
) : EdgeToEdgeHelper {
    private val decorView = window.decorView as ViewGroup
    private val manager = EditTextManager(this, window.callback)
    val currentFocus: View?
        get() = window.currentFocus

    fun attach() = apply {
        check(decorView.viewTreeWindow == null) { "InputView.init()只能调用一次" }
        @Suppress("DEPRECATION")
        window.setSoftInputMode(SOFT_INPUT_ADJUST_RESIZE)
        window.setDecorFitsSystemWindowsCompat(false)
        window.checkDispatchApplyInsetsCompatibility()
        window.callback = manager
        decorView.viewTreeWindow = this
    }

    fun setOnApplyWindowInsetsListener(statusBarEdgeToEdge: Boolean) {
        val contentRootRef = decorView.children
            .firstOrNull { it is ViewGroup }?.let(::WeakReference)
        decorView.setOnApplyWindowInsetsListenerCompat { _, insets ->
            window.checkDispatchApplyInsetsCompatibility()
            val decorInsets = insets.toDecorInsets(statusBarEdgeToEdge)
            decorView.onApplyWindowInsetsCompat(decorInsets)
            // 在DecorView处理完ContentRoot的Margins后，再设置Margins
            contentRootRef?.get()?.updateMargins(
                top = decorInsets.statusBarHeight,
                bottom = decorInsets.navigationBarHeight
            )
            insets
        }
    }

    /**
     * 不调用[Window.setStatusBarColor]和[Window.setNavigationBarColor]将背景颜色设为透明，
     * 通过消费状态栏和导航栏的`Insets`实现不绘制背景，这种处理方式的通用性更强，侵入性更低。
     */
    private fun WindowInsetsCompat.toDecorInsets(statusBarEdgeToEdge: Boolean): WindowInsetsCompat {
        var typeMask = 0
        if (statusBarEdgeToEdge) {
            typeMask = typeMask or statusBars()
        }
        if (supportGestureNavBarEdgeToEdge(decorView)) {
            typeMask = typeMask or navigationBars()
        }
        return consume(typeMask)
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

    fun getRootWindowInsets() = decorView.getRootWindowInsetsCompat()

    fun showIme(editText: EditText) {
        // controllerCompat对象很轻量，showIme不会产生内部状态
        window.createWindowInsetsControllerCompat(editText).show(ime())
    }

    fun hideIme() {
        // controllerCompat对象很轻量，hideIme不会产生内部状态
        window.createWindowInsetsControllerCompat(decorView).hide(ime())
    }

    fun modifyImeAnimation(durationMillis: Long, interpolator: Interpolator) {
        ReflectCompat { window.modifyImeAnimation(durationMillis, interpolator) }
    }

    fun restoreImeAnimation() {
        ReflectCompat { window.restoreImeAnimation() }
    }

    fun register(host: EditorHost) = manager.register(host)

    fun unregister(host: EditorHost) = manager.unregister(host)

    fun addEditText(editText: EditText) = manager.addEditText(editText)

    fun removeEditText(editText: EditText) = manager.removeEditText(editText)
}