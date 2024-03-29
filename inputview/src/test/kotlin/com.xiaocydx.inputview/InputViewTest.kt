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
        scenario = launch(TestActivity::class.java).moveToState(RESUMED)
    }

    @After
    fun release() {
        scenario.close()
    }

    @Test
    fun initViewTreeWindow() {
        scenario.onActivity {
            assertThat(it.inputView.findViewTreeWindow()).isNotNull()
        }
    }

    @Test
    fun editorAdapterAttachAndDetach() {
        scenario.onActivity {
            val adapter1 = spyk(ImeAdapter())
            val adapter2 = spyk(ImeAdapter())
            it.inputView.editorAdapter = adapter1
            it.inputView.editorAdapter = adapter2

            val host = it.inputView.getEditorHost()
            assertThat(host.editorOffset).isEqualTo(0)
            verify(exactly = 1) { adapter1.onAttachedToHost(host) }
            verify(exactly = 1) { adapter1.onDetachedFromHost(host) }
            verify(exactly = 1) { adapter2.onAttachedToHost(host) }
        }
    }

    @Test
    fun editorAnimatorAttachAndDetach() {
        scenario.onActivity {
            val animator1 = spyk(NopEditorAnimator())
            val animator2 = spyk(NopEditorAnimator())
            it.inputView.editorAnimator = animator1
            it.inputView.editorAnimator = animator2

            val host = it.inputView.getEditorHost()
            verify(exactly = 1) { animator1.onAttachedToHost(host) }
            verify(exactly = 1) { animator1.onDetachedFromHost(host) }
            verify(exactly = 1) { animator2.onAttachedToHost(host) }
        }
    }

    @Test
    fun replicableEditorChangedListener() {
        scenario.onActivity {
            val adapter1 = ImeAdapter()
            it.inputView.editorAdapter = adapter1

            val listener1 = EditorChangedListener<Editor> { _, _ -> }
            val listener2 = ReplicableEditorChangedListener { _, _ -> }
            adapter1.addEditorChangedListener(listener1)
            adapter1.addEditorChangedListener(listener2)

            val adapter2 = ImeAdapter()
            it.inputView.editorAdapter = adapter2
            assertThat(adapter2.containsListener(listener1)).isFalse()
            assertThat(adapter2.containsListener(listener2)).isTrue()
        }
    }

    @Test
    fun replicableAnimationCallback() {
        scenario.onActivity {
            val animator1 = NopEditorAnimator()
            it.inputView.editorAnimator = animator1

            val callback1 = object : AnimationCallback {}
            val callback2 = object : ReplicableAnimationCallback {}
            animator1.addAnimationCallback(callback1)
            animator1.addAnimationCallback(callback2)

            val animator2 = NopEditorAnimator()
            it.inputView.editorAnimator = animator2
            assertThat(animator2.containsCallback(callback1)).isFalse()
            assertThat(animator2.containsCallback(callback2)).isTrue()
        }
    }

    @Test
    fun adjustPanUpdateEditorOffset() {
        scenario.onActivity {
            it.inputView.editorMode = EditorMode.ADJUST_PAN
            val host = it.inputView.getEditorHost()
            val insets = WindowInsetsCompat.Builder().build().toWindowInsets()
            assertThat(insets).isNotNull()
            it.inputView.onApplyWindowInsets(insets!!)
            assertThat(host.navBarOffset).isEqualTo(0)

            val offset = 10
            host.updateEditorOffset(offset)
            assertThat(host.editorOffset).isEqualTo(offset)
        }
    }
}