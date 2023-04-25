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

@file:SuppressLint("ObsoleteSdkInt")

package com.xiaocydx.inputview.compat

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.util.SparseArray
import android.view.View
import android.view.Window
import android.view.WindowInsets
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.core.util.forEach
import androidx.core.view.*
import androidx.core.view.WindowInsetsCompat.Type.InsetsType
import com.xiaocydx.inputview.R

internal fun View.setOnApplyWindowInsetsListenerImmutableCompat(
    listener: OnApplyWindowInsetsListener? = defaultOnApplyWindowInsetsListener
): Unit = with(InsetsCompatReflection) {
    setOnApplyWindowInsetsListenerCompat(listener)
    if (!isImmutableCompatNeeded || !reflectSucceed) return
    // 确保View.OnApplyWindowInsetsListener分发的WindowInsets不可变
    immutableListenerCompat = listener?.run { setupImmutableListenerCompat() }
}

internal fun View.setWindowInsetsAnimationCallbackImmutableCompat(
    callback: WindowInsetsAnimationCompat.Callback?
): Unit = with(InsetsCompatReflection) {
    setWindowInsetsAnimationCallbackCompat(callback)
    if (!isImmutableCompatNeeded || !reflectSucceed) return
    if (isAttachedToWindow) {
        // 确保Impl21OnApplyWindowInsetsListener构造函数创建的mLastInsets生成缓存insets
        getLastInsetsFromProxyListener()?.ensureCreateCacheInsets()
    }
    if (immutableListenerCompat == null) {
        // 确保Impl21OnApplyWindowInsetsListener分发的WindowInsets不可变
        setOnApplyWindowInsetsListenerImmutableCompat()
    }
}

internal fun View.setOnApplyWindowInsetsListenerCompat(listener: OnApplyWindowInsetsListener?) {
    ViewCompat.setOnApplyWindowInsetsListener(this, listener)
}

internal fun View.setWindowInsetsAnimationCallbackCompat(callback: WindowInsetsAnimationCompat.Callback?) {
    ViewCompat.setWindowInsetsAnimationCallback(this, callback)
}

/**
 * 传入[view]是为了确保转换出的[WindowInsetsCompat]是正确的结果
 */
internal fun WindowInsets.toCompat(view: View) =
        WindowInsetsCompat.toWindowInsetsCompat(this, view)

@Suppress("DEPRECATION")
internal fun WindowInsets.toImmutable(): WindowInsets = InsetsCompatReflection.run {
    if (!isImmutableCompatNeeded || !reflectSucceed) return this@toImmutable
    val insets = replaceSystemWindowInsets(
        systemWindowInsetLeft, systemWindowInsetTop,
        systemWindowInsetRight, systemWindowInsetBottom
    )
    // WindowInsetsCompat不兼容WindowInsets.mWindowDecorInsets，因此不需要修改
    insets.setStableInsets(stableInsetLeft, stableInsetTop, stableInsetRight, stableInsetBottom)
    return insets
}

@Suppress("DEPRECATION")
internal fun WindowInsetsCompat.ensureCreateCacheInsets() {
    if (!isImmutableCompatNeeded) return
    apply { stableInsets }.apply { systemWindowInsets }
}

internal fun Window.setDecorFitsSystemWindowsCompat(decorFitsSystemWindows: Boolean) {
    WindowCompat.setDecorFitsSystemWindows(this, decorFitsSystemWindows)
}

internal fun Window.createWindowInsetsControllerCompat(editText: View) =
        WindowInsetsControllerCompat(this, editText)

internal fun View.getRootWindowInsetsCompat() = ViewCompat.getRootWindowInsets(this)

internal fun View.requestApplyInsetsCompat() = ViewCompat.requestApplyInsets(this)

internal fun View.dispatchApplyWindowInsetsCompat(insets: WindowInsetsCompat) =
        ViewCompat.dispatchApplyWindowInsets(this, insets)

internal fun View.onApplyWindowInsetsCompat(insets: WindowInsetsCompat) =
        ViewCompat.onApplyWindowInsets(this, insets)

internal fun WindowInsetsAnimationCompat.contains(@InsetsType typeMask: Int) =
        this.typeMask and typeMask == typeMask

/**
 * Android 9.0及以上，[WindowInsets]不可变，不需要兼容
 */
@ChecksSdkIntAtLeast(api = 21)
private val isImmutableCompatNeeded = Build.VERSION.SDK_INT in 21 until 28

@RequiresApi(21)
private val defaultOnApplyWindowInsetsListener =
        OnApplyWindowInsetsListener { v, insets -> v.onApplyWindowInsetsCompat(insets) }

@get:RequiresApi(21)
private var View.immutableListenerCompat: WindowInsetsImmutableListenerCompat?
    get() = getTag(R.id.tag_window_insets_immutable_listener_compat) as? WindowInsetsImmutableListenerCompat
    set(value) {
        setTag(R.id.tag_window_insets_immutable_listener_compat, value)
    }

