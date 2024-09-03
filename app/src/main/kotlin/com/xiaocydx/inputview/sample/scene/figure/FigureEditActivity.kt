package com.xiaocydx.inputview.sample.scene.figure

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.xiaocydx.inputview.FadeEditorAnimator
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.Insets.Decor
import com.xiaocydx.inputview.init
import com.xiaocydx.inputview.sample.common.launchRepeatOnLifecycle
import com.xiaocydx.inputview.sample.common.snackbar
import com.xiaocydx.inputview.sample.databinding.ActivityFigureEditBinding
import com.xiaocydx.inputview.sample.scene.figure.PendingView.Request
import com.xiaocydx.inputview.sample.scene.figure.PendingView.Result
import com.xiaocydx.inputview.sample.scene.figure.pager.FigurePager
import com.xiaocydx.inputview.sample.scene.figure.pager.TextPager
import com.xiaocydx.inputview.transform.EditorBackground
import com.xiaocydx.inputview.transform.Overlay
import com.xiaocydx.inputview.transform.SceneChangedListener
import com.xiaocydx.inputview.transform.SceneEditorConverter
import com.xiaocydx.inputview.transform.addSceneFadeChange
import com.xiaocydx.inputview.transform.createOverlay
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.navigationBars
import com.xiaocydx.insets.statusBars
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 通过选中编辑的交互案例，演示[createOverlay]的使用
 *
 * @author xcc
 * @date 2024/4/13
 */
class FigureEditActivity : AppCompatActivity() {
    private val sharedViewModel: FigureViewModel by viewModels()
    private lateinit var figurePager: FigurePager
    private lateinit var textPager: TextPager
    private lateinit var overlay: Overlay<FigureScene>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InputView.init(window, Decor(statusBarEdgeToEdge = true, gestureNavBarEdgeToEdge = true))
        setContentView(ActivityFigureEditBinding.inflate(layoutInflater).init().root)
        configureOverlay()
        launchUpdatePageJob()
    }

    private fun ActivityFigureEditBinding.init() = apply {
        root.insets().paddings(statusBars() or navigationBars())
        figurePager = FigurePager(
            lifecycle = lifecycle,
            viewPager2 = vpFigure,
            requestManager = Glide.with(this@FigureEditActivity),
            go = sharedViewModel::submitScene,
            removeFigure = sharedViewModel::submitRemove,
            selectPosition = sharedViewModel::selectPosition,
            figureListFlow = sharedViewModel.figureListFlow
        )
        textPager = TextPager(
            textView = tvFigure,
            go = sharedViewModel::submitScene,
        )
        // 覆盖层，包含内容区、编辑区、变换操作
        overlay = InputView.createOverlay(
            sceneList = FigureScene.entries,
            lifecycleOwner = this@FigureEditActivity,
            contentAdapter = FigureContentAdapter(lifecycle, supportFragmentManager),
            editorAdapter = FigureEditAdapter(lifecycle, supportFragmentManager),
            editorAnimator = FadeEditorAnimator(durationMillis = 300)
        )
        overlay.attach(window)
    }

    private fun configureOverlay() = with(overlay) {
        addSceneFadeChange() // 添加overlay的透明度变换
        add(EditorBackground(0xFF1D1D1D.toInt())) // 添加overlay的编辑区背景
        add(FigureSceneBackground(0xFF111113.toInt())) // 添加overlay的整体背景
        // overlay调度Transformer期间拦截触摸事件，点击backgroundView退出当前FigureScene
        add(FigureSceneDispatchTouch(window, go = sharedViewModel::submitScene))

        sceneChangedListener = SceneChangedListener { _, current ->
            sharedViewModel.submitScene(current) // 同步当前FigureScene
        }
        sceneEditorConverter = SceneEditorConverter next@{ _, current, nextEditor ->
            // 处于FigureScene.InputText，点击或回退隐藏Ime，转换为FigureScene.InputIdle，表示不退出
            if (current == FigureScene.InputText && nextEditor == null) FigureScene.InputIdle else current
        }

        addToOnBackPressedDispatcher(onBackPressedDispatcher)
    }

    private fun launchUpdatePageJob() {
        var requestJob: Job? = null
        sharedViewModel.figureState.onEach {
            figurePager.updateCurrentPage(it)
            textPager.updateCurrentPage(it)
            if (it.pendingRemove != null) {
                window.snackbar().setText("已删除").show()
                sharedViewModel.consumePendingRemove()
            }
            if (it.pendingScene != null) {
                overlay.go(it.pendingScene.scene)
                sharedViewModel.consumePendingScene(overlay.current)
            }
            if (it.pendingView is Request) {
                requestJob?.cancel()
                requestJob = lifecycleScope.launch {
                    val ref = when (it.pendingView) {
                        Request.Figure -> figurePager.awaitFigureView()
                        Request.Text -> textPager.textView()
                    }
                    sharedViewModel.consumePendingView(Result(ref))
                }
            }
        }.launchRepeatOnLifecycle(lifecycle)
    }
}