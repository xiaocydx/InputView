package com.xiaocydx.inputview.sample.scene.figure.overlay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.xiaocydx.inputview.sample.databinding.FragmentDubbingBinding
import com.xiaocydx.inputview.sample.common.onClick
import com.xiaocydx.inputview.sample.scene.figure.FigureViewModel

/**
 * @author xcc
 * @date 2024/4/13
 */
class DubbingFragment : Fragment() {
    private lateinit var binding: FragmentDubbingBinding
    private val sharedViewModel: FigureViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentDubbingBinding.inflate(
        layoutInflater, container, false
    ).apply {
        binding = this
        tvFigureGrid.onClick { sharedViewModel.submitPendingEditor(FigureEditor.GRID) }
    }.root
}