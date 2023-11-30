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

import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE

/**
 * 显示[Editor]的布局模式
 *
 * @author xcc
 * @date 2023/1/10
 */
enum class EditorMode {

    /**
     * 类似[SOFT_INPUT_ADJUST_PAN]的作用，显示[Editor]平移`contentView`
     */
    ADJUST_PAN,

    /**
     * 类似[SOFT_INPUT_ADJUST_RESIZE]的作用，显示[Editor]修改`contentView`的尺寸
     */
    ADJUST_RESIZE
}