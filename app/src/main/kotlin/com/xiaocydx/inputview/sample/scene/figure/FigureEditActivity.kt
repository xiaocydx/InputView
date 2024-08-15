package com.xiaocydx.inputview.sample.scene.figure

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.sample.common.launchRepeatOnLifecycle
import com.xiaocydx.inputview.sample.common.snackbar
import com.xiaocydx.inputview.sample.databinding.ActivityFigureEditBinding
import com.xiaocydx.inputview.sample.scene.figure.pager.FigurePager
import com.xiaocydx.inputview.sample.scene.figure.pager.TextPager
import com.xiaocydx.inputview.transform.EditorBackground
import com.xiaocydx.inputview.transform.Overlay
import com.xiaocydx.inputview.transform.SceneChangedListener
import com.xiaocydx.inputview.transform.addSceneFadeChange
import com.xiaocydx.inputview.transform.createOverlay
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.navigationBars
import com.xiaocydx.insets.statusBars
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

/**
 * 通过选中编辑的交互案例，演示[InputView]的使用
 *
 * @author xcc
 * @date 2024/4/13
 */
class FigureEditActivity : AppCompatActivity() {
    private val sharedViewModel: FigureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InputView.init(window, statusBarEdgeToEdge = true, gestureNavBarEdgeToEdge = true)
        setContentView(ActivityFigureEditBinding.inflate(layoutInflater).init().root)
    }

    private fun ActivityFigureEditBinding.init() = apply {
        root.insets().paddings(statusBars() or navigationBars())
        val requestManager = Glide.with(this@FigureEditActivity)

        // 数字人列表
        val figurePager = FigurePager(
            lifecycle = lifecycle,
            viewPager2 = vpFigure,
            requestManager = requestManager,
            goScene = sharedViewModel::submitPendingScene,
            removeFigure = sharedViewModel::submitPendingRemove,
            selectPosition = sharedViewModel::selectPosition,
            figureListFlow = sharedViewModel.figureListFlow
        )

        // 文字输入
        val textPager = TextPager(
            textView = tvFigure,
            goScene = sharedViewModel::submitPendingScene,
        )

        // 覆盖层，包含内容区、编辑区、变换操作
        val overlay: Overlay<FigureScene> = InputView.createOverlay(
            lifecycleOwner = this@FigureEditActivity,
            contentAdapter = FigureContentAdapter(lifecycle, supportFragmentManager),
            editorAdapter = FigureEditAdapter(lifecycle, supportFragmentManager)
        ) {
            addSceneFadeChange()
            add(EditorBackground(0xFF1D1D1D.toInt()))
            add(OverlayBackground(0xFF111113.toInt(), sharedViewModel::submitPendingScene))
            sceneChangedListener = SceneChangedListener { _, c -> sharedViewModel.submitPendingScene(c) }
            addToOnBackPressedDispatcher(onBackPressedDispatcher)
            attach(window) { it.editorAnimator = FadeEditorAnimator(durationMillis = 300) }
        }

        sharedViewModel.figureState.onEach {
            if (it.pendingRemove != null) {
                window.snackbar().setText("已删除").show()
                sharedViewModel.consumePendingRemove()
            }
            if (it.pendingScene != null) {
                overlay.go(it.pendingScene.scene)
                sharedViewModel.consumePendingScene(overlay.current)
            }
            figurePager.updateCurrentPage(it)
            textPager.updateCurrentPage(it)
        }.launchRepeatOnLifecycle(lifecycle)

        // pendingView不结合repeatOnLifecycle收集
        sharedViewModel.figureState
            .mapNotNull { it.pendingView }
            .filterIsInstance<PendingView.Request>()
            .mapLatest {
                val ref = when (it) {
                    PendingView.Request.Figure -> figurePager.awaitFigureView()
                    PendingView.Request.Text -> textPager.textView()
                }
                sharedViewModel.consumePendingView(PendingView.Result(ref))
            }
            .launchIn(lifecycleScope)
    }
}