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

import android.os.Build
import android.os.Bundle
import android.os.Looper.getMainLooper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * [FragmentEditorAdapter]的单元测试
 *
 * @author xcc
 * @date 2023/10/12
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class FragmentEditorAdapterTest {
    private lateinit var scenario: ActivityScenario<TestActivity>
    private lateinit var editorAdapter: TestEditorAdapter

    @Before
    fun setup() {
        scenario = launch(TestActivity::class.java).moveToState(RESUMED)
        scenario.onActivity {
            editorAdapter = TestEditorAdapter(it)
            it.inputView.editorAdapter = editorAdapter
        }
    }

    @Test
    fun createFragment() {
        scenario.onActivity {
            val fm = it.supportFragmentManager
            editorAdapter.notifyShow(TestEditor.A)
            assertThat(editorAdapter.current).isEqualTo(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.testFragmentA()).isNotNull()

            editorAdapter.notifyShow(TestEditor.B)
            assertThat(editorAdapter.current).isEqualTo(TestEditor.B)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.testFragmentB()).isNotNull()
        }
    }

    @Test
    fun updateFragmentMaxLifecycle() {
        scenario.onActivity {
            val fm = it.supportFragmentManager
            editorAdapter.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.testFragmentA()?.lifecycle?.currentState).isEqualTo(RESUMED)

            editorAdapter.notifyShow(TestEditor.B)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.testFragmentA()?.lifecycle?.currentState).isEqualTo(STARTED)
            assertThat(fm.testFragmentB()?.lifecycle?.currentState).isEqualTo(RESUMED)

            editorAdapter.notifyHide(TestEditor.B)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.testFragmentA()?.lifecycle?.currentState).isEqualTo(STARTED)
            assertThat(fm.testFragmentB()?.lifecycle?.currentState).isEqualTo(STARTED)
        }
    }

    @Test
    fun missUpdateFragmentMaxLifecycle() {
        editorAdapter.setDelayUpdateFragmentMaxLifecycleEnabled(false)
        scenario.onActivity {
            val fm = it.supportFragmentManager
            editorAdapter.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            editorAdapter.notifyShow(TestEditor.B)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.testFragmentA()?.lifecycle?.currentState).isEqualTo(STARTED)
            assertThat(fm.testFragmentB()?.lifecycle?.currentState).isEqualTo(RESUMED)
        }

        scenario.moveToState(CREATED).onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.isStateSaved).isTrue()
            editorAdapter.notifyHide(TestEditor.B)
            shadowOf(getMainLooper()).idle()
            // Activity已完成saveState，不允许提交事务将testFragmentB的max设为STARTED
            assertThat(fm.testFragmentA()?.lifecycle?.currentState).isEqualTo(CREATED)
            assertThat(fm.testFragmentB()?.lifecycle?.currentState).isEqualTo(CREATED)
        }

        scenario.moveToState(RESUMED).onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.isStateSaved).isFalse()
            // Activity转换到RESUMED，testFragmentB的max仍为RESUMED
            assertThat(fm.testFragmentA()?.lifecycle?.currentState).isEqualTo(STARTED)
            assertThat(fm.testFragmentB()?.lifecycle?.currentState).isEqualTo(RESUMED)
        }
        editorAdapter.setDelayUpdateFragmentMaxLifecycleEnabled(true)
    }

    @Test
    fun delayUpdateFragmentMaxLifecycle() {
        scenario.onActivity {
            val fm = it.supportFragmentManager
            editorAdapter.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            editorAdapter.notifyShow(TestEditor.B)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.testFragmentA()?.lifecycle?.currentState).isEqualTo(STARTED)
            assertThat(fm.testFragmentB()?.lifecycle?.currentState).isEqualTo(RESUMED)
        }

        scenario.moveToState(CREATED).onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.isStateSaved).isTrue()
            editorAdapter.notifyHide(TestEditor.B)
            shadowOf(getMainLooper()).idle()
            // Activity已完成saveState，不允许提交事务将testFragmentB的max设为STARTED
            assertThat(fm.testFragmentA()?.lifecycle?.currentState).isEqualTo(CREATED)
            assertThat(fm.testFragmentB()?.lifecycle?.currentState).isEqualTo(CREATED)
        }

        scenario.moveToState(RESUMED).onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.isStateSaved).isFalse()
            // Activity转换到RESUMED，允许提交事务将testFragmentB的max修正为STARTED
            assertThat(fm.testFragmentA()?.lifecycle?.currentState).isEqualTo(STARTED)
            assertThat(fm.testFragmentB()?.lifecycle?.currentState).isEqualTo(STARTED)
        }
    }

    @Test
    fun recreateFragment() {
        scenario.onActivity {
            editorAdapter.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            editorAdapter.notifyShow(TestEditor.B)
            shadowOf(getMainLooper()).idle()
        }

        repeat(2) {
            scenario.recreate().onActivity {
                val fm = it.supportFragmentManager
                val editorAdapter = TestEditorAdapter(it)
                it.inputView.editorAdapter = editorAdapter
                assertThat(editorAdapter.current).isNull()
                assertThat(fm.fragments.filterIsInstance<TestFragmentA>()).hasSize(1)
                assertThat(fm.fragments.filterIsInstance<TestFragmentB>()).hasSize(1)
            }
        }
    }

    @Test
    fun restoreFragment() {
        scenario.onActivity {
            editorAdapter.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            editorAdapter.notifyShow(TestEditor.B)
            shadowOf(getMainLooper()).idle()
        }

        repeat(2) {
            scenario.recreate().onActivity {
                val fm = it.supportFragmentManager
                val editorAdapter = TestEditorAdapter(it)
                it.inputView.editorAdapter = editorAdapter
                assertThat(editorAdapter.current).isNull()
                editorAdapter.notifyShow(TestEditor.A)
                shadowOf(getMainLooper()).idle()
                editorAdapter.notifyShow(TestEditor.B)
                shadowOf(getMainLooper()).idle()
                assertThat(fm.fragments.filterIsInstance<TestFragmentA>()).hasSize(1)
                assertThat(fm.fragments.filterIsInstance<TestFragmentB>()).hasSize(1)
            }
        }
    }

    private fun FragmentManager.testFragmentA(): TestFragmentA? {
        return fragments.filterIsInstance<TestFragmentA>().firstOrNull()
    }

    private fun FragmentManager.testFragmentB(): TestFragmentB? {
        return fragments.filterIsInstance<TestFragmentB>().firstOrNull()
    }

    private enum class TestEditor : Editor {
        IME, A, B
    }

    private class TestEditorAdapter(
        fragmentActivity: FragmentActivity
    ) : FragmentEditorAdapter<TestEditor>(fragmentActivity) {
        override val ime = TestEditor.IME

        override fun getEditorKey(editor: TestEditor) = editor.name

        override fun onCreateFragment(editor: TestEditor): Fragment? = when (editor) {
            TestEditor.IME -> null
            TestEditor.A -> TestFragmentA()
            TestEditor.B -> TestFragmentB()
        }
    }

    class TestFragmentA : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = View(requireContext())
    }

    class TestFragmentB : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = View(requireContext())
    }
}