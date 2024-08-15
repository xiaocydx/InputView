package com.xiaocydx.inputview.sample.scene.figure

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.xiaocydx.cxrv.list.MutableStateList
import com.xiaocydx.cxrv.list.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.lang.ref.WeakReference

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

    fun currentSceneFlow(): Flow<FigureScene?> {
        return _figureState.map { it.currentScene }.distinctUntilChanged()
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
        submitPendingScene(scene = null)
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
     * 提交待处理的[scene]，由视图做进一步处理
     */
    fun submitPendingScene(scene: FigureScene?) = _figureState.update {
        val pending = it.pendingScene
        if (it.currentScene == scene) {
            if (pending == null) return
            return@update it.copy(pendingScene = null)
        }
        if (pending != null && pending.scene == scene) return
        it.copy(pendingScene = PendingScene(scene))
    }

    /**
     * 消费待处理的`scene`，设置当前的[FigureScene]
     */
    fun consumePendingScene(current: FigureScene?) = _figureState.update {
        it.pendingScene ?: return
        it.copy(pendingScene = null, currentScene = current)
    }

    suspend fun requestView(request: PendingView.Request): WeakReference<View>? {
        _figureState.update { it.copy(pendingView = request) }
        val result = _figureState.map { it.pendingView }
            .filterIsInstance<PendingView.Result>().first()
        consumePendingView(current = null)
        return result.ref
    }

    fun consumePendingView(current: PendingView?) {
        _figureState.update { it.copy(pendingView = current) }
    }
}

data class FigureState(
    val currentPosition: Int = NO_POSITION,
    val currentScene: FigureScene? = null,
    val currentText: String = "",
    val pendingRemove: Figure? = null,
    val pendingScene: PendingScene? = null,
    val pendingView: PendingView? = null
)

data class PendingScene(val scene: FigureScene?)

sealed class PendingView {
    sealed class Request : PendingView() {
        data object Figure : Request()
        data object Text : Request()
    }
    data class Result(val ref: WeakReference<View>?) : PendingView()
}