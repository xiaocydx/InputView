package com.xiaocydx.inputview.sample.scene.figure.overlay

import androidx.fragment.app.FragmentActivity
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.FragmentEditorAdapter

/**
 * @author xcc
 * @date 2024/4/13
 */
enum class FigureEditor : Editor {
    TEXT, GRID, DUBBING
}

class FigureEditAdapter(
    fragmentActivity: FragmentActivity
) : FragmentEditorAdapter<FigureEditor>(fragmentActivity) {
    override val ime = FigureEditor.TEXT

    override fun getEditorKey(editor: FigureEditor) = editor.name

    override fun onCreateFragment(editor: FigureEditor) = when(editor) {
        FigureEditor.TEXT -> null
        FigureEditor.GRID -> FigureGridFragment()
        FigureEditor.DUBBING -> DubbingFragment()
    }
}