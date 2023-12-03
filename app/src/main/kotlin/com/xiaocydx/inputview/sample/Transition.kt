@file:JvmName("TransitionInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.transition

import android.graphics.Rect
import android.view.View

inline fun View.getBounds(bounds: Rect, change: Rect.() -> Unit) {
    bounds.set(left, top, right, bottom)
    bounds.change()
}

fun View.setLeftTopRightBottomCompat(rect: Rect) {
    setLeftTopRightBottomCompat(rect.left, rect.top, rect.right, rect.bottom)
}

fun View.setLeftTopRightBottomCompat(left: Int, top: Int, right: Int, bottom: Int) {
    ViewUtils.setLeftTopRightBottom(this, left, top, right, bottom)
}