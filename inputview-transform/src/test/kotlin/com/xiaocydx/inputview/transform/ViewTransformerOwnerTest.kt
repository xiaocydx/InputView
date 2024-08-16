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
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [ViewTransformerOwner]的单元测试
 *
 * @author xcc
 * @date 2024/8/14
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class ViewTransformerOwnerTest {
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
    fun add() {
        scenario.onActivity {
            val owner = it.contentView.viewTransform()
            owner.setHost(it.overlay)

            val transformer = TestTransformer()
            owner.add(transformer)
            assertThat(owner.has(transformer)).isTrue()
            assertThat(it.overlay.has(transformer)).isTrue()
        }
    }

    @Test
    fun remove() {
        scenario.onActivity {
            val owner = it.contentView.viewTransform()
            owner.setHost(it.overlay)

            val transformer = TestTransformer()
            owner.add(transformer)
            owner.remove(transformer)
            assertThat(owner.has(transformer)).isFalse()
            assertThat(it.overlay.has(transformer)).isFalse()
        }
    }

    @Test
    fun detachThenAttach() {
        scenario.onActivity {
            val owner = it.contentView.viewTransform()
            owner.setHost(it.overlay)

            val transformer = TestTransformer()
            owner.add(transformer)

            val parent = it.contentView.parent as ViewGroup
            parent.removeView(it.contentView)
            assertThat(owner.has(transformer)).isTrue()
            assertThat(it.overlay.has(transformer)).isFalse()

            parent.addView(it.contentView)
            assertThat(owner.has(transformer)).isTrue()
            assertThat(it.overlay.has(transformer)).isTrue()
        }
    }

    private class TestTransformer : Transformer() {
        override fun match(state: ImperfectState) = true
    }
}