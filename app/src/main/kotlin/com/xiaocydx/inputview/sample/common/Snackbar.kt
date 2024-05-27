/*
 * Copyright 2022 xiaocydx
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

package com.xiaocydx.inputview.sample.common

import android.view.View
import android.view.Window
import android.widget.FrameLayout
import androidx.annotation.CheckResult
import com.google.android.material.snackbar.Snackbar

@CheckResult
@Suppress("SpellCheckingInspection")
fun View.snackbar() = Snackbar.make(
    this, "", Snackbar.LENGTH_SHORT
).setTextMaxLines(Int.MAX_VALUE).dismissAction()

@CheckResult
@Suppress("SpellCheckingInspection")
fun Window.snackbar() = decorView.findViewById<View>(android.R.id.content).snackbar()

@CheckResult
fun Snackbar.short() = setDuration(Snackbar.LENGTH_SHORT)

@CheckResult
fun Snackbar.long() = setDuration(Snackbar.LENGTH_LONG)

@CheckResult
fun Snackbar.indefinite() = setDuration(Snackbar.LENGTH_INDEFINITE)

@CheckResult
fun Snackbar.dismissAction(text: CharSequence = "dismiss") = setAction(text) { dismiss() }

@CheckResult
fun Snackbar.gravity(gravity: Int) = apply {
    (view.layoutParams as? FrameLayout.LayoutParams)?.gravity = gravity
}