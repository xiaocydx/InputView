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
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
import android.view.animation.Interpolator
import android.widget.EditText
import androidx.annotation.VisibleForTesting
import androidx.core.view.*
import androidx.core.view.WindowInsetsCompat.Type.*
import com.xiaocydx.inputview.compat.*
import com.xiaocydx.insets.consumeInsets
import com.xiaocydx.insets.getImeOffset
import com.xiaocydx.insets.getRootWindowInsetsCompat
import com.xiaocydx.insets.imeHeight
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.isGestureNavigationBar
import com.xiaocydx.insets.navigationBarHeight
import com.xiaocydx.insets.onApplyWindowInsetsCompat
import com.xiaocydx.insets.setDecorFitsSystemWindowsCompat
import com.xiaocydx.insets.setOnApplyWindowInsetsListenerCompat
import com.xiaocydx.insets.statusBarHeight
import com.xiaocydx.insets.updateMargins
import java.lang.ref.WeakReference

/**
 * 初始化[InputView]所需的配置
 *
 * @param window [Activity.getWindow]或[Dialog.getWindow]。
 * @param statusBarEdgeToEdge 是否启用状态栏EdgeToEdge。
 * 若启用，则不消费[WindowInsets]的状态栏Insets，不设置状态栏高度的间距，不绘制背景色。
 * @param gestureNavBarEdgeToEdge 是否启用手势导航栏EdgeToEdge。
 * 若启用，则不消费[WindowInsets]的手势导航栏Insets，不设置手势导航栏高度的间距，不绘制背景色，
 * [InputView]增加`navBarOffset`区域，[AnimationState.navBarOffset]有值。
 *
 * 可以利用[View.insets]、[WindowInsetsCompat.isGestureNavigationBar]等扩展实现EdgeToEdge。
 *
 * @return 首次初始化返回`true`，再次初始化返回`false`。
 */
fun InputView.Companion.init(
    window: Window,
    statusBarEdgeToEdge: Boolean = false,
    gestureNavBarEdgeToEdge: Boolean = false
): Boolean {
    if (ViewTreeWindow.isAttached(window)) return false
    ViewTreeWindow(window, gestureNavBarEdgeToEdge).attach()
        .setOnApplyWindowInsetsListener(statusBarEdgeToEdge)
    return true
}

/**
 * 初始化[InputView]所需的配置，该函数用于兼容已有的[WindowInsets]处理方案
 *
 * @param window [Activity.getWindow]或[Dialog.getWindow]。
 * @param gestureNavBarEdgeToEdge 是否启用手势导航栏EdgeToEdge。
 * 若启用，[InputView]增加`navBarOffset`区域，[AnimationState.navBarOffset]有值。
 *
 * 可以利用[View.insets]、[WindowInsetsCompat.isGestureNavigationBar]等扩展实现EdgeToEdge。
 *
 * @return 首次初始化返回`true`，再次初始化返回`false`。
 */
fun InputView.Companion.initCompat(
    window: Window,
    gestureNavBarEdgeToEdge: Boolean = false,
): Boolean {
    if (ViewTreeWindow.isAttached(window)) return false
    ViewTreeWindow(window, gestureNavBarEdgeToEdge).attach()
    return true
}

