package com.xiaocydx.inputview

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
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
 * [EditorView]的单元测试
 *
 * @author xcc
 * @date 2023/1/13
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
class EditorViewTest {
    private lateinit var scenario: ActivityScenario<TestActivity>

    @Before
    fun setup() {
        scenario = launch(TestActivity::class.java).moveToState(State.CREATED)
    }

    @Test
    fun showChecked_Success() {
        scenario.onActivity {
            val editorView = EditorView(it)
            val adapter = spyk(TestEditorAdapter())
            editorView.setAdapter(adapter)

            assertThat(editorView.showChecked(TestEditor.IME)).isTrue()
            assertThat(editorView.current).isEqualTo(TestEditor.IME)
            assertThat(editorView.childCount).isEqualTo(0)
            verify(exactly = 1) { adapter.onEditorChanged(null, TestEditor.IME) }

            assertThat(editorView.showChecked(TestEditor.A)).isTrue()
            assertThat(editorView.current).isEqualTo(TestEditor.A)
            assertThat(editorView.childCount).isEqualTo(1)
            assertThat(editorView.changeRecord.previousChild).isNull()
            assertThat(editorView.changeRecord.currentChild).isInstanceOf(TestViewA::class.java)
            verify(exactly = 1) { adapter.onEditorChanged(TestEditor.IME, TestEditor.A) }

            assertThat(editorView.showChecked(TestEditor.B)).isTrue()
            assertThat(editorView.current).isEqualTo(TestEditor.B)
            assertThat(editorView.childCount).isEqualTo(1)
            assertThat(editorView.changeRecord.previousChild).isInstanceOf(TestViewA::class.java)
            assertThat(editorView.changeRecord.currentChild).isInstanceOf(TestViewB::class.java)
            verify(exactly = 1) { adapter.onEditorChanged(TestEditor.A, TestEditor.B) }
        }
    }

    @Test
    fun hideChecked_Success() {
        scenario.onActivity {
            val editorView = EditorView(it)
            val adapter = spyk(TestEditorAdapter())
            editorView.setAdapter(adapter)

            assertThat(editorView.showChecked(TestEditor.A)).isTrue()
            assertThat(editorView.current).isEqualTo(TestEditor.A)
            assertThat(editorView.childCount).isEqualTo(1)
            assertThat(editorView.changeRecord.previousChild).isNull()
            assertThat(editorView.changeRecord.currentChild).isInstanceOf(TestViewA::class.java)
            verify(exactly = 1) { adapter.onEditorChanged(null, TestEditor.A) }

            assertThat(editorView.hideChecked(TestEditor.A)).isTrue()
            assertThat(editorView.current).isNull()
            assertThat(editorView.childCount).isEqualTo(0)
            assertThat(editorView.changeRecord.previousChild).isInstanceOf(TestViewA::class.java)
            assertThat(editorView.changeRecord.currentChild).isNull()
            verify(exactly = 1) { adapter.onEditorChanged(TestEditor.A, null) }
        }
    }

    @Test
    fun repeat_ShowChecked() {
        scenario.onActivity {
            val editorView = EditorView(it)
            val adapter = spyk(TestEditorAdapter())
            editorView.setAdapter(adapter)
            assertThat(editorView.showChecked(TestEditor.A)).isTrue()
            assertThat(editorView.showChecked(TestEditor.A)).isFalse()
            verify(exactly = 1) { adapter.onEditorChanged(null, TestEditor.A) }
        }
    }

    @Test
    fun repeat_HideChecked() {
        scenario.onActivity {
            val editorView = EditorView(it)
            val adapter = spyk(TestEditorAdapter())
            editorView.setAdapter(adapter)
            assertThat(editorView.showChecked(TestEditor.A)).isTrue()
            assertThat(editorView.hideChecked(TestEditor.A)).isTrue()
            assertThat(editorView.hideChecked(TestEditor.A)).isFalse()
            verify(exactly = 1) { adapter.onEditorChanged(TestEditor.A, null) }
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