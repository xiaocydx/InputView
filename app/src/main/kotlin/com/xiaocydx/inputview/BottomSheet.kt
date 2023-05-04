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

@file:JvmName("BottomSheetInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package com.google.android.material.bottomsheet

import android.view.View
import android.view.WindowInsets
import androidx.core.view.WindowCompat
import androidx.core.view.doOnAttach

/**
 * 禁用[BottomSheetDialog]的`EdgeToEdge`和`fitsSystemWindows`，自行处理[WindowInsets]和实现`EdgeToEdge`
 *
 * 禁用后的效果：
 * 1. 去除`container`和`coordinator`消费`systemWindowInsets`的逻辑，避免停止分发[WindowInsets]。
 * 2. 去除`bottomSheet`状态栏`EdgeToEdge`的逻辑，避免重复实现状态栏`EdgeToEdge`。
 * 3. 去除`bottomSheet`适应`systemWindowInsets`的逻辑，避免重复设置`paddings`。
 *
 * **注意**：该函数的实现逻辑为示例代码，主要是演示禁用的实现思路，实际场景可以做进一步优化。
 */
fun BottomSheetDialog.disableEdgeToEdgeAndFitsSystemWindows() = doOnAttachSafely {
    val clazz = BottomSheetDialog::class.java
    val edgeToEdgeEnabledField = clazz
        .getDeclaredField("edgeToEdgeEnabled").apply { isAccessible = true }
    val containerField = clazz.getDeclaredField("container").apply { isAccessible = true }
    val coordinatorField = clazz.getDeclaredField("coordinator").apply { isAccessible = true }
    val bottomSheetField = clazz.getDeclaredField("bottomSheet").apply { isAccessible = true }

    // BottomSheetDialog.onAttachedToWindow()调用自DecorView.onAttachedToWindow(),
    // DecorView.doOnAttach()在DecorView.onAttachedToWindow()之后执行。
    // BottomSheetDialog.onAttachedToWindow()会修改container和coordinator的fitsSystemWindows，
    // 以及修改decorFitsSystemWindows，需要在该函数之后，将这些属性重新设置为false。
    edgeToEdgeEnabledField.set(this, false)
    containerField.get(this)?.let { it as? View }?.fitsSystemWindows = false
    coordinatorField.get(this)?.let { it as? View }?.fitsSystemWindows = false
    WindowCompat.setDecorFitsSystemWindows(window!!, false)

    val bottomSheet = bottomSheetField.get(this) as? View ?: return@doOnAttachSafely
    val listener = View.OnApplyWindowInsetsListener { _, insets -> insets }
    // 初始化阶段调用BottomSheetDialog.wrapInBottomSheet()，edgeToEdgeEnabled为true，
    // 对bottomSheet设置OnApplyWindowInsetsListener，当前处于DecorView.doOnAttach()，
    // 首帧还未进行WindowInsets分发，因此对bottomSheet设置listener替换已有的实现逻辑。
    bottomSheet.setOnApplyWindowInsetsListener(listener)

    // BottomSheetBehavior.onLayoutChild()会对bottomSheet设置OnApplyWindowInsetsListener，
    // 因此在bottomSheet.onLayout()布局完成后，对bottomSheet设置listener替换已有的实现逻辑。
    bottomSheet.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        bottomSheet.setOnApplyWindowInsetsListener(listener)
    }
}

private inline fun BottomSheetDialog.doOnAttachSafely(crossinline action: (view: View) -> Unit) {
    window?.decorView?.doOnAttach { kotlin.runCatching { action(it) } }
}

abstract class SimpleBottomSheetCallback : BottomSheetBehavior.BottomSheetCallback() {
    override fun onStateChanged(bottomSheet: View, newState: Int) = Unit
    override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
    public override fun onLayout(bottomSheet: View) = Unit
}