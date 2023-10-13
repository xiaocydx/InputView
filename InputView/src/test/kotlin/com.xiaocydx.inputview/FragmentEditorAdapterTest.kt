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
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.common.truth.Truth.assertThat
import org.junit.After
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
            val editorAdapter = TestEditorAdapter(it)
            it.inputView.editorAdapter = editorAdapter

            editorAdapter.notifyShow(TestEditor.A)
            assertThat(editorAdapter.current).isEqualTo(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragmentA()).isNotNull()

            editorAdapter.notifyShow(TestEditor.B)
            assertThat(editorAdapter.current).isEqualTo(TestEditor.B)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragmentB()).isNotNull()
        }
    }

    @Test
    fun createChildFragment() {
        scenario.onActivity {
            val fm = it.supportFragmentManager
            val editorAdapter = TestEditorAdapter(it)
            it.inputView.editorAdapter = editorAdapter

            editorAdapter.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            val childA = fm.fragmentA()!!.childFragmentManager
            assertThat(childA.fragmentVp2s()).hasSize(ITEM_COUNT)

            editorAdapter.notifyShow(TestEditor.B)
            shadowOf(getMainLooper()).idle()
            val childB = fm.fragmentB()!!.childFragmentManager
            assertThat(childB.fragmentVp2s()).hasSize(ITEM_COUNT)
        }
    }

    @Test
    fun updateFragmentMaxLifecycle() {
        scenario.onActivity {
            val fm = it.supportFragmentManager
            val editorAdapter = TestEditorAdapter(it)
            it.inputView.editorAdapter = editorAdapter

            editorAdapter.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(RESUMED)

            editorAdapter.notifyShow(TestEditor.B)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(STARTED)
            assertThat(fm.fragmentB()!!.lifecycleState()).isEqualTo(RESUMED)

            editorAdapter.notifyHide(TestEditor.B)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(STARTED)
            assertThat(fm.fragmentB()!!.lifecycleState()).isEqualTo(STARTED)
        }
    }

    @Test
    fun updateChildFragmentMaxLifecycle() {
        scenario.onActivity {
            val fm = it.supportFragmentManager
            val editorAdapter = TestEditorAdapter(it)
            it.inputView.editorAdapter = editorAdapter

            editorAdapter.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            val childA = fm.fragmentA()!!.childFragmentManager
            assertThat(childA.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(RESUMED)
            assertThat(childA.fragmentVp2s().lastOrNull()?.lifecycleState()).isEqualTo(STARTED)

            editorAdapter.notifyShow(TestEditor.B)
            shadowOf(getMainLooper()).idle()
            val childB = fm.fragmentB()!!.childFragmentManager
            assertThat(childB.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(RESUMED)
            assertThat(childB.fragmentVp2s().lastOrNull()?.lifecycleState()).isEqualTo(STARTED)
            assertThat(childA.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(STARTED)
            assertThat(childA.fragmentVp2s().lastOrNull()?.lifecycleState()).isEqualTo(STARTED)

            editorAdapter.notifyHide(TestEditor.B)
            shadowOf(getMainLooper()).idle()
            assertThat(childB.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(STARTED)
            assertThat(childB.fragmentVp2s().lastOrNull()?.lifecycleState()).isEqualTo(STARTED)
            assertThat(childA.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(STARTED)
            assertThat(childA.fragmentVp2s().lastOrNull()?.lifecycleState()).isEqualTo(STARTED)
        }
    }

    @Test
    fun missUpdateFragmentMaxLifecycle() {
        var editorAdapter: TestEditorAdapter? = null
        scenario.onActivity {
            val fm = it.supportFragmentManager
            editorAdapter = TestEditorAdapter(it)
            it.inputView.editorAdapter = editorAdapter!!
            editorAdapter!!.setDelayUpdateFragmentMaxLifecycleEnabled(false)

            editorAdapter!!.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(RESUMED)
        }

        scenario.moveToState(CREATED).onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.isStateSaved).isTrue()
            editorAdapter!!.notifyHide(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            // Activity已完成saveState，不允许提交事务将fragmentA的max设为STARTED
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(CREATED)
        }

        scenario.moveToState(RESUMED).onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.isStateSaved).isFalse()
            // Activity转换到RESUMED，fragmentA的max仍为RESUMED
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(RESUMED)
        }
    }

    @Test
    fun missUpdateChildFragmentMaxLifecycle() {
        var editorAdapter: TestEditorAdapter? = null
        scenario.onActivity {
            val fm = it.supportFragmentManager
            editorAdapter = TestEditorAdapter(it)
            it.inputView.editorAdapter = editorAdapter!!
            editorAdapter!!.setDelayUpdateFragmentMaxLifecycleEnabled(false)

            editorAdapter!!.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            val child = fm.fragmentA()!!.childFragmentManager
            assertThat(child.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(RESUMED)
        }

        scenario.moveToState(CREATED).onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.isStateSaved).isTrue()
            editorAdapter!!.notifyHide(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            // Activity已完成saveState，不允许提交事务将fragmentVp2的max设为STARTED
            val child = fm.fragmentA()!!.childFragmentManager
            assertThat(child.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(CREATED)
        }

        scenario.moveToState(RESUMED).onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.isStateSaved).isFalse()
            // Activity转换到RESUMED，fragmentVp2的max仍为RESUMED
            val child = fm.fragmentA()!!.childFragmentManager
            assertThat(child.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(RESUMED)
        }
    }

    @Test
    fun delayUpdateFragmentMaxLifecycle() {
        var editorAdapter: TestEditorAdapter? = null
        scenario.onActivity {
            val fm = it.supportFragmentManager
            editorAdapter = TestEditorAdapter(it)
            it.inputView.editorAdapter = editorAdapter!!
            editorAdapter!!.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(RESUMED)
        }

        scenario.moveToState(CREATED).onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.isStateSaved).isTrue()
            editorAdapter!!.notifyHide(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            // Activity已完成saveState，不允许提交事务将fragmentA的max设为STARTED
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(CREATED)
        }

        scenario.moveToState(RESUMED).onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.isStateSaved).isFalse()
            // Activity转换到RESUMED，fragmentA的max仍为RESUMED
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(STARTED)
        }
    }

    @Test
    fun delayUpdateChildFragmentMaxLifecycle() {
        var editorAdapter: TestEditorAdapter? = null
        scenario.onActivity {
            val fm = it.supportFragmentManager
            editorAdapter = TestEditorAdapter(it)
            it.inputView.editorAdapter = editorAdapter!!
            editorAdapter!!.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            val child = fm.fragmentA()!!.childFragmentManager
            assertThat(child.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(RESUMED)
        }

        scenario.moveToState(CREATED).onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.isStateSaved).isTrue()
            editorAdapter!!.notifyHide(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            // Activity已完成saveState，不允许提交事务将fragmentVp2的max设为STARTED
            val child = fm.fragmentA()!!.childFragmentManager
            assertThat(child.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(CREATED)
        }

        scenario.moveToState(RESUMED).onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.isStateSaved).isFalse()
            // Activity转换到RESUMED，fragmentVp2的max仍为RESUMED
            val child = fm.fragmentA()!!.childFragmentManager
            assertThat(child.fragmentVp2s().firstOrNull()?.lifecycleState()).isEqualTo(STARTED)
        }
    }

    @Test
    fun recreateFragment() {
        scenario.onActivity {
            val editorAdapter = TestEditorAdapter(it)
            it.inputView.editorAdapter = editorAdapter
            editorAdapter.notifyShow(TestEditor.A)
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
            val editorAdapter = TestEditorAdapter(it)
            it.inputView.editorAdapter = editorAdapter
            editorAdapter.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
        }

        scenario.recreate().onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.fragmentA()!!.childFragmentManager.fragmentVp2s()).hasSize(ITEM_COUNT)
        }
    }

    @Test
    fun restoreFragment() {
        scenario.onActivity {
            val editorAdapter = TestEditorAdapter(it)
            it.inputView.editorAdapter = editorAdapter
            editorAdapter.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
        }

        scenario.recreate().onActivity {
            val fm = it.supportFragmentManager
            val editorAdapter = TestEditorAdapter(it)
            it.inputView.editorAdapter = editorAdapter
            assertThat(editorAdapter.current).isNull()
            editorAdapter.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragments.filterIsInstance<TestFragmentA>()).hasSize(1)
        }
    }

    @Test
    fun restoreChildFragment() {
        scenario.onActivity {
            val editorAdapter = TestEditorAdapter(it)
            it.inputView.editorAdapter = editorAdapter
            editorAdapter.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
        }

        scenario.recreate().onActivity {
            val fm = it.supportFragmentManager
            val editorAdapter = TestEditorAdapter(it)
            it.inputView.editorAdapter = editorAdapter
            assertThat(editorAdapter.current).isNull()
            editorAdapter.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragmentA()!!.childFragmentManager.fragmentVp2s()).hasSize(ITEM_COUNT)
        }
    }

    @Test
    fun recreateFragmentAddToContainer() {
        scenario.onActivity {
            val editorAdapter = TestEditorAdapter(it)
            it.inputView.editorAdapter = editorAdapter
            editorAdapter.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
        }

        scenario.recreate().onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.fragmentA()!!.view!!.parent).isNotNull()
        }

        scenario.recreate().onActivity {
            val fm = it.supportFragmentManager
            // FragmentEditorAdapter在重建完成后，移除Fragment.view
            it.inputView.editorAdapter = TestEditorAdapter(it)
            assertThat(fm.fragmentA()!!.view!!.parent).isNull()
        }
    }

    @Test
    fun recreateChildFragmentAddToContainer() {
        scenario.onActivity {
            val editorAdapter = TestEditorAdapter(it)
            it.inputView.editorAdapter = editorAdapter
            editorAdapter.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
        }

        scenario.recreate().onActivity {
            val fm = it.supportFragmentManager
            // 重建fragmentA的过程，会重建fragmentVp2，添加到ViewHolder.itemView
            fm.fragmentA()!!.childFragmentManager.fragmentVp2s()
                .forEach { f -> assertThat(f.view!!.parent).isNotNull() }
        }
    }

    @Test
    fun recreateUpdateFragmentMaxLifecycle() {
        scenario.onActivity {
            val editorAdapter = TestEditorAdapter(it)
            it.inputView.editorAdapter = editorAdapter
            val fm = it.supportFragmentManager
            editorAdapter.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(RESUMED)
        }

        scenario.recreate().onActivity {
            val fm = it.supportFragmentManager
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(RESUMED)
        }

        scenario.recreate().onActivity {
            val fm = it.supportFragmentManager
            // FragmentEditorAdapter在重建完成后，更新fragmentA的生命周期状态
            val editorAdapter = TestEditorAdapter(it)
            it.inputView.editorAdapter = editorAdapter
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(STARTED)

            editorAdapter.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
            assertThat(fm.fragmentA()!!.lifecycleState()).isEqualTo(RESUMED)
        }
    }

    @Test
    fun removeRecreateFragment() {
        scenario.onActivity {
            val editorAdapter = TestEditorAdapter(it)
            it.inputView.editorAdapter = editorAdapter
            it.viewModel.canSetInputView = false
            editorAdapter.notifyShow(TestEditor.A)
            shadowOf(getMainLooper()).idle()
        }

        scenario.recreate().onActivity {
            val fm = it.supportFragmentManager
            // 重建的FragmentA缺少container，无法恢复view.layoutParams
            assertThat(fm.fragmentA()!!.view!!.parent).isNull()
        }

        scenario.recreate().onActivity {
            val fm = it.supportFragmentManager
            // FragmentEditorAdapter在重建完成后，移除fragmentA及其childFragment
            it.inputView.editorAdapter = TestEditorAdapter(it)
            assertThat(fm.fragmentA()).isNull()
            it.viewModel.canSetInputView = true
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

    private enum class TestEditor : Editor {
        IME, A, B
    }

    private class TestEditorAdapter(
        fragmentActivity: FragmentActivity
    ) : FragmentEditorAdapter<TestEditor>(fragmentActivity) {
        override val ime = TestEditor.IME
        override fun getEditorKey(editor: TestEditor) = editor.name
        override fun onCreateFragment(editor: TestEditor) = when (editor) {
            TestEditor.IME -> null
            TestEditor.A -> TestFragmentA()
            TestEditor.B -> TestFragmentB()
        }
    }

    class TestFragmentA : TestFragment()
    class TestFragmentB : TestFragment()
    abstract class TestFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = ViewPager2(requireContext()).also {
            it.id = VP2_ID
            it.offscreenPageLimit = 1
            it.adapter = TestFragmentStateAdapter(this)
            it.layoutParams = LayoutParams(MATCH_PARENT, 400)
        }
    }

    private class TestFragmentStateAdapter(
        fragment: Fragment
    ) : FragmentStateAdapter(fragment) {
        override fun getItemCount() = ITEM_COUNT
        override fun createFragment(position: Int) = TestFragmentVp2()
    }

    class TestFragmentVp2 : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = View(requireContext()).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
    }

    private companion object {
        const val ITEM_COUNT = 2
        val VP2_ID = ViewCompat.generateViewId()
    }
}