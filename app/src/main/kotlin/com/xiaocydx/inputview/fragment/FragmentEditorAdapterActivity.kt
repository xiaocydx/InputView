@file:Suppress("PrivatePropertyName")

package com.xiaocydx.inputview.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.xiaocydx.inputview.EmojiRecyclerView
import com.xiaocydx.inputview.FragmentEditorAdapter
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.MessageEditor
import com.xiaocydx.inputview.R
import com.xiaocydx.inputview.databinding.MessageListBinding
import com.xiaocydx.inputview.init

/**
 * @author xcc
 * @date 2023/10/11
 */
class FragmentEditorAdapterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InputView.init(window, gestureNavBarEdgeToEdge = true)
        val binding = MessageListBinding.inflate(layoutInflater)
        val editorAdapter = MessageFragmentEditorAdapter(this)
        setContentView(binding.init(window, editorAdapter).root)
    }
}

class MessageFragmentEditorAdapter(
    fragmentActivity: FragmentActivity
) : FragmentEditorAdapter<MessageEditor>(fragmentActivity) {
    override val ime = MessageEditor.IME

    override fun getEditorKey(editor: MessageEditor) = editor.name

    override fun onCreateFragment(editor: MessageEditor): Fragment? = when (editor) {
        MessageEditor.IME, MessageEditor.VOICE -> null
        MessageEditor.EMOJI -> EmojiFragment()
        MessageEditor.EXTRA -> ExtraFragment()
    }
}

class EmojiFragment : Fragment() {
    private val viewModel: EditorViewModel by viewModels()
    private val TAG = javaClass.canonicalName

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = EmojiRecyclerView(requireContext())
        .apply { id = viewModel.viewId }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.e(TAG, "viewModel.count = ${viewModel.count}")
        viewModel.increase()
        viewLifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                Log.e(TAG, "viewLifecycle.currentState = ${source.lifecycle.currentState}")
            }
        })
    }
}

class ExtraFragment : Fragment(R.layout.message_editor_extra) {
    private val viewModel: EditorViewModel by viewModels()
    private val TAG = javaClass.canonicalName

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.e(TAG, "viewModel.count = ${viewModel.count}")
        viewModel.increase()
        viewLifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                Log.e(TAG, "viewLifecycle.currentState = ${source.lifecycle.currentState}")
            }
        })
    }
}

class EditorViewModel : ViewModel() {
    var count = 0
        private set
    val viewId = ViewCompat.generateViewId()

    fun increase() {
        count++
    }
}