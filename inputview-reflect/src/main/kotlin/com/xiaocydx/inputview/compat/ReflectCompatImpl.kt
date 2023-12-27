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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")

package com.xiaocydx.inputview.compat

import android.view.View
import android.view.Window
import android.view.animation.Interpolator
import androidx.core.view.WindowInsetsAnimationCompat
import com.xiaocydx.inputview.OnApplyWindowInsetsListenerCompat
import com.xiaocydx.insets.isFullscreenCompatEnabled as isFullscreenCompatEnabledImpl
import com.xiaocydx.insets.modifyImeAnimation as modifyImeAnimationImpl
import com.xiaocydx.insets.restoreImeAnimation as restoreImeAnimationImpl
import com.xiaocydx.insets.setOnApplyWindowInsetsListenerImmutable as setOnApplyWindowInsetsListenerImmutableImpl
import com.xiaocydx.insets.setWindowInsetsAnimationCallbackImmutable as setWindowInsetsAnimationCallbackImmutableImpl

/**
 * 反射兼容的实现类
 *
 * @author xcc
 * @date 2023/11/29
 */
internal class ReflectCompatImpl : ReflectCompat {
    override val Window.isFullscreenCompatEnabled: Boolean
        get() = isFullscreenCompatEnabledImpl

    override fun Window.modifyImeAnimation(durationMillis: Long, interpolator: Interpolator) {
        modifyImeAnimationImpl(durationMillis, interpolator)
    }

    override fun Window.restoreImeAnimation() {
        restoreImeAnimationImpl()
    }

    override fun View.setOnApplyWindowInsetsListenerImmutable(listener: OnApplyWindowInsetsListenerCompat?) {
        setOnApplyWindowInsetsListenerImmutableImpl(listener)
    }

    override fun View.setWindowInsetsAnimationCallbackImmutable(callback: WindowInsetsAnimationCompat.Callback?) {
        setWindowInsetsAnimationCallbackImmutableImpl(callback)
    }
}