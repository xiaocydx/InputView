package com.xiaocydx.inputview.sample.edit

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * @author xcc
 * @date 2023/12/1
 */
class VideoEditViewModel : ViewModel() {
    private val _state = MutableStateFlow<VideoEditor?>(null)
    val state = _state.asStateFlow()

    fun show(current: VideoEditor?) {
        _state.value = current
    }
}