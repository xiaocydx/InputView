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

@file:Suppress("INVISIBLE_REFERENCE", "CANNOT_OVERRIDE_INVISIBLE_MEMBER", "PackageDirectoryMismatch")

package com.xiaocydx.inputview.transform

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.xiaocydx.insets.systembar.SystemBar
import com.xiaocydx.insets.systembar.SystemBarExtensions
import com.xiaocydx.insets.systembar.name

/**
 * 扩展[SystemBar]的检查逻辑
 *
 * @author xcc
 * @date 2024/8/22
 */
internal class FragmentContentChecker : SystemBarExtensions {

    override fun checkUnsupportedOnResume(fragment: Fragment, parent: ViewGroup) = run {
        if (parent !is ContentContainer) return@run null
        """使用${SystemBar.name}的Fragment不支持Overlay
               |    ${fragment.javaClass.canonicalName ?: ""} : ${SystemBar.name}
        """.trimMargin()
    }
}