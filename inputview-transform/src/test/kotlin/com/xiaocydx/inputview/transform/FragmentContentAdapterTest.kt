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

package com.xiaocydx.inputview.transform

import android.os.Build
import android.os.Looper.getMainLooper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * [FragmentContentAdapter]的单元测试
 *
 * @author xcc
 * @date 2024/8/14
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class FragmentContentAdapterTest {
    private lateinit var scenario: ActivityScenario<TestActivity>

    @Before
    fun setup() {
        scenario = launch(TestActivity::class.java).moveToState(RESUMED)
    }

    @After
    fun release() {
        scenario.close()
    }

    @Test
    fun createFragment() {
        scenario.onActivity {
            val fm = it.supportFragmentManager

            it.overlay.go(TestScene.A)
            assertThat(it.overlay.current).isEqualTo(TestScene.A)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragmentA()).isNotNull()

            it.overlay.go(TestScene.B)
            assertThat(it.overlay.current).isEqualTo(TestScene.B)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragmentB()).isNotNull()
        }
    }

    @Test
    fun createChildFragment() {
        scenario.onActivity {
            val fm = it.supportFragmentManager

            it.overlay.go(TestScene.A)
            shadowOf(getMainLooper()).idle()
            val childA = fm.fragmentA()!!.childFragmentManager
            assertThat(childA.fragmentVp2s()).hasSize(VP2_ITEM_COUNT)

            it.overlay.go(TestScene.B)
            shadowOf(getMainLooper()).idle()
            val childB = fm.fragmentB()!!.childFragmentManager
            assertThat(childB.fragmentVp2s()).hasSize(VP2_ITEM_COUNT)
        }
    }

    @Test
    fun updateFragmentMaxLifecycle() {
        scenario.onActivity {
            val fm = it.supportFragmentManager

            it.overlay.go(TestScene.A)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(RESUMED)

            it.overlay.go(TestScene.B)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(STARTED)
            assertThat(fm.fragmentB()!!.lifecycleState()).isEqualTo(RESUMED)

            it.overlay.go(null)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(STARTED)
            assertThat(fm.fragmentB()!!.lifecycleState()).isEqualTo(STARTED)
        }
    }

    @Test
    fun updateChildFragmentMaxLifecycle() {
        scenario.onActivity {
            val fm = it.supportFragmentManager

            it.overlay.go(TestScene.A)
            shadowOf(getMainLooper()).idle()
            val childA = fm.fragmentA()!!.childFragmentManager
            assertThat(childA.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(RESUMED)
            assertThat(childA.fragmentVp2s().lastOrNull()?.lifecycleState()).isEqualTo(STARTED)

            it.overlay.go(TestScene.B)
            shadowOf(getMainLooper()).idle()
            val childB = fm.fragmentB()!!.childFragmentManager
            assertThat(childB.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(RESUMED)
            assertThat(childB.fragmentVp2s().lastOrNull()?.lifecycleState()).isEqualTo(STARTED)
            assertThat(childA.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(STARTED)
            assertThat(childA.fragmentVp2s().lastOrNull()?.lifecycleState()).isEqualTo(STARTED)

            it.overlay.go(null)
            shadowOf(getMainLooper()).idle()
            assertThat(childB.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(STARTED)
            assertThat(childB.fragmentVp2s().lastOrNull()?.lifecycleState()).isEqualTo(STARTED)
            assertThat(childA.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(STARTED)
            assertThat(childA.fragmentVp2s().lastOrNull()?.lifecycleState()).isEqualTo(STARTED)
        }
    }

    @Test
    fun missUpdateFragmentMaxLifecycle() {
        scenario.onActivity {
            val fm = it.supportFragmentManager
            it.contentAdapter.setDelayUpdateFragmentMaxLifecycleEnabled(false)

            it.overlay.go(TestScene.A)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(RESUMED)
        }

        scenario.moveToState(CREATED).onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.isStateSaved).isTrue()
            it.overlay.go(null)
            shadowOf(getMainLooper()).idle()
            // Activity已完成saveState，不允许提交事务将fragmentA的max设为STARTED
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(CREATED)
        }

        scenario.moveToState(RESUMED).onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.isStateSaved).isFalse()
            shadowOf(getMainLooper()).idle()
            // Activity转换到RESUMED，fragmentA的max仍为RESUMED
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(RESUMED)
        }
    }

    @Test
    fun missUpdateChildFragmentMaxLifecycle() {
        scenario.onActivity {
            val fm = it.supportFragmentManager
            it.contentAdapter.setDelayUpdateFragmentMaxLifecycleEnabled(false)

            it.overlay.go(TestScene.A)
            shadowOf(getMainLooper()).idle()
            val child = fm.fragmentA()!!.childFragmentManager
            assertThat(child.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(RESUMED)
        }

        scenario.moveToState(CREATED).onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.isStateSaved).isTrue()
            it.overlay.go(null)
            shadowOf(getMainLooper()).idle()
            // Activity已完成saveState，不允许提交事务将fragmentVp2的max设为STARTED
            val child = fm.fragmentA()!!.childFragmentManager
            assertThat(child.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(CREATED)
        }

        scenario.moveToState(RESUMED).onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.isStateSaved).isFalse()
            shadowOf(getMainLooper()).idle()
            // Activity转换到RESUMED，fragmentVp2的max仍为RESUMED
            val child = fm.fragmentA()!!.childFragmentManager
            assertThat(child.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(RESUMED)
        }
    }

    @Test
    fun delayUpdateFragmentMaxLifecycle() {
        scenario.onActivity {
            val fm = it.supportFragmentManager
            it.overlay.go(TestScene.A)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(RESUMED)
        }

        scenario.moveToState(CREATED).onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.isStateSaved).isTrue()
            it.overlay.go(null)
            shadowOf(getMainLooper()).idle()
            // Activity已完成saveState，不允许提交事务将fragmentA的max设为STARTED
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(CREATED)
        }

        scenario.moveToState(RESUMED).onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.isStateSaved).isFalse()
            // Activity转换到RESUMED，fragmentA的max修正为STARTED
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(STARTED)
        }
    }

    @Test
    fun delayUpdateChildFragmentMaxLifecycle() {
        scenario.onActivity {
            val fm = it.supportFragmentManager
            it.overlay.go(TestScene.A)
            shadowOf(getMainLooper()).idle()
            val child = fm.fragmentA()!!.childFragmentManager
            assertThat(child.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(RESUMED)
        }

        scenario.moveToState(CREATED).onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.isStateSaved).isTrue()
            it.overlay.go(null)
            shadowOf(getMainLooper()).idle()
            // Activity已完成saveState，不允许提交事务将fragmentVp2的max设为STARTED
            val child = fm.fragmentA()!!.childFragmentManager
            assertThat(child.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(CREATED)
        }

        scenario.moveToState(RESUMED).onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.isStateSaved).isFalse()
            // Activity转换到RESUMED，fragmentVp2的max修正为STARTED
            val child = fm.fragmentA()!!.childFragmentManager
            assertThat(child.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(STARTED)
        }
    }

    @Test
    fun recreateFragment() {
        scenario.onActivity {
            it.overlay.go(TestScene.A)
            shadowOf(getMainLooper()).idle()
        }

        scenario.recreate().onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.fragments.filterIsInstance<TestFragmentA>()).hasSize(1)
        }
    }

    @Test
    fun recreateChildFragment() {
        scenario.onActivity {
            it.overlay.go(TestScene.A)
            shadowOf(getMainLooper()).idle()
        }

        scenario.recreate().onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.fragmentA()!!.childFragmentManager.fragmentVp2s()).hasSize(VP2_ITEM_COUNT)
        }
    }

    @Test
    fun restoreFragment() {
        scenario.onActivity {
            it.overlay.go(TestScene.A)
            shadowOf(getMainLooper()).idle()
        }

        scenario.recreate().onActivity {
            val fm = it.supportFragmentManager
            assertThat(it.overlay.current).isNull()
            it.overlay.go(TestScene.A)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragments.filterIsInstance<TestFragmentA>()).hasSize(1)
        }
    }

    @Test
    fun restoreChildFragment() {
        scenario.onActivity {
            it.overlay.go(TestScene.A)
            shadowOf(getMainLooper()).idle()
        }

        scenario.recreate().onActivity {
            val fm = it.supportFragmentManager
            assertThat(it.overlay.current).isNull()
            it.overlay.go(TestScene.A)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragmentA()!!.childFragmentManager.fragmentVp2s()).hasSize(VP2_ITEM_COUNT)
        }
    }

    @Test
    fun recreateFragmentAddToContainer() {
        scenario.onActivity {
            it.overlay.go(TestScene.A)
            shadowOf(getMainLooper()).idle()
        }

        scenario.recreate().onActivity {
            val fm = it.supportFragmentManager
            // FragmentContentAdapter在重建完成后，移除Fragment.view
            assertThat(fm.fragmentA()!!.view!!.parent).isNull()
        }
    }

    @Test
    fun recreateUpdateFragmentMaxLifecycle() {
        scenario.onActivity {
            val fm = it.supportFragmentManager
            it.overlay.go(TestScene.A)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(RESUMED)
        }

        scenario.recreate().onActivity {
            val fm = it.supportFragmentManager
            // FragmentContentAdapter在重建完成后，更新fragmentA的生命周期状态
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(STARTED)

            it.overlay.go(TestScene.A)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(RESUMED)
        }
    }

    @Test
    fun recreateUpdateChildFragmentMaxLifecycle() {
        scenario.onActivity {
            val fm = it.supportFragmentManager
            it.overlay.go(TestScene.A)
            shadowOf(getMainLooper()).idle()
            val childA = fm.fragmentA()!!.childFragmentManager
            assertThat(childA.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(RESUMED)
        }

        scenario.recreate().onActivity {
            val fm = it.supportFragmentManager
            // FragmentContentAdapter在重建完成后，更新fragmentVp2的生命周期状态
            val childA = fm.fragmentA()!!.childFragmentManager
            assertThat(childA.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(STARTED)

            it.overlay.go(TestScene.A)
            shadowOf(getMainLooper()).idle()
            assertThat(childA.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(RESUMED)
        }
    }

    @Test
    fun removeRecreateFragment() {
        scenario.onActivity {
            it.viewModel.canAttachOverlay = false
            it.overlay.go(TestScene.A)
            shadowOf(getMainLooper()).idle()
        }

        scenario.recreate().onActivity {
            val fm = it.supportFragmentManager
            // 重建的FragmentA缺少container，无法恢复view.layoutParams。
            // FragmentContentAdapter在重建完成后，移除fragmentA及其childFragment
            assertThat(fm.fragmentA()).isNull()
            it.viewModel.canAttachOverlay = true
        }
    }

    private fun Fragment.lifecycleState() = lifecycle.currentState

    private fun FragmentManager.fragmentA(): TestFragmentA? {
        return fragments.filterIsInstance<TestFragmentA>().firstOrNull()
    }

    private fun FragmentManager.fragmentB(): TestFragmentB? {
        return fragments.filterIsInstance<TestFragmentB>().firstOrNull()
    }

    private fun FragmentManager.fragmentVp2s(): List<TestFragmentVp2> {
        return fragments.filterIsInstance<TestFragmentVp2>()
    }
}