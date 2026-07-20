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

import android.os.Looper
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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

suspend fun Lifecycle.awaitTargetState(state: Lifecycle.State) {
    if (currentState === state) return
    suspendCancellableCoroutine { cont ->
        val observer = doOnTargetState(state) { cont.resume(Unit) }
        cont.invokeOnCancellation {
            assert(Thread.currentThread() === Looper.getMainLooper().thread)
            removeObserver(observer)
        }
    }
}