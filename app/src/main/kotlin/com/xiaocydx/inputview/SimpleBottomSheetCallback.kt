@file:Suppress("PackageDirectoryMismatch")

package com.google.android.material.bottomsheet

import android.view.View

/**
 * @author xcc
 * @date 2023/1/30
 */
abstract class SimpleBottomSheetCallback : BottomSheetBehavior.BottomSheetCallback() {
    override fun onStateChanged(bottomSheet: View, newState: Int) = Unit

    override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit

    public override fun onLayout(bottomSheet: View) = Unit
}