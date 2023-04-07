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
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle.State
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [InputView]的单元测试
 *
 * @author xcc
 * @date 2023/1/13
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class InputViewTest {
    private lateinit var scenario: ActivityScenario<TestActivity>

    @Before
    fun setup() {
        scenario = launch(TestActivity::class.java).moveToState(State.CREATED)
    }

    @Test
    fun viewTreeWindow_Init_Success() {
        scenario.onActivity { activity ->
            val inputView = activity.inputView
            assertThat(inputView.findViewTreeWindow()).isNotNull()
        }
    }

    @Test
    fun editorAdapter_AttachAndDetach_Success() {
        scenario.onActivity { activity ->
            val inputView = activity.inputView

            val adapter1 = spyk(ImeAdapter())
            val adapter2 = spyk(ImeAdapter())
            inputView.editorAdapter = adapter1
            inputView.editorAdapter = adapter2

            val host = inputView.getEditorHost()
            assertThat(host.editorOffset).isEqualTo(0)
            verify(exactly = 1) { adapter1.onAttachToEditorHost(host) }
            verify(exactly = 1) { adapter1.onDetachFromEditorHost(host) }
            verify(exactly = 1) { adapter2.onAttachToEditorHost(host) }
        }
    }

    @Test
    fun editorAnimator_AttachAndDetach_Success() {
        scenario.onActivity { activity ->
            val inputView = activity.inputView

            val animator1 = spyk(NopEditorAnimator())
            val animator2 = spyk(NopEditorAnimator())
            inputView.editorAnimator = animator1
            inputView.editorAnimator = animator2

            val host = inputView.getEditorHost()
            verify(exactly = 1) { animator1.onAttachToEditorHost(host) }
            verify(exactly = 1) { animator1.onDetachFromEditorHost(host) }
            verify(exactly = 1) { animator2.onAttachToEditorHost(host) }
        }
    }

    @Test
    fun adjustPan_UpdateEditorOffset_Success() {
        scenario.moveToState(State.RESUMED).onActivity { activity ->
            val inputView = activity.inputView
            inputView.editorMode = EditorMode.ADJUST_PAN
            val host = inputView.getEditorHost()
            val editorView = inputView.getEditorView()
            val contentView = inputView.getContentView()
            assertThat(contentView).isNotNull()

            val insets = WindowInsetsCompat.Builder().build().toWindowInsets()
            assertThat(insets).isNotNull()
            inputView.onApplyWindowInsets(insets!!)
            assertThat(host.navBarOffset).isEqualTo(0)

            val offset = 10
            host.updateEditorOffset(offset)
            assertThat(host.editorOffset).isEqualTo(offset)
            assertThat(editorView.top).isEqualTo(inputView.height - offset)
            assertThat(contentView!!.top).isEqualTo(-offset)
        }
    }
}