package com.xiaocydx.inputview.sample.scene.figure.content

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.common.viewLifecycleScope
import com.xiaocydx.inputview.sample.databinding.FragmentTextBinding
import com.xiaocydx.inputview.sample.scene.figure.FigureContent.Text
import com.xiaocydx.inputview.sample.scene.figure.FigureScene
import com.xiaocydx.inputview.sample.scene.figure.FigureViewModel
import com.xiaocydx.inputview.sample.scene.figure.PendingView.Request
import com.xiaocydx.inputview.transform.ContentChangeEditText
import com.xiaocydx.inputview.transform.Overlay
import com.xiaocydx.insets.doOnApplyWindowInsets
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.lang.ref.WeakReference

/**
 * @author xcc
 * @date 2024/8/15
 */
class TextFragment : Fragment(), Overlay.Transform {
    private val sharedViewModel: FigureViewModel by activityViewModels()
    private lateinit var binding: FragmentTextBinding
    private var textTarget: WeakReference<View>? = null
    private var lastInsets = WindowInsetsCompat.CONSUMED

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentTextBinding.inflate(
        inflater, container, false
    ).apply {
        binding = this
        root.transform().add(TextEnterReturn(binding, ::textTarget, ::lastInsets))
        root.transform().add(TextChangePaddings(binding, ::lastInsets))
        root.transform().add(ContentChangeEditText(editText, Text))
        root.doOnApplyWindowInsets { _, insets, _ -> lastInsets = insets }

        tvEmoji.onClick { sharedViewModel.submitScene(FigureScene.InputEmoji) }
        tvFigure.onClick { sharedViewModel.submitScene(FigureScene.SelectFigure) }
        tvDubbing.onClick { sharedViewModel.submitScene(FigureScene.SelectDubbing) }
        ivConfirm.onClick { sharedViewModel.submitScene(scene = null) }
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        launchUpdateTextBoundsJob()
        launchInitAndConfirmTextJob()
    }

    private fun launchUpdateTextBoundsJob() {
        sharedViewModel.currentSceneFlow()
            .filter { it?.content == Text }
            .onEach {
                // Content更改为Text，设置textTarget
                val target = sharedViewModel.requestView(Request.Text)
                textTarget = target?.let(::WeakReference)
            }
            .launchIn(viewLifecycleScope)
    }

    private fun launchInitAndConfirmTextJob() = with(binding) {
        sharedViewModel.currentSceneFlow()
            .map { it?.content == Text }
            .distinctUntilChanged()
            .onEach { isText ->
                if (isText) {
                    // Content更改为Text，设置最新文案和光标
                    editText.setText(sharedViewModel.figureState.value.currentText)
                    editText.setSelection(editText.text.length)
                } else {
                    // Content不是Text，保存当前文案
                    sharedViewModel.confirmText(editText.text.toString())
                }
            }
            .launchIn(viewLifecycleScope)
    }
}