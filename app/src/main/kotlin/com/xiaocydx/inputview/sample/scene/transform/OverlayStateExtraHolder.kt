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

package com.xiaocydx.inputview.sample.scene.transform

import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformation.EnforcerScope
import com.xiaocydx.inputview.sample.scene.transform.OverlayTransformation.State
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * [State]的额外属性持有类，不包含变换逻辑，负责请求分发[State]
 *
 * @author xcc
 * @date 2024/4/13
 */
@Deprecated(
    message = "实现类的职责不够清晰，调度流程不够完善",
    replaceWith = ReplaceWith("待替换为InputView.createOverlay()")
)
class OverlayStateExtraHolder<T : Any>(value: T) : OverlayTransformation<State> {
    private val receiver = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1, onBufferOverflow = DROP_OLDEST
    )

    var value = value
        private set

    fun setValue(value: T, request: Boolean) {
        this.value = value
        if (request) receiver.tryEmit(Unit)
    }

    override fun launch(state: State, scope: EnforcerScope) {
        if (state.current == null) return
        receiver.onEach { scope.requestDispatch(state) }.launchIn(scope)
    }
}