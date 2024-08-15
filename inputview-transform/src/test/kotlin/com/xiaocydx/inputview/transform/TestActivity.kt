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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.EditorAdapter
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.init

/**
 * @author xcc
 * @date 2024/8/14
 */
internal class TestActivity : AppCompatActivity() {
    lateinit var overlay: Overlay<TestScene>; private set
    lateinit var contentAdapter: TestContentAdapter; private set
    lateinit var editorAdapter: TestEditorAdapter; private set
    lateinit var contentView: View; private set
    lateinit var viewModel: TestViewModel; private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InputView.init(window)
        contentAdapter = TestContentAdapter(this)
        editorAdapter = TestEditorAdapter()
        overlay = InputView.createOverlay(
            lifecycleOwner = this,
            contentAdapter = contentAdapter,
            editorAdapter = editorAdapter
        )
        viewModel = ViewModelProvider(this, TestViewModel)[TestViewModel::class.java]
        overlay.attach(window)

        contentView = View(this)
        setContentView(contentView)
    }

    override fun onStart() {
        if (!viewModel.canAttachOverlay) {
            // 在重建fragment之前移除overlay的rootView
            val parent = findViewById<ViewGroup>(android.R.id.content)
            for (i in 0 until parent.childCount) {
                if (parent.getChildAt(i) == contentView) continue
                parent.removeViewAt(i)
                break
            }
        }
        super.onStart()
    }
}

class TestViewModel : ViewModel() {
    var canAttachOverlay = true

    companion object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass === TestViewModel::class.java)
            @Suppress("UNCHECKED_CAST")
            return TestViewModel() as T
        }
    }
}

internal enum class TestScene(
    override val content: TestContent,
    override val editor: TestEditor
) : Scene<TestContent, TestEditor> {
    A(TestContent.A, TestEditor.A), B(TestContent.B, TestEditor.B)
}

internal enum class TestEditor : Editor {
    Ime, A, B
}

internal enum class TestContent : Content {
    A, B
}

internal class TestEditorAdapter : EditorAdapter<TestEditor>() {
    override val ime = TestEditor.Ime
    override fun onCreateView(parent: ViewGroup, editor: TestEditor) = null
}

internal class TestContentAdapter(
    fragmentActivity: FragmentActivity
) : FragmentContentAdapter<TestContent>(fragmentActivity) {
    override fun getContentKey(content: TestContent) = content.name
    override fun onCreateFragment(content: TestContent) = when (content) {
        TestContent.A -> TestFragmentA()
        TestContent.B -> TestFragmentB()
    }
}

internal class TestFragmentA : TestFragment()
internal class TestFragmentB : TestFragment()

internal const val VP2_ITEM_COUNT = 2
private val vp2Id = ViewCompat.generateViewId()

internal abstract class TestFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ViewPager2(requireContext()).also {
        it.id = vp2Id
        it.offscreenPageLimit = 1
        it.adapter = TestFragmentStateAdapter(this)
        it.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, 400)
    }
}

private class TestFragmentStateAdapter(
    fragment: Fragment
) : FragmentStateAdapter(fragment) {
    override fun getItemCount() = VP2_ITEM_COUNT
    override fun createFragment(position: Int) = TestFragmentVp2()
}

class TestFragmentVp2 : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = View(requireContext()).apply {
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    }
}