package com.xiaocydx.inputview.sample.scene.figure

import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.xiaocydx.cxrv.list.MutableStateList
import com.xiaocydx.cxrv.list.asStateFlow
import com.xiaocydx.inputview.sample.scene.figure.overlay.FigureEditor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * @author xcc
 * @date 2024/4/13
 */
class FigureViewModel : ViewModel() {
    private val _figureState = MutableStateFlow(FigureState())
    private val figureList = MutableStateList<Figure>()

    val figureState = _figureState.asStateFlow()
    val figureListFlow = figureList.asStateFlow()
    val currentFigure: Figure?
        get() = figureList.getOrNull(_figureState.value.currentPosition)

    init {
        figureList.submit(FigureSource.generateList(size = 50))
    }

    fun currentFigureFlow(): Flow<Figure?> {
        return _figureState.map { currentFigure }.distinctUntilChanged()
    }

    fun currentEditorFlow(): Flow<FigureEditor?> {
        return _figureState.map { it.currentEditor }.distinctUntilChanged()
    }

    fun selectPosition(position: Int) {
        _figureState.update { it.copy(currentPosition = position) }
    }

    fun selectFigure(figure: Figure) {
        selectPosition(figureList.indexOfFirst { it.id == figure.id })
    }

    fun confirmDubbing(dubbing: Dubbing?) {
        val position = _figureState.value.currentPosition
        val current = figureList.getOrNull(position) ?: return
        figureList[position] = current.copy(dubbing = dubbing ?: Dubbing())
        submitPendingEditor(editor = null)
    }

    fun confirmText(text: String?) {
        _figureState.update { it.copy(currentText = text ?: "") }
    }

    /**
     * 提交待移除的[figure]，由视图做进一步处理
     */
    fun submitPendingRemove(figure: Figure) {
        _figureState.update { it.copy(pendingRemove = figure) }
    }

    /**
     * 消费待移除的`figure`，更新列表并滚动到指定位置
     */
    fun consumePendingRemove() {
        val remove = _figureState.value.pendingRemove ?: return
        val position = figureList.indexOfFirst { it.id == remove.id }
        if (position == _figureState.value.currentPosition) {
            val nextPosition = when {
                position == 0 -> 0
                figureList.getOrNull(position - 1) != null -> position - 1
                figureList.getOrNull(position + 1) != null -> position + 1
                else -> NO_POSITION
            }
            selectPosition(nextPosition)
        }
        if (position != NO_POSITION) figureList.removeAt(position)
        _figureState.update { it.copy(pendingRemove = null) }
    }

    /**
     * 提交待处理的[editor]，由视图做进一步处理
     */
    fun submitPendingEditor(
        editor: FigureEditor?,
        request: Boolean = false
    ) = _figureState.update {
        val pending = it.pendingEditor
        if (!request && it.currentEditor == editor) {
            if (pending == null) return
            return@update it.copy(pendingEditor = null)
        }
        if (request && pending != null) return
        if (pending != null && pending.editor == editor) return
        it.copy(pendingEditor = PendingEditor(editor, request))
    }

    /**
     * 消费待处理的`editor`，设置当前的[FigureEditor]
     */
    fun consumePendingEditor(current: FigureEditor?) = _figureState.update {
        it.pendingEditor ?: return
        it.copy(pendingEditor = null, currentEditor = current)
    }
}

data class FigureState(
    val currentPosition: Int = NO_POSITION,
    val currentEditor: FigureEditor? = null,
    val currentText: String = "",
    val pendingRemove: Figure? = null,
    val pendingEditor: PendingEditor? = null
)

data class PendingEditor(val editor: FigureEditor?, val request: Boolean)