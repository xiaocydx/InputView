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
    fun showChecked() {
        scenario.onActivity {
            val container = EditorContainer(it)
            val adapter = spyk(TestEditorAdapter())
            container.setAdapter(adapter)

            assertThat(container.showChecked(TestEditor.IME)).isTrue()
            assertThat(container.current).isEqualTo(TestEditor.IME)
            assertThat(container.childCount).isEqualTo(0)
            verify(exactly = 1) { adapter.onEditorChanged(null, TestEditor.IME) }

            assertThat(container.showChecked(TestEditor.A)).isTrue()
            assertThat(container.current).isEqualTo(TestEditor.A)
            assertThat(container.childCount).isEqualTo(0)
            verify(exactly = 1) { adapter.onEditorChanged(TestEditor.IME, TestEditor.A) }

            assertThat(container.showChecked(TestEditor.B)).isTrue()
            assertThat(container.current).isEqualTo(TestEditor.B)
            assertThat(container.childCount).isEqualTo(0)
            verify(exactly = 1) { adapter.onEditorChanged(TestEditor.A, TestEditor.B) }
        }
    }

    @Test
    fun hideChecked() {
        scenario.onActivity {
            val container = EditorContainer(it)
            val adapter = spyk(TestEditorAdapter())
            container.setAdapter(adapter)

            assertThat(container.showChecked(TestEditor.A)).isTrue()
            assertThat(container.current).isEqualTo(TestEditor.A)
            assertThat(container.childCount).isEqualTo(0)
            verify(exactly = 1) { adapter.onEditorChanged(null, TestEditor.A) }

            assertThat(container.hideChecked(TestEditor.A)).isTrue()
            assertThat(container.current).isNull()
            assertThat(container.childCount).isEqualTo(0)
            verify(exactly = 1) { adapter.onEditorChanged(TestEditor.A, null) }
        }
    }

    @Test
    fun repeatShowChecked() {
        scenario.onActivity {
            val container = EditorContainer(it)
            val adapter = spyk(TestEditorAdapter())
            container.setAdapter(adapter)
            assertThat(container.showChecked(TestEditor.A)).isTrue()
            assertThat(container.showChecked(TestEditor.A)).isFalse()
            verify(exactly = 1) { adapter.onEditorChanged(null, TestEditor.A) }
        }
    }

    @Test
    fun repeatHideChecked() {
        scenario.onActivity {
            val container = EditorContainer(it)
            val adapter = spyk(TestEditorAdapter())
            container.setAdapter(adapter)
            assertThat(container.showChecked(TestEditor.A)).isTrue()
            assertThat(container.hideChecked(TestEditor.A)).isTrue()
            assertThat(container.hideChecked(TestEditor.A)).isFalse()
            verify(exactly = 1) { adapter.onEditorChanged(TestEditor.A, null) }
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
        IME, A, B
    }

    private class TestEditorAdapter : EditorAdapter<TestEditor>() {
        override val ime: TestEditor = TestEditor.IME

        override fun onCreateView(parent: ViewGroup, editor: TestEditor): View? = when (editor) {
            TestEditor.IME -> null
            TestEditor.A -> TestViewA(parent.context)
            TestEditor.B -> TestViewB(parent.context)
        }
    }

    private class TestViewA(context: Context?) : View(context)
    private class TestViewB(context: Context?) : View(context)
}