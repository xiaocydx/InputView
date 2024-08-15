package com.xiaocydx.inputview.sample.scene.figure.content

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.databinding.FigureTextLayoutBinding
import com.xiaocydx.inputview.sample.scene.figure.FigureScene
import com.xiaocydx.inputview.sample.scene.figure.FigureViewModel
import com.xiaocydx.inputview.transform.Overlay

/**
 * @author xcc
 * @date 2024/8/15
 */
class TextFragment : Fragment(), Overlay.Transform {
    private val sharedViewModel: FigureViewModel by activityViewModels()
    private lateinit var binding: FigureTextLayoutBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FigureTextLayoutBinding.inflate(
        inflater, container, false
    ).apply {
        binding = this
        tvEmoji.onClick { sharedViewModel.submitPendingScene(FigureScene.InputEmoji) }
        tvFigure.onClick { sharedViewModel.submitPendingScene(FigureScene.SelectFigure) }
        tvDubbing.onClick { sharedViewModel.submitPendingScene(FigureScene.SelectDubbing) }
        ivConfirm.onClick { sharedViewModel.submitPendingScene(scene = null) }
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    }
}