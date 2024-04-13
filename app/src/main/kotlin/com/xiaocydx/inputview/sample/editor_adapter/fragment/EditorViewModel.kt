package com.xiaocydx.inputview.sample.editor_adapter.fragment

import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel

/**
 * @author xcc
 * @date 2023/10/12
 */
class EditorViewModel : ViewModel() {
    var count = 0
        private set
    val viewId = ViewCompat.generateViewId()

    fun increase() {
        count++
    }
}