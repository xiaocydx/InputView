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

import androidx.fragment.app.FragmentManager

/**
 * [FragmentManager]低版本兼容类
 *
 * ```
 * public abstract class FragmentManager {
 *
 *      void moveFragmentToExpectedState(@NonNull Fragment f) {
 *             ...
 *             final ViewGroup container = f.mContainer;
 *             int underIndex = container.indexOfChild(underView);
 *             int viewIndex = container.indexOfChild(f.mView);
 *             if (viewIndex < underIndex) {
 *                 container.removeViewAt(viewIndex); // 抛出异常不崩溃
 *                 container.addView(f.mView, underIndex); // 拦截addView
 *             }
 *             ...
 *      }
 * }
 * ```
 *
 * @author xcc
 * @date 2023/12/22
 */
internal class FragmentManagerCompat {
    var interceptAddView = false

    inline fun removeViewAt(action: () -> Unit) {
        try {
            action()
        } catch (e: NullPointerException) {
            val moveFragmentToExpectedState = e.stackTrace.firstOrNull {
                it.className == "androidx.fragment.app.FragmentManager"
                        && it.methodName == "moveFragmentToExpectedState"
            }
            interceptAddView = moveFragmentToExpectedState != null
            if (!interceptAddView) throw e
        }
    }

    inline fun addView(action: () -> Unit) {
        if (!interceptAddView) action()
        interceptAddView = false
    }
}