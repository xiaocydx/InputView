package com.xiaocydx.inputview.sample.scene.figure

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.sample.common.snackbar
import com.xiaocydx.inputview.sample.databinding.ActivityFigureEditBinding
import com.xiaocydx.inputview.sample.scene.figure.overlay.FigureEditOverlay
import com.xiaocydx.inputview.sample.scene.figure.pager.FigurePager
import com.xiaocydx.inputview.sample.scene.figure.pager.TextPager
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.navigationBars
import com.xiaocydx.insets.statusBars
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest

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
        setContentView(ActivityFigureEditBinding.inflate(layoutInflater).init().root)
    }

    private fun ActivityFigureEditBinding.init() = apply {
        root.insets().paddings(statusBars() or navigationBars())
        val requestManager = Glide.with(this@FigureEditActivity)

        val figurePager = FigurePager(
            lifecycle = lifecycle,
            viewPager2 = vpFigure,
            requestManager = requestManager,
            showEditor = sharedViewModel::submitPendingEditor,
            removeFigure = sharedViewModel::submitPendingRemove,
            selectPosition = sharedViewModel::selectPosition,
            figureListFlow = sharedViewModel.figureListFlow,
            figureState = sharedViewModel.figureState
        ).init()

        val textPager = TextPager(
            lifecycle = lifecycle,
            textView = tvFigure,
            showEditor = sharedViewModel::submitPendingEditor,
            figureState = sharedViewModel.figureState
        ).init()

        FigureEditOverlay(
            context = this@FigureEditActivity,
            lifecycleOwner = this@FigureEditActivity,
            fragmentManager = supportFragmentManager,
            requestManager = requestManager,
            sharedViewModel = sharedViewModel
        ).attachToWindow(window, onBackPressedDispatcher)

        sharedViewModel.figureState
            .filter { it.pendingRemove != null || it.pendingEditor != null }
            .mapLatest {
                if (it.pendingRemove != null) {
                    window.snackbar().setText("已删除").show()
                    sharedViewModel.consumePendingRemove()
                }
                if (it.pendingEditor != null) {
                    var snapshot = figurePager.awaitSnapshot()
                    snapshot = snapshot.merge(textPager.snapshot())
                    sharedViewModel.consumePendingEditor(snapshot)
                }
            }
            .launchIn(lifecycleScope)
    }
}