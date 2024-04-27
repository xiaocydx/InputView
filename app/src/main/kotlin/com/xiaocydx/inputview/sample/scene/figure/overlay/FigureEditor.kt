package com.xiaocydx.inputview.sample.scene.figure.overlay

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.FragmentEditorAdapter
import com.xiaocydx.inputview.sample.editor_adapter.fragment.EmojiFragment

/**
 * @author xcc
 * @date 2024/4/13
 */
enum class FigureEditor : Editor {
    INPUT, INPUT_IDLE, EMOJI, GRID, DUBBING
}

class FigureEditAdapter(
    lifecycle: Lifecycle,
    fragmentManager: FragmentManager
) : FragmentEditorAdapter<FigureEditor>(lifecycle, fragmentManager) {
    override val ime = FigureEditor.INPUT

    override fun getEditorKey(editor: FigureEditor) = editor.name

    override fun onCreateFragment(editor: FigureEditor) = when (editor) {
        FigureEditor.INPUT,
        FigureEditor.INPUT_IDLE -> null
        FigureEditor.EMOJI -> EmojiFragment()
        FigureEditor.GRID -> FigureGridFragment()
        FigureEditor.DUBBING -> DubbingFragment()
    }
}