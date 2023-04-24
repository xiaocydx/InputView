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
import android.view.View
import android.view.Window
import android.view.WindowInsets
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.core.view.*
import com.xiaocydx.inputview.R

@Suppress("DEPRECATION")
internal fun WindowInsets.toImmutable(): WindowInsets {
    if (!isImmutableCompatNeeded) return this
    // WindowInsetsCompat不兼容WindowInsets.mWindowDecorInsets，因此不需要修改
    WindowInsetsReflectCache.mStableInsetsField?.set(this, Rect(
        stableInsetLeft, stableInsetTop,
        stableInsetRight, stableInsetBottom
    ))
    return replaceSystemWindowInsets(
        systemWindowInsetLeft, systemWindowInsetTop,
        systemWindowInsetRight, systemWindowInsetBottom
    )
}

@Suppress("DEPRECATION")
internal fun WindowInsetsCompat.toImmutable(): WindowInsetsCompat {
    if (!isImmutableCompatNeeded) return this
    return apply { stableInsets }.apply { systemWindowInsets }
}

internal fun View.setOnApplyWindowInsetsListenerCompat(
    listener: OnApplyWindowInsetsListener? = defaultOnApplyWindowInsetsListener
): Unit = with(WindowInsetsReflectCache) {
    val view = this@setOnApplyWindowInsetsListenerCompat
    ViewCompat.setOnApplyWindowInsetsListener(view, listener)
    if (!isImmutableCompatNeeded) return
    immutableListenerCompat = if (listener == null) null else {
        val mListenerInfo = mListenerInfoField?.get(view) ?: return
        val delegate = mOnApplyWindowInsetsListenerField?.get(mListenerInfo)
        if (delegate !is View.OnApplyWindowInsetsListener) return
        val compat = WindowInsetsImmutableListenerCompat(delegate)
        mOnApplyWindowInsetsListenerField?.set(mListenerInfo, compat)
        compat
    }
}

internal fun View.setWindowInsetsAnimationCallbackCompat(callback: WindowInsetsAnimationCompat.Callback?) {
    ViewCompat.setWindowInsetsAnimationCallback(this, callback)
    if (!isImmutableCompatNeeded) return
    if (isAttachedToWindow) {
        // 确保Impl21OnApplyWindowInsetsListener构造函数创建的mLastInsets不可变
        val proxyListener = getTag(R.id.tag_window_insets_animation_callback)
        if (proxyListener !is View.OnApplyWindowInsetsListener) return
        WindowInsetsReflectCache.mLastInsetsField?.get(proxyListener)
            ?.let { it as? WindowInsetsCompat }?.toImmutable()
    }
    if (immutableListenerCompat == null) {
        // 确保Impl21OnApplyWindowInsetsListener分发到不可变的WindowInsets
        setOnApplyWindowInsetsListenerCompat()
    }
}

internal fun Window.setDecorFitsSystemWindowsCompat(decorFitsSystemWindows: Boolean) {
    WindowCompat.setDecorFitsSystemWindows(this, decorFitsSystemWindows)
}

internal fun View.getWindowInsetsCompat() = ViewCompat.getRootWindowInsets(this)

internal fun View.requestApplyInsetsCompat() = ViewCompat.requestApplyInsets(this)

internal fun View.dispatchApplyWindowInsetsCompat(insets: WindowInsetsCompat) =
        ViewCompat.dispatchApplyWindowInsets(this, insets)

internal fun View.onApplyWindowInsetsCompat(insets: WindowInsetsCompat) =
        ViewCompat.onApplyWindowInsets(this, insets)

/**
 * Android 9.0及以上，[WindowInsets]完全不可变，不需要兼容
 */
@ChecksSdkIntAtLeast(api = 21)
private val isImmutableCompatNeeded = Build.VERSION.SDK_INT in 21 until 28

@RequiresApi(21)
private val defaultOnApplyWindowInsetsListener =
        OnApplyWindowInsetsListener { v, insets -> ViewCompat.onApplyWindowInsets(v, insets) }

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
        return if (view.hasParentImmutableListenerCompat()) {
            // 父级已经确保insets不可变，不需要再转换
            delegate.onApplyWindowInsets(view, insets)
        } else {
            delegate.onApplyWindowInsets(view, insets.toImmutable())
        }
    }

    private fun View.hasParentImmutableListenerCompat(): Boolean {
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
private object WindowInsetsReflectCache : ReflectHelper {
    var mListenerInfoField: FieldCache? = null; private set
    var mOnApplyWindowInsetsListenerField: FieldCache? = null; private set
    var mStableInsetsField: FieldCache? = null; private set
    var mLastInsetsField: FieldCache? = null; private set

    init {
        runCatching {
            val viewClass = View::class.java
            val listenerInfoClass = Class.forName("android.view.View\$ListenerInfo")
            val windowInsetsClass = WindowInsets::class.java
            val proxyListenerClass = Class.forName("androidx.core.view" +
                    ".WindowInsetsAnimationCompat\$Impl21\$Impl21OnApplyWindowInsetsListener")
            mListenerInfoField = viewClass.declaredInstanceFields.find("mListenerInfo").toCache()
            mOnApplyWindowInsetsListenerField = listenerInfoClass.declaredInstanceFields
                .find("mOnApplyWindowInsetsListener").toCache()
            mStableInsetsField = windowInsetsClass.declaredInstanceFields
                .find("mStableInsets").toCache()
            mLastInsetsField = proxyListenerClass.declaredInstanceFields
                .find("mLastInsets").toCache()
        }.onFailure {
            mListenerInfoField = null
            mOnApplyWindowInsetsListenerField = null
            mStableInsetsField = null
        }
    }
}