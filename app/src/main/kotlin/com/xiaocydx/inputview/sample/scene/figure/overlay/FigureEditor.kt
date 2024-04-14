package com.xiaocydx.inputview.sample.scene.figure.overlay

import androidx.fragment.app.FragmentActivity
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.FragmentEditorAdapter
import com.xiaocydx.inputview.sample.editor_adapter.fragment.EmojiFragment
import com.xiaocydx.inputview.sample.scene.video.CommonFragment

/**
 * @author xcc
 * @date 2024/4/13
 */
enum class FigureEditor : Editor {
    INPUT, EMOJI, GRID, DUBBING
}

class FigureEditAdapter(
    fragmentActivity: FragmentActivity
) : FragmentEditorAdapter<FigureEditor>(fragmentActivity) {
    override val ime = FigureEditor.INPUT

    override fun getEditorKey(editor: FigureEditor) = editor.name

    override fun onCreateFragment(editor: FigureEditor) = when(editor) {
        FigureEditor.INPUT -> null
        FigureEditor.EMOJI -> EmojiFragment()
        FigureEditor.GRID -> FigureGridFragment()
        FigureEditor.DUBBING -> DubbingFragment()
    }
}