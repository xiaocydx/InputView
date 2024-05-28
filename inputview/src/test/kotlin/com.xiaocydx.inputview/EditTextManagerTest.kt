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

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
 * [EditTextManager]的单元测试
 *
 * @author xcc
 * @date 2024/4/6
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class EditTextManagerTest {
    private lateinit var inputViewScenario: ActivityScenario<TestInputViewActivity>
    private lateinit var imeAnimatorScenario: ActivityScenario<TestImeAnimatorActivity>

    @Before
    fun setup() {
        inputViewScenario = launch(TestInputViewActivity::class.java).moveToState(RESUMED)
        imeAnimatorScenario = launch(TestImeAnimatorActivity::class.java).moveToState(RESUMED)
    }

    @After
    fun release() {
        inputViewScenario.close()
        imeAnimatorScenario.close()
    }

    @Test
    fun hostAttachedToWindow() {
        inputViewScenario.onActivity {
            val manager = it.getEditTextManager()
            val host = it.inputView.getEditorHost()
            assertThat(it.inputView.isAttachedToWindow).isTrue()
            assertThat(manager.isHostRegistered(host)).isTrue()
            assertThat(manager.isEditTextAdded(it.editText)).isTrue()
        }
        imeAnimatorScenario.onActivity {
            val manager = it.getEditTextManager()
            val host = it.animator.getEditorHost()!!
            assertThat(it.contentView.isAttachedToWindow).isTrue()
            assertThat(manager.isHostRegistered(host)).isTrue()
            assertThat(manager.isEditTextAdded(it.editText)).isTrue()
        }
    }

    @Test
    fun hostDetachedFromWindow() {
        inputViewScenario.onActivity {
            val manager = it.getEditTextManager()
            val host = it.inputView.getEditorHost()
            it.inputView.removeFromParent()
            assertThat(it.inputView.isAttachedToWindow).isFalse()
            assertThat(manager.isHostRegistered(host)).isFalse()
            assertThat(manager.isEditTextAdded(it.editText)).isFalse()
        }
        imeAnimatorScenario.onActivity {
            val manager = it.getEditTextManager()
            val host = it.animator.getEditorHost()!!
            it.contentView.removeFromParent()
            assertThat(it.contentView.isAttachedToWindow).isFalse()
            assertThat(manager.isHostRegistered(host)).isFalse()
            assertThat(manager.isEditTextAdded(it.editText)).isFalse()
        }
    }

    @Test
    fun hostReattachedToWindow() {
        inputViewScenario.onActivity {
            val manager = it.getEditTextManager()
            val host = it.inputView.getEditorHost()
            val parent = it.inputView.removeFromParent()
            parent.addView(it.inputView)
            assertThat(it.inputView.isAttachedToWindow).isTrue()
            assertThat(manager.isHostRegistered(host)).isTrue()
            assertThat(manager.isEditTextAdded(it.editText)).isTrue()
        }
        imeAnimatorScenario.onActivity {
            val manager = it.getEditTextManager()
            val host = it.animator.getEditorHost()!!
            val parent = it.contentView.removeFromParent()
            parent.addView(it.contentView)
            assertThat(it.contentView.isAttachedToWindow).isTrue()
            assertThat(manager.isHostRegistered(host)).isTrue()
            assertThat(manager.isEditTextAdded(it.editText)).isTrue()
        }
    }

    @Test
    fun editTextAttachedToWindow() {
        inputViewScenario.onActivity {
            val manager = it.getEditTextManager()
            assertThat(it.editText.isAttachedToWindow).isTrue()
            assertThat(manager.isEditTextAdded(it.editText)).isTrue()
        }
        imeAnimatorScenario.onActivity {
            val manager = it.getEditTextManager()
            assertThat(it.editText.isAttachedToWindow).isTrue()
            assertThat(manager.isEditTextAdded(it.editText)).isTrue()
        }
    }

    @Test
    fun editTextDetachedFromWindow() {
        inputViewScenario.onActivity {
            val manager = it.getEditTextManager()
            it.editText.removeFromParent()
            assertThat(it.editText.isAttachedToWindow).isFalse()
            assertThat(manager.isEditTextAdded(it.editText)).isFalse()
        }
        imeAnimatorScenario.onActivity {
            val manager = it.getEditTextManager()
            it.editText.removeFromParent()
            assertThat(it.editText.isAttachedToWindow).isFalse()
            assertThat(manager.isEditTextAdded(it.editText)).isFalse()
        }
    }

    @Test
    fun editTextReattachedToWindow() {
        inputViewScenario.onActivity {
            val manager = it.getEditTextManager()
            val parent = it.editText.removeFromParent()
            parent.addView(it.editText)
            assertThat(it.editText.isAttachedToWindow).isTrue()
            assertThat(manager.isEditTextAdded(it.editText)).isTrue()
        }
        imeAnimatorScenario.onActivity {
            val manager = it.getEditTextManager()
            val parent = it.editText.removeFromParent()
            parent.addView(it.editText)
            assertThat(it.editText.isAttachedToWindow).isTrue()
            assertThat(manager.isEditTextAdded(it.editText)).isTrue()
        }
    }

    @Test
    fun setNewEditTextRemoveOldEditText() {
        inputViewScenario.onActivity {
            val manager = it.getEditTextManager()
            val oldEditText = it.editText
            val newEditText = EditText(it)
            it.contentView.addView(newEditText)

            assertThat(manager.isEditTextAdded(oldEditText)).isTrue()
            assertThat(manager.isEditTextAdded(newEditText)).isFalse()

            it.inputView.editText = newEditText
            assertThat(manager.isEditTextAdded(oldEditText)).isFalse()
            assertThat(manager.isEditTextAdded(newEditText)).isTrue()
        }
        imeAnimatorScenario.onActivity {
            val manager = it.getEditTextManager()
            val oldEditText = it.editText
            val newEditText = EditText(it)
            it.contentView.addView(newEditText)

            assertThat(manager.isEditTextAdded(oldEditText)).isTrue()
            assertThat(manager.isEditTextAdded(newEditText)).isFalse()

            it.animator.editText = newEditText
            assertThat(manager.isEditTextAdded(oldEditText)).isFalse()
            assertThat(manager.isEditTextAdded(newEditText)).isTrue()
        }
    }

    @Test
    fun clearEditTextRemoveEditTextHandle() {
        inputViewScenario.onActivity {
            val manager = it.getEditTextManager()
            assertThat(it.inputView.editText != null).isTrue()
            assertThat(manager.peekEditTextHandleSize()).isEqualTo(1)

            it.clearEditText()
            GcTrigger.runGc()
            assertThat(it.inputView.editText == null).isTrue()
            assertThat(manager.peekEditTextHandleSize()).isEqualTo(0)
        }
        imeAnimatorScenario.onActivity {
            val manager = it.getEditTextManager()
            assertThat(manager.peekEditTextHandleSize()).isEqualTo(1)

            it.clearEditText()
            GcTrigger.runGc()
            assertThat(it.animator.editText == null).isTrue()
            assertThat(manager.peekEditTextHandleSize()).isEqualTo(0)
        }
    }

    @Test
    @Suppress("UNUSED_VALUE")
    fun gcEditTextDelayRemoveEditTextHandle() {
        inputViewScenario.onActivity {
            val manager = it.getEditTextManager()
            it.editText.removeFromParent()
            assertThat(manager.peekEditTextHandleSize()).isEqualTo(0)

            var target: EditText? = EditText(it)
            manager.addEditText(target!!)
            assertThat(manager.peekEditTextHandleSize()).isEqualTo(1)

            target = null
            GcTrigger.runGc()
            assertThat(manager.peekEditTextHandleSize()).isEqualTo(1)
            assertThat(manager.getEditTextHandleSize()).isEqualTo(0)
        }
        imeAnimatorScenario.onActivity {
            val manager = it.getEditTextManager()
            it.editText.removeFromParent()
            assertThat(manager.peekEditTextHandleSize()).isEqualTo(0)

            var target: EditText? = EditText(it)
            manager.addEditText(target!!)
            assertThat(manager.peekEditTextHandleSize()).isEqualTo(1)

            target = null
            GcTrigger.runGc()
            assertThat(manager.peekEditTextHandleSize()).isEqualTo(1)
            assertThat(manager.getEditTextHandleSize()).isEqualTo(0)
        }
    }

    private fun Activity.getEditTextManager(): EditTextManager {
        return window.decorView.requireViewTreeWindow().getEditTextManager()
    }

    private fun View.removeFromParent(): ViewGroup {
        val parent = parent as ViewGroup
        parent.removeView(this)
        return parent
    }

    private object GcTrigger {

        fun runGc() {
            // Code taken from AOSP FinalizationTest:
            // https://android.googlesource.com/platform/libcore/+/master/support/src/test/java/libcore/
            // java/lang/ref/FinalizationTester.java
            // System.gc() does not garbage collect every time. Runtime.gc() is
            // more likely to perform a gc.
            Runtime.getRuntime().gc()
            enqueueReferences()
            System.runFinalization()
        }

        private fun enqueueReferences() {
            // Hack. We don't have a programmatic way to wait for the reference queue daemon to move
            // references to the appropriate queues.
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                throw AssertionError()
            }
        }
    }
}