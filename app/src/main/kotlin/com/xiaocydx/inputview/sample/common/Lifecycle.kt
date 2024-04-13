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

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

val Fragment.viewLifecycle: Lifecycle
    get() = viewLifecycleOwner.lifecycle

val Fragment.viewLifecycleScope: CoroutineScope
    get() = viewLifecycleOwner.lifecycleScope

inline fun Lifecycle.doOnStateChanged(
    once: Boolean = false,
    crossinline action: (source: LifecycleOwner, event: Lifecycle.Event) -> Unit
): LifecycleObserver = object : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (once) source.lifecycle.removeObserver(this)
        action(source, event)
    }
}.also(::addObserver)

inline fun Lifecycle.doOnTargetState(
    state: Lifecycle.State,
    crossinline action: () -> Unit
): LifecycleObserver = object : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (source.lifecycle.currentState !== state) return
        source.lifecycle.removeObserver(this)
        action()
    }
}.also(::addObserver)

/**
 * 若`flow`的最后一个操作符是[Flow.flowWithLifecycle]，则可以调用该函数进行优化，
 * 避免[Flow.flowWithLifecycle]调用[callbackFlow]，创建额外的[Channel]，例如：
 * ```
 * flow
 *     .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
 *     .launchIn(lifecycle.coroutineScope)
 * ```
 * 替换为
 * ```
 * flow.launchRepeatOnLifecycle(lifecycle, Lifecycle.State.STARTED)
 * ```
 */
fun <T> Flow<T>.launchRepeatOnLifecycle(
    lifecycle: Lifecycle,
    state: Lifecycle.State = Lifecycle.State.STARTED
): Job = lifecycle.coroutineScope.launch {
    lifecycle.repeatOnLifecycle(state) { collect() }
}