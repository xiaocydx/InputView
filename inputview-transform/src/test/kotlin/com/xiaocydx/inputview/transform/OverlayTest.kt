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
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.inputview.notifyHideCurrent
import io.mockk.spyk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [Overlay]的单元测试
 *
 * @author xcc
 * @date 2024/8/14
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class OverlayTest {
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
    fun repeatAttach() {
        scenario.onActivity {
            assertThat(it.overlay.attach(it.window)).isFalse()
        }
    }

    @Test
    fun go() {
        scenario.onActivity {
            assertThat(it.overlay.go(TestScene.A)).isTrue()
            assertThat(it.overlay.go(TestScene.A)).isFalse()
            assertThat(it.overlay.current).isEqualTo(TestScene.A)

            assertThat(it.overlay.go(TestScene.B)).isTrue()
            assertThat(it.overlay.previous).isEqualTo(TestScene.A)
            assertThat(it.overlay.current).isEqualTo(TestScene.B)
        }
    }

    @Test
    fun sceneChangedListener() {
        scenario.onActivity {
            val listener = spyk<SceneChangedListener<TestScene>>()
            it.overlay.sceneChangedListener = listener
            it.overlay.go(TestScene.A)
            it.overlay.go(TestScene.A)
            verify(exactly = 1) { listener.onChanged(null, TestScene.A) }
            it.overlay.go(TestScene.B)
            verify(exactly = 1) { listener.onChanged(TestScene.A, TestScene.B) }
        }
    }

    @Test
    fun sceneEditorConverter() {
        scenario.onActivity {
            val listener = spyk<SceneChangedListener<TestScene>>()
            val converter = SceneEditorConverter<TestScene> { _, c, n -> if (n == null) null else c }
            it.overlay.sceneChangedListener = listener
            it.overlay.sceneEditorConverter = converter
            it.overlay.go(TestScene.A)
            it.editorAdapter.notifyHideCurrent()
            assertThat(it.overlay.current).isNull()
            verify(exactly = 1) { listener.onChanged(null, TestScene.A) }
            verify(exactly = 1) { listener.onChanged(TestScene.A, null) }
        }
    }
}