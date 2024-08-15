package com.xiaocydx.inputview.sample.scene.figure

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import com.xiaocydx.inputview.Editor
import com.xiaocydx.inputview.FragmentEditorAdapter
import com.xiaocydx.inputview.sample.editor_adapter.fragment.EmojiFragment
import com.xiaocydx.inputview.sample.scene.figure.FigureContent.Cover
import com.xiaocydx.inputview.sample.scene.figure.FigureContent.Text
import com.xiaocydx.inputview.sample.scene.figure.FigureEditor.Emoji
import com.xiaocydx.inputview.sample.scene.figure.FigureEditor.FigureDubbing
import com.xiaocydx.inputview.sample.scene.figure.FigureEditor.FigureGrid
import com.xiaocydx.inputview.sample.scene.figure.FigureEditor.Ime
import com.xiaocydx.inputview.sample.scene.figure.FigureEditor.ImeIdle
import com.xiaocydx.inputview.sample.scene.figure.content.CoverFragment
import com.xiaocydx.inputview.sample.scene.figure.editor.DubbingFragment
import com.xiaocydx.inputview.sample.scene.figure.editor.FigureGridFragment
import com.xiaocydx.inputview.transform.Content
import com.xiaocydx.inputview.transform.FragmentContentAdapter
import com.xiaocydx.inputview.transform.Scene

/**
 * @author xcc
 * @date 2024/8/15
 */
enum class FigureScene(
    override val content: FigureContent,
    override val editor: FigureEditor
) : Scene<FigureContent, FigureEditor> {
    InputText(Text, Ime),
    InputIdle(Text, ImeIdle),
    InputEmoji(Text, Emoji),
    SelectFigure(Cover, FigureGrid),
    SelectDubbing(Cover, FigureDubbing)
}

enum class FigureContent : Content {
    Text, Cover
}

enum class FigureEditor : Editor {
    Ime, ImeIdle, Emoji, FigureGrid, FigureDubbing
}

class FigureContentAdapter(
    lifecycle: Lifecycle,
    fragmentManager: FragmentManager
) : FragmentContentAdapter<FigureContent>(lifecycle, fragmentManager) {

    override fun getContentKey(content: FigureContent) = content.name

    override fun onCreateFragment(content: FigureContent) = when(content) {
        Text -> null
        Cover -> CoverFragment()
    }
}

class FigureEditAdapter(
    lifecycle: Lifecycle,
    fragmentManager: FragmentManager
) : FragmentEditorAdapter<FigureEditor>(lifecycle, fragmentManager) {
    override val ime = Ime

    override fun getEditorKey(editor: FigureEditor) = editor.name

    override fun onCreateFragment(editor: FigureEditor) = when (editor) {
        Ime,
        ImeIdle -> null
        Emoji -> EmojiFragment()
        FigureGrid -> FigureGridFragment()
        FigureDubbing -> DubbingFragment()
    }
}