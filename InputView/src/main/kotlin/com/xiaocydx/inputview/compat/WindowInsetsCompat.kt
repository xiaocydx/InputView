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

package com.xiaocydx.inputview.compat

import android.view.View
import android.view.Window
import android.view.WindowInsets
import androidx.core.view.*
import androidx.core.view.WindowInsetsCompat.Type.InsetsType

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