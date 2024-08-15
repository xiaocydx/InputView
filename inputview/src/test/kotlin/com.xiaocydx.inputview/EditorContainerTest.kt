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

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import io.mockk.spyk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [EditorContainer]的单元测试
 *
 * @author xcc
 * @date 2023/1/13
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class EditorContainerTest {
    private lateinit var scenario: ActivityScenario<TestInputViewActivity>

    @Before
    fun setup() {
        scenario = launch(TestInputViewActivity::class.java).moveToState(RESUMED)
    }

    @After
    fun release() {
        scenario.close()
    }

    @Test
    fun showChecked() {
        scenario.onActivity {
            val container = EditorContainer(it)
            val adapter = TestEditorAdapter()
            container.setAdapter(adapter)

            val listener = spyk<EditorChangedListener<TestEditor>>()
            adapter.addEditorChangedListener(listener)

            assertThat(container.showChecked(TestEditor.Ime)).isTrue()
            assertThat(container.current).isEqualTo(TestEditor.Ime)
            assertThat(container.childCount).isEqualTo(0)
            verify(exactly = 1) { listener.onEditorChanged(null, TestEditor.Ime) }

            assertThat(container.showChecked(TestEditor.A)).isTrue()
            assertThat(container.current).isEqualTo(TestEditor.A)
            assertThat(container.childCount).isEqualTo(0)
            verify(exactly = 1) { listener.onEditorChanged(TestEditor.Ime, TestEditor.A) }

            assertThat(container.showChecked(TestEditor.B)).isTrue()
            assertThat(container.current).isEqualTo(TestEditor.B)
            assertThat(container.childCount).isEqualTo(0)
            verify(exactly = 1) { listener.onEditorChanged(TestEditor.A, TestEditor.B) }
        }
    }

    @Test
    fun hideChecked() {
        scenario.onActivity {
            val container = EditorContainer(it)
            val adapter = TestEditorAdapter()
            container.setAdapter(adapter)

            val listener = spyk<EditorChangedListener<TestEditor>>()
            adapter.addEditorChangedListener(listener)

            assertThat(container.showChecked(TestEditor.A)).isTrue()
            assertThat(container.current).isEqualTo(TestEditor.A)
            assertThat(container.childCount).isEqualTo(0)
            verify(exactly = 1) { listener.onEditorChanged(null, TestEditor.A) }

            assertThat(container.hideChecked(TestEditor.A)).isTrue()
            assertThat(container.current).isNull()
            assertThat(container.childCount).isEqualTo(0)
            verify(exactly = 1) { listener.onEditorChanged(TestEditor.A, null) }
        }
    }

    @Test
    fun repeatShowChecked() {
        scenario.onActivity {
            val container = EditorContainer(it)
            val adapter = TestEditorAdapter()
            container.setAdapter(adapter)

            val listener = spyk<EditorChangedListener<TestEditor>>()
            adapter.addEditorChangedListener(listener)
            assertThat(container.showChecked(TestEditor.A)).isTrue()
            assertThat(container.showChecked(TestEditor.A)).isFalse()
            verify(exactly = 1) { listener.onEditorChanged(null, TestEditor.A) }
        }
    }

    @Test
    fun repeatHideChecked() {
        scenario.onActivity {
            val container = EditorContainer(it)
            val adapter = TestEditorAdapter()
            container.setAdapter(adapter)

            val listener = spyk<EditorChangedListener<TestEditor>>()
            adapter.addEditorChangedListener(listener)
            assertThat(container.showChecked(TestEditor.A)).isTrue()
            assertThat(container.hideChecked(TestEditor.A)).isTrue()
            assertThat(container.hideChecked(TestEditor.A)).isFalse()
            verify(exactly = 1) { listener.onEditorChanged(TestEditor.A, null) }
        }
    }

    @Test
    fun dispatchingShowChecked() {
        scenario.onActivity {
            val container = EditorContainer(it)
            val adapter = TestEditorAdapter()
            container.setAdapter(adapter)

            val firstListener = spyk<EditorChangedListener<TestEditor>>()
            val lastListener = spyk<EditorChangedListener<TestEditor>>()
            val midListener = spyk(object : EditorChangedListener<TestEditor> {
                override fun onEditorChanged(previous: TestEditor?, current: TestEditor?) {
                    if (previous == null && current == TestEditor.Ime) container.showChecked(TestEditor.A)
                }
            })
            adapter.addEditorChangedListener(lastListener)
            adapter.addEditorChangedListener(midListener)
            adapter.addEditorChangedListener(firstListener)

            // null变为IME，分发过程被midListener拦截，lastListener不会触发
            container.showChecked(TestEditor.Ime)
            verify(exactly = 1) { firstListener.onEditorChanged(null, TestEditor.Ime) }
            verify(exactly = 1) { midListener.onEditorChanged(null, TestEditor.Ime) }
            verify(exactly = 0) { lastListener.onEditorChanged(null, TestEditor.Ime) }

            // midListener拦截后，触发IME变为A，重新执行完整的分发过程
            verify(exactly = 1) { firstListener.onEditorChanged(TestEditor.Ime, TestEditor.A) }
            verify(exactly = 1) { midListener.onEditorChanged(TestEditor.Ime, TestEditor.A) }
            verify(exactly = 1) { lastListener.onEditorChanged(TestEditor.Ime, TestEditor.A) }
        }
    }

    @Test
    fun dispatchingHideChecked() {
        scenario.onActivity {
            val container = EditorContainer(it)
            val adapter = TestEditorAdapter()
            container.setAdapter(adapter)

            val firstListener = spyk<EditorChangedListener<TestEditor>>()
            val lastListener = spyk<EditorChangedListener<TestEditor>>()
            val midListener = spyk(object : EditorChangedListener<TestEditor> {
                override fun onEditorChanged(previous: TestEditor?, current: TestEditor?) {
                    if (previous == null && current == TestEditor.Ime) container.hideChecked(TestEditor.Ime)
                }
            })
            adapter.addEditorChangedListener(lastListener)
            adapter.addEditorChangedListener(midListener)
            adapter.addEditorChangedListener(firstListener)

            // null变为IME，分发过程被midListener拦截，lastListener不会触发
            container.showChecked(TestEditor.Ime)
            verify(exactly = 1) { firstListener.onEditorChanged(null, TestEditor.Ime) }
            verify(exactly = 1) { midListener.onEditorChanged(null, TestEditor.Ime) }
            verify(exactly = 0) { lastListener.onEditorChanged(null, TestEditor.Ime) }

            // midListener拦截后，触发IME变为null，重新执行完整的分发过程
            verify(exactly = 1) { firstListener.onEditorChanged(TestEditor.Ime, null) }
            verify(exactly = 1) { midListener.onEditorChanged(TestEditor.Ime, null) }
            verify(exactly = 1) { lastListener.onEditorChanged(TestEditor.Ime, null) }
        }
    }

    @Test
    fun consumePendingChange() {
        scenario.onActivity {
            val container = EditorContainer(it)
            val adapter = TestEditorAdapter()
            container.setAdapter(adapter)

            assertThat(container.showChecked(TestEditor.A)).isTrue()
            assertThat(container.consumePendingChange()).isTrue()
            assertThat(container.childCount).isEqualTo(1)
            assertThat(container.changeRecord.previous).isNull()
            assertThat(container.changeRecord.current).isEqualTo(TestEditor.A)
            assertThat(container.changeRecord.previousChild).isNull()
            assertThat(container.changeRecord.currentChild).isInstanceOf(TestViewA::class.java)

            assertThat(container.hideChecked(TestEditor.A)).isTrue()
            assertThat(container.consumePendingChange()).isTrue()
            assertThat(container.childCount).isEqualTo(0)
            assertThat(container.changeRecord.previous).isEqualTo(TestEditor.A)
            assertThat(container.changeRecord.current).isNull()
            assertThat(container.changeRecord.previousChild).isInstanceOf(TestViewA::class.java)
            assertThat(container.changeRecord.currentChild).isNull()
        }
    }

    @Test
    fun repeatConsumePendingChange() {
        scenario.onActivity {
            val container = EditorContainer(it)
            val adapter = TestEditorAdapter()
            container.setAdapter(adapter)

            assertThat(container.showChecked(TestEditor.A)).isTrue()
            assertThat(container.consumePendingChange()).isTrue()
            assertThat(container.consumePendingChange()).isFalse()
        }
    }

    @Test
    fun removePreviousNotImmediately() {
        scenario.onActivity {
            val container = EditorContainer(it)
            container.setRemovePreviousImmediately(false)
            val adapter = TestEditorAdapter()
            container.setAdapter(adapter)
            container.showChecked(TestEditor.A)
            container.consumePendingChange()
            container.showChecked(TestEditor.B)
            container.consumePendingChange()
            assertThat(container.childCount).isEqualTo(2)
            assertThat(container.getChildAt(0)).isInstanceOf(TestViewA::class.java)
            assertThat(container.getChildAt(1)).isInstanceOf(TestViewB::class.java)
        }
    }

    @Test
    fun resetRemovePreviousImmediately() {
        scenario.onActivity {
            val container = EditorContainer(it)
            container.setRemovePreviousImmediately(false)
            val adapter = TestEditorAdapter()
            container.setAdapter(adapter)
            container.showChecked(TestEditor.A)
            container.consumePendingChange()
            container.showChecked(TestEditor.B)
            container.consumePendingChange()
            container.setRemovePreviousImmediately(true)
            assertThat(container.childCount).isEqualTo(1)
            assertThat(container.getChildAt(0)).isInstanceOf(TestViewB::class.java)
        }
    }

    private enum class TestEditor : Editor {
        Ime, A, B
    }

    private class TestEditorAdapter : EditorAdapter<TestEditor>() {
        override val ime: TestEditor = TestEditor.Ime

        override fun onCreateView(parent: ViewGroup, editor: TestEditor): View? = when (editor) {
            TestEditor.Ime -> null
            TestEditor.A -> TestViewA(parent.context)
            TestEditor.B -> TestViewB(parent.context)
        }
    }

    private class TestViewA(context: Context?) : View(context)
    private class TestViewB(context: Context?) : View(context)
}