/**
 * 添加需要处理水滴状指示器的[editText]，再次添加[editText]返回`false`，
 * 内部实现通过弱引用的方式持有[editText]，因此不必担心会有内存泄漏问题，
 * 调用该函数之前，需要先调用[init]或[initCompat]完成初始化。
 *
 * 跟[InputView]和[ImeAnimator]关联的`editText`，会自动调用该函数，
 * 水滴状指示器的处理逻辑，可以看[EditTextManager.EditTextHandle]。
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

private var View.viewTreeWindow: ViewTreeWindow?
    get() = getTag(R.id.tag_view_tree_window) as? ViewTreeWindow
    set(value) {
        setTag(R.id.tag_view_tree_window, value)
    }

internal fun View.findViewTreeWindow() = when {
    !isAttachedToWindow -> viewTreeWindow
    else -> rootView?.viewTreeWindow
}

internal fun View.requireViewTreeWindow() = requireNotNull(findViewTreeWindow()) {
    when {
        !isAttachedToWindow -> "${javaClass.simpleName}当前未附加到Window"
        else -> "需要先调用InputView.init()或InputView.initCompat()完成初始化"
    }
}

internal class ViewTreeWindow(
    private val window: Window,
    private val gestureNavBarEdgeToEdge: Boolean
) {
    private val decorView = window.decorView as ViewGroup
    private val editTextManager = EditTextManager(this, window.callback)
    val currentFocus: View?
        get() = window.currentFocus
    val hasWindowFocus: Boolean
        get() = window.decorView.hasWindowFocus()

    fun attach() = apply {
        check(!isAttached(window)) { "只能调用一次attach()" }
        @Suppress("DEPRECATION")
        window.setSoftInputMode(SOFT_INPUT_ADJUST_RESIZE)
        window.setDecorFitsSystemWindowsCompat(false)
        window.checkDispatchApplyInsetsCompatibility()
        window.callback = editTextManager
        editTextManager.setFocusableCompat(decorView)
        decorView.viewTreeWindow = this
    }

    fun setOnApplyWindowInsetsListener(statusBarEdgeToEdge: Boolean) {
        val contentRootRef = decorView.children
            .firstOrNull { it is ViewGroup }?.let(::WeakReference)
        decorView.setOnApplyWindowInsetsListenerCompat { _, insets ->
            window.checkDispatchApplyInsetsCompatibility()
            // 不调用window.setStatusBarColor和window.setNavigationBarColor将背景颜色设为透明，
            // 通过消费状态栏和导航栏的Insets实现不绘制背景，这种处理方式的通用性更强，侵入性更低。
            var consumeType = 0
            if (statusBarEdgeToEdge) consumeType = consumeType or statusBars()
            if (insets.supportGestureNavBarEdgeToEdge) consumeType = consumeType or navigationBars()
            val decorInsets = insets.consumeInsets(consumeType or ime())
            decorView.onApplyWindowInsetsCompat(decorInsets)
            // 在decorView处理完contentRoot的Margins后，再设置Margins
            contentRootRef?.get()?.updateMargins(
                top = decorInsets.statusBarHeight,
                bottom = decorInsets.navigationBarHeight
            )
            consumeType = consumeType.inv() and (statusBars() or navigationBars())
            insets.consumeInsets(consumeType)
        }
    }

    private val WindowInsetsCompat.supportGestureNavBarEdgeToEdge: Boolean
        get() {
            if (!gestureNavBarEdgeToEdge) return false
            var isGestureNavigationBar = isGestureNavigationBar(decorView)
            if (isGestureNavigationBar) {
                val rootInsets = decorView.getRootWindowInsetsCompat()
                if (rootInsets == null || rootInsets.navigationBarHeight > navigationBarHeight) {
                    // 父级消费了导航栏Insets，将这种情况视为不支持手势导航栏EdgeToEdge
                    isGestureNavigationBar = false
                }
            }
            return isGestureNavigationBar
        }

    val WindowInsetsCompat.navBarOffset: Int
        get() = if (supportGestureNavBarEdgeToEdge) navigationBarHeight else 0

    val WindowInsetsCompat.imeOffset: Int
        get() = if (supportGestureNavBarEdgeToEdge) imeHeight else getImeOffset(decorView)

    fun getRootWindowInsets() = decorView.getRootWindowInsetsCompat()

    fun showIme(view: View) {
        // controllerCompat对象很轻量，showIme不会产生内部状态
        WindowInsetsControllerCompat(window, view).show(ime())
    }

    fun hideIme() {
        // controllerCompat对象很轻量，hideIme不会产生内部状态
        WindowInsetsControllerCompat(window, decorView).hide(ime())
    }

    fun modifyImeAnimation(durationMillis: Long, interpolator: Interpolator) {
        ReflectCompat { window.modifyImeAnimation(durationMillis, interpolator) }
    }

    fun restoreImeAnimation() {
        ReflectCompat { window.restoreImeAnimation() }
    }

    fun registerHost(host: EditorHost) = editTextManager.registerHost(host)

    fun unregisterHost(host: EditorHost) = editTextManager.unregisterHost(host)

    fun addEditText(editText: EditText) = editTextManager.addEditText(editText)

    fun removeEditText(editText: EditText) = editTextManager.removeEditText(editText)

    fun getFocusableView() = editTextManager.getFocusableView(decorView)

    @VisibleForTesting
    fun getEditTextManager() = editTextManager

    companion object {
        fun isAttached(window: Window): Boolean {
            return window.decorView.viewTreeWindow != null
        }
    }
}