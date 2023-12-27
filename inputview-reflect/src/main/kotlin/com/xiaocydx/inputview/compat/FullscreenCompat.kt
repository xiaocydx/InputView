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

import android.view.Window
import android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
import androidx.core.view.ViewCompat
import com.xiaocydx.inputview.EditorAnimator
import com.xiaocydx.insets.enableDispatchApplyInsetsFullscreenCompat as enableDispatchApplyInsetsFullscreenCompatImpl

/**
 * 启用Android 11以下`window.attributes.flags`包含[FLAG_FULLSCREEN]的兼容方案
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
 *
 * 兼容方案：
 * 1. 在`ViewRootImpl.ViewRootHandler`处理完`MSG_RESIZED`或`MSG_RESIZED_REPORT`后，
 * 根据情况申请WindowInsets分发，确保下一帧`ViewRooImpl.performTraversals()`调用
 * `ViewRootImpl.dispatchApplyInsets()`。
 *
 * 2. 替换`ViewRootImpl.mScroller`，禁止滚动到焦点可见的位置，让视图树自行处理WindowInsets。
 *
 * **注意**：Android 9.0和Android 10，若`window.attributes.flags`包含[FLAG_FULLSCREEN]，
 * 则[ViewCompat.setWindowInsetsAnimationCallback]设置的回调对象，其函数可能不会被调用，
 * 这个问题跟`WindowInsetsAnimationCompat.Impl21`的兼容代码有关，该函数不负责兼容这种情况，
 * 若实际场景需要设置回调对象以实现其他需求，则可以参考[EditorAnimator]的兼容方案。
 */
fun Window.enableDispatchApplyInsetsFullscreenCompat() {
    enableDispatchApplyInsetsFullscreenCompatImpl()
}