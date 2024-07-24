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

@file:Suppress("PackageDirectoryMismatch")

package com.xiaocydx.inputview.transform

import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * @author xcc
 * @date 2024/7/22
 */
abstract class FragmentContentAdapter<T : Content>(
    private val lifecycle: Lifecycle,
    private val fragmentManager: FragmentManager
) : ContentAdapter<T>() {
    private val fragments = mutableMapOf<T, Fragment?>()
    private val restoreFragments = mutableMapOf<String, Fragment>()
    private val fragmentRestoreEnforcer = FragmentRestoreEnforcer()
    private val fragmentMaxLifecycleEnforcer = FragmentMaxLifecycleEnforcer()

    /**
     * 获取[content]的Key，Key将作为`Fragment.tag`的一部分
     */
    protected abstract fun getContentKey(content: T): String

    protected abstract fun onCreateFragment(content: T): Fragment?

    final override fun onCreateView(parent: ViewGroup, content: T): View? {
        val tag = "$KEY_PREFIX_FRAGMENT${getContentKey(content)}"
        val fragment = restoreFragments.remove(tag) ?: onCreateFragment(content)
        fragments[content] = fragment
        if (fragment != null) placeFragmentInContainer(parent, fragment, tag)
        return fragment?.view
    }

    private fun shouldDelayFragmentTransactions() = fragmentManager.isStateSaved

    private fun placeFragmentInContainer(container: ViewGroup, fragment: Fragment, tag: String) {
        // InputView确保在常规布局流程调用onCreateView()
        require(container.id != View.NO_ID) { "container未设置id" }
        require(!shouldDelayFragmentTransactions()) { "当前未处于常规布局流程" }
        var view = fragment.view
        when {
            fragment.isAdded && view == null -> {
                throw IllegalStateException("Fragment生命周期状态转换的异常情况")
            }
            fragment.isAdded && view != null && view.parent != null -> {
                throw IllegalStateException("FragmentRestoreEnforcer未移除View")
            }
            fragment.isAdded && view != null && view.layoutParams == null -> {
                throw IllegalStateException("重建的Fragment未对container添加View")
            }
            !fragment.isAdded && view != null -> {
                throw IllegalStateException("违背常规流程的异常情况")
            }
            !fragment.isAdded && view == null -> {
                // 不使用add(fragment, tag) + 手动添加Fragment.view的方案，
                // 原因是创建view时会丢失layoutParams，直观表现就是丢失尺寸。
                // 使用add(containerViewId, fragment, tag)的方案，其缺陷是
                // 重建流程会将Fragment.view添加到container，不符合当前设计，
                // FragmentRestoreEnforcer在重建完成后，移除Fragment.view。
                fragmentManager.beginTransaction()
                    .add(container.id, fragment, tag)
                    .setMaxLifecycle(fragment, STARTED)
                    .commitNow()
            }
        }
        view = fragment.view
        assert(view != null)
        val parent = view?.parent
        assert(parent == null || parent === container)
        // 移除view，在onCreateView()之后重新添加view，确保跟重建流程表现一致
        view?.let(container::removeView)
    }

    final override fun onAttachedToHost(host: ContentHost) {
        super.onAttachedToHost(host)
        fragmentRestoreEnforcer.register()
        fragmentMaxLifecycleEnforcer.register(host)
    }

    final override fun onDetachedFromHost(host: ContentHost) {
        super.onDetachedFromHost(host)
        fragmentRestoreEnforcer.unregister()
        fragmentMaxLifecycleEnforcer.unregister(host)
    }

    @VisibleForTesting
    internal fun setDelayUpdateFragmentMaxLifecycleEnabled(isEnabled: Boolean) {
        fragmentMaxLifecycleEnforcer.setDelayUpdateEnabled(isEnabled)
    }

    private inner class FragmentRestoreEnforcer : LifecycleEventObserver {

        fun register() {
            lifecycle.addObserver(this)
        }

        fun unregister() {
            lifecycle.removeObserver(this)
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            val currentState = source.lifecycle.currentState
            if (shouldDelayFragmentTransactions() || !currentState.isAtLeast(STARTED)) return
            unregister()

            val container = host?.container ?: return
            var toRemove: MutableList<Fragment>? = null
            require(container.id != View.NO_ID) { "container未设置id" }
            fragmentManager.fragments.takeIf { it.isNotEmpty() }?.forEach { fragment ->
                val tag = fragment.tag ?: return@forEach
                if (!tag.contains(KEY_PREFIX_FRAGMENT)) return@forEach
                if (fragments.containsValue(fragment)) return@forEach
                val view = fragment.view
                if (view?.parent === container) {
                    restoreFragments[tag] = fragment
                } else {
                    // view = null或parent = null
                    if (toRemove == null) {
                        toRemove = mutableListOf()
                    }
                    toRemove!!.add(fragment)
                }
            }

            if (restoreFragments.isEmpty() && toRemove.isNullOrEmpty()) return
            val transaction = fragmentManager.beginTransaction()
            restoreFragments.takeIf { it.isNotEmpty() }?.forEach {
                val fragment = it.value
                fragment.view?.let(container::removeView)
                // 重建的Fragment，生命周期状态可能是RESUMED，
                // 此时未显示Fragment，状态需要回退到STARTED。
                transaction.setMaxLifecycle(it.value, STARTED)
                fragment.setMenuVisibility(false)
            }
            toRemove?.takeIf { it.isNotEmpty() }?.forEach { fragment ->
                // 执行重建流程，container还未添加到视图树，
                // 此时重建的Fragment，其view缺少container，
                // view丢失layoutParams，直观表现就是丢失尺寸，
                // 移除重建的Fragment，放弃恢复状态，确保表现一致。
                transaction.remove(fragment)
            }
            transaction.takeIf { !it.isEmpty }?.commitNow()
        }
    }

    private inner class FragmentMaxLifecycleEnforcer {
        private var isAnimationRunning = false
        private var isDelayUpdateNeeded = false
        private var isDelayUpdateEnabled = true
        private var delayUpdateObserver: LifecycleObserver? = null
        private var transformer: Transformer? = null

        fun register(host: ContentHost) {
            delayUpdateObserver = LifecycleEventObserver { _, _ ->
                // 当动画结束时，可能错过了saveState，不允许提交事务，
                // 因此观察Lifecycle的状态更改，尝试提交事务修正状态。
                val isNeeded = isDelayUpdateNeeded.also { isDelayUpdateNeeded = false }
                if (!isNeeded || !isDelayUpdateEnabled) return@LifecycleEventObserver
                updateFragmentMaxLifecycle(host.current)
            }
            transformer = object : Transformer {
                override fun match(state: ImperfectState) = true

                override fun onPrepare(state: ImperfectState) {
                    isAnimationRunning = true
                }

                override fun onEnd(state: TransformState) {
                    // 当动画结束时，才转换Fragment的生命周期状态，
                    // 目的是对调用者提供一个协调动画卡顿问题的时机，
                    // 例如当Fragment的生命周期状态转换为RESUMED时，
                    // 才设置数据，申请下一帧重新布局，创建大量视图。
                    isAnimationRunning = false
                    updateFragmentMaxLifecycle(state.current?.content)
                }
            }
            delayUpdateObserver?.let(lifecycle::addObserver)
            transformer?.let(host::addTransformer)
        }

        fun unregister(host: ContentHost) {
            delayUpdateObserver?.let(lifecycle::removeObserver)
            transformer?.let(host::removeTransformer)
            delayUpdateObserver = null
            transformer = null
            isAnimationRunning = false
            isDelayUpdateNeeded = false
        }

        @VisibleForTesting
        fun setDelayUpdateEnabled(isEnabled: Boolean) {
            isDelayUpdateEnabled = isEnabled
        }

        private fun updateFragmentMaxLifecycle(current: Content?) {
            isDelayUpdateNeeded = false
            if (fragments.isEmpty()) return
            if (shouldDelayFragmentTransactions() || isAnimationRunning) {
                // 不使用commitNowAllowingStateLoss()，是因为此时没必要提交事务，而不是担心状态丢失
                isDelayUpdateNeeded = true
                return
            }
            val transaction = fragmentManager.beginTransaction()
            var toResume: Fragment? = null
            fragments.forEach action@{
                val content = it.key
                val fragment = it.value
                if (fragment == null || !fragment.isAdded) return@action
                if (content !== current) {
                    transaction.setMaxLifecycle(fragment, STARTED)
                } else {
                    toResume = fragment
                }
                fragment.setMenuVisibility(content === current)
            }
            toResume?.let { transaction.setMaxLifecycle(it, RESUMED) }
            transaction.takeIf { !it.isEmpty }?.commitNow()
        }
    }

    private companion object {
        const val KEY_PREFIX_FRAGMENT = "com.xiaocydx.inputview.overlay.FragmentContentAdapter.f#"
    }
}