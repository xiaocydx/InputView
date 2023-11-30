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

@file:Suppress("PackageDirectoryMismatch")

package com.xiaocydx.inputview.compat

import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.animation.Interpolator
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.InsetsType
import androidx.core.view.WindowInsetsControllerCompat
import com.xiaocydx.inputview.OnApplyWindowInsetsListenerCompat

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

internal fun WindowInsetsAnimationCompat.contains(@InsetsType typeMask: Int) =
        this.typeMask and typeMask == typeMask

internal val reflectCompat: ReflectCompat = try {
    val className = "com.xiaocydx.inputview.compat.ReflectCompatImpl"
    val clazz = Class.forName(className, false, ReflectCompat::class.java.classLoader)
    clazz.asSubclass(ReflectCompat::class.java).newInstance()
} catch (e: Throwable) {
    NotReflectCompat
}

@Suppress("FunctionName")
internal inline fun <R> ReflectCompat(block: ReflectCompat.() -> R): R = with(reflectCompat, block)

internal interface ReflectCompat {
    val Window.isFullscreenCompatEnabled: Boolean

    fun Window.modifyImeAnimation(durationMillis: Long, interpolator: Interpolator)

    fun Window.restoreImeAnimation()

    fun View.setOnApplyWindowInsetsListenerImmutable(listener: OnApplyWindowInsetsListenerCompat?)

    fun View.setWindowInsetsAnimationCallbackImmutable(callback: WindowInsetsAnimationCompat.Callback?)
}

internal object NotReflectCompat : ReflectCompat {
    override val Window.isFullscreenCompatEnabled: Boolean
        get() = false

    override fun Window.modifyImeAnimation(durationMillis: Long, interpolator: Interpolator) = Unit

    override fun Window.restoreImeAnimation() = Unit

    override fun View.setOnApplyWindowInsetsListenerImmutable(listener: OnApplyWindowInsetsListenerCompat?) {
        setOnApplyWindowInsetsListenerCompat(listener)
    }

    override fun View.setWindowInsetsAnimationCallbackImmutable(callback: WindowInsetsAnimationCompat.Callback?) {
        setWindowInsetsAnimationCallbackCompat(callback)
    }
}