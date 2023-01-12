package com.xiaocydx.inputview

import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE

/**
 * [InputView]编辑区的[Editor]模式
 *
 * @author xcc
 * @date 2023/1/10
 */
enum class EditorMode {

    /**
     * 显示[Editor]时平移`contentView`，类似[SOFT_INPUT_ADJUST_PAN]
     */
    ADJUST_PAN,

    /**
     * 显示[Editor]时修改`contentView`的尺寸，类似[SOFT_INPUT_ADJUST_RESIZE]
     */
    ADJUST_RESIZE
}