@RequiresApi(21)
private class WindowInsetsImmutableListenerCompat(
    private val delegate: View.OnApplyWindowInsetsListener
) : View.OnApplyWindowInsetsListener {

    override fun onApplyWindowInsets(view: View, insets: WindowInsets): WindowInsets {
        val applyInsets = if (view.hasParentCompat()) insets else insets.toImmutable()
        return delegate.onApplyWindowInsets(view, applyInsets)
    }

    private fun View.hasParentCompat(): Boolean {
        var parent: View? = parent as? View
        var found: WindowInsetsImmutableListenerCompat? = parent?.immutableListenerCompat
        while (found == null && parent is View) {
            found = parent.immutableListenerCompat
            parent = parent.parent as? View
        }
        return found != null
    }
}

@RequiresApi(21)
@SuppressLint("PrivateApi")
private object InsetsCompatReflection : ReflectHelper {
    private var mKeyedTagsField: FieldCache? = null
    private var mListenerInfoField: FieldCache? = null
    private var mOnApplyWindowInsetsListenerField: FieldCache? = null
    private var mStableInsetsField: FieldCache? = null
    private var mLastInsetsField: FieldCache? = null
    private var proxyListenerClass: Class<*>? = null
    var reflectSucceed: Boolean = false; private set

    init {
        runCatching {
            val viewClass = View::class.java
            val listenerInfoClass = Class.forName("android.view.View\$ListenerInfo")
            val windowInsetsClass = WindowInsets::class.java
            val proxyListenerClass = Class.forName("androidx.core.view." +
                    "WindowInsetsAnimationCompat\$Impl21\$Impl21OnApplyWindowInsetsListener")
            val viewFields = viewClass.declaredInstanceFields
            mKeyedTagsField = viewFields.find("mKeyedTags").toCache()
            mListenerInfoField = viewFields.find("mListenerInfo").toCache()
            mOnApplyWindowInsetsListenerField = listenerInfoClass
                .declaredInstanceFields.find("mOnApplyWindowInsetsListener").toCache()
            mStableInsetsField = windowInsetsClass
                .declaredInstanceFields.find("mStableInsets").toCache()
            mLastInsetsField = proxyListenerClass
                .declaredInstanceFields.find("mLastInsets").toCache()
            this.proxyListenerClass = proxyListenerClass
            reflectSucceed = true
        }.onFailure {
            mKeyedTagsField = null
            mListenerInfoField = null
            mOnApplyWindowInsetsListenerField = null
            mStableInsetsField = null
            proxyListenerClass = null
        }
    }

    /**
     * ```
     * public class View implements Drawable.Callback, KeyEvent.Callback,
     *         AccessibilityEventSource {
     *
     *     static class ListenerInfo {
     *         OnApplyWindowInsetsListener mOnApplyWindowInsetsListener;
     *     }
     * }
     * ```
     */
    fun View.setupImmutableListenerCompat(): WindowInsetsImmutableListenerCompat? {
        val mListenerInfo = mListenerInfoField?.get(this) ?: return null
        val delegate = mOnApplyWindowInsetsListenerField?.get(mListenerInfo)
                as? View.OnApplyWindowInsetsListener ?: return null
        val compat = WindowInsetsImmutableListenerCompat(delegate)
        mOnApplyWindowInsetsListenerField?.set(mListenerInfo, compat)
        return compat
    }

    /**
     * compileOnly依赖AndroidX core，访问`androidx.core.R.id.tag_window_insets_animation_callback`，
     * 源码会爆红，但编译能通过，改为implementation依赖AndroidX core，虽然能解决爆红问题，但是不符合初衷，
     * 在values目录下定义跟AndroidX core同名的id，不确定会不会造成其他问题，因此反射获取`proxyListener`，
     * 若后续确定同名id不会造成其他问题，或者有其他办法解决爆红问题，则去除反射实现。
     *
     * ```
     * private static class Impl21OnApplyWindowInsetsListener implements
     *         View.OnApplyWindowInsetsListener {
     *     private WindowInsetsCompat mLastInsets;
     * }
     * ```
     */
    fun View.getLastInsetsFromProxyListener(): WindowInsetsCompat? {
        val mKeyedTags = mKeyedTagsField?.get(this) as? SparseArray<*> ?: return null
        mKeyedTags.forEach action@{ _, value ->
            if (!value.javaClass.isAssignableFrom(proxyListenerClass!!)) return@action
            mLastInsetsField?.get(value)?.let { it as? WindowInsetsCompat }?.let { return it }
        }
        return null
    }

    /**
     * [WindowInsets]没有提供`replaceStableInsets()`之类的函数，因此反射修改`mStableInsets`
     *
     * ```
     * public final class WindowInsets {
     *     private Rect mStableInsets;
     * }
     * ```
     */
    fun WindowInsets.setStableInsets(
        stableInsetLeft: Int, stableInsetTop: Int,
        stableInsetRight: Int, stableInsetBottom: Int
    ) {
        mStableInsetsField?.set(this, Rect(
            stableInsetLeft, stableInsetTop,
            stableInsetRight, stableInsetBottom
        ))
    }
}