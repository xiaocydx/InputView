// package com.xiaocydx.inputview.sample.scene.figure.overlay
//
// import android.graphics.Rect
// import android.view.LayoutInflater
// import android.view.View
// import androidx.core.view.isInvisible
// import androidx.core.view.isVisible
// import androidx.core.view.updatePadding
// import androidx.transition.getPaddings
// import androidx.transition.updatePaddings
// import com.xiaocydx.inputview.sample.common.dp
// import com.xiaocydx.inputview.sample.common.onClick
// import com.xiaocydx.inputview.sample.databinding.FigureTextLayoutBinding
// import com.xiaocydx.inputview.sample.scene.figure.FigureEditor
// import com.xiaocydx.inputview.sample.scene.figure.FigureEditor.Emoji
// import com.xiaocydx.inputview.sample.scene.figure.FigureEditor.FigureDubbing
// import com.xiaocydx.inputview.sample.scene.figure.FigureEditor.FigureGrid
// import com.xiaocydx.inputview.sample.scene.figure.FigureEditor.Ime
// import com.xiaocydx.inputview.sample.scene.figure.FigureEditor.ImeIdle
// import com.xiaocydx.inputview.sample.scene.figure.ViewBounds
// import com.xiaocydx.inputview.sample.scene.transform.ContainerTransformation
// import com.xiaocydx.insets.getRootWindowInsetsCompat
// import com.xiaocydx.insets.isGestureNavigationBar
// import com.xiaocydx.insets.navigationBarHeight
// import com.xiaocydx.insets.statusBarHeight
// import com.xiaocydx.insets.updateLayoutSize
// import com.xiaocydx.insets.updateMargins
// import kotlin.math.absoluteValue
//
// /**
//  * @author xcc
//  * @date 2024/4/14
//  */
// class TextGroupTransformation(
//     private val showEditor: (FigureEditor?) -> Unit,
//     private val currentText: () -> String,
//     private val confirmText: (text: String) -> Unit
// ) : ContainerTransformation<FigureSnapshotState>(Ime, ImeIdle, Emoji) {
//     private var binding: FigureTextLayoutBinding? = null
//     private var textBounds: ViewBounds? = null
//     private val startPaddings = Rect()
//     private val endPaddings = Rect()
//     private val initialToolsHeight = 44.dp
//     private var startToolsHeight = 0
//     private var endToolsHeight = 0
//     private var navigationBarHeight = 0
//     private var isGestureNavigationBar = false
//
//     override fun getView(state: FigureSnapshotState): View {
//         return binding?.root ?: FigureTextLayoutBinding.inflate(
//             LayoutInflater.from(state.container.context),
//             state.container,
//             false
//         ).apply {
//             binding = this
//             tvEmoji.onClick { showEditor(Emoji) }
//             tvFigure.onClick { showEditor(FigureGrid) }
//             tvDubbing.onClick { showEditor(FigureDubbing) }
//             ivConfirm.onClick { showEditor(null) }
//         }.root
//     }
//
//     override fun onPrepare(state: FigureSnapshotState) = with(state) {
//         val binding = binding ?: return
//         val bounds = snapshot.textView?.get()?.let(ViewBounds::from)
//         textBounds = bounds
//         if (bounds == null) {
//             binding.root.isInvisible = true
//             return
//         }
//         binding.root.isVisible = true
//         if (previous == null) {
//             startPaddings.set(bounds)
//             startPaddings.right = container.width - bounds.right
//             startPaddings.bottom = container.height - bounds.bottom
//             startToolsHeight = 0
//         } else {
//             binding.root.getPaddings(startPaddings)
//             startToolsHeight = initialToolsHeight
//         }
//         binding.root.updatePaddings(startPaddings)
//         binding.llTools.updateLayoutSize(height = startToolsHeight)
//         when {
//             !isPrevious(state) -> {
//                 inputView.editText = binding.editText
//                 if (current == Ime) {
//                     inputView.editText?.requestFocus()
//                     binding.editText.setText(currentText())
//                     binding.editText.setSelection(binding.editText.text.length)
//                 }
//             }
//             !isCurrent(state) -> {
//                 inputView.editText = null
//                 confirmText(binding.editText.text.toString())
//             }
//         }
//     }
//
//     override fun onStart(state: FigureSnapshotState) = with(state) {
//         val bounds = textBounds ?: return@with
//         val insets = container.getRootWindowInsetsCompat()
//         if (current == null) {
//             endPaddings.set(bounds)
//             endPaddings.right = container.width - bounds.right
//             endPaddings.bottom = container.height - bounds.bottom
//             endToolsHeight = 0
//         } else {
//             val top = insets?.statusBarHeight ?: 0
//             val bottom = (endAnchorY - initialAnchorY).absoluteValue
//             endPaddings.set(0, top, 0, bottom)
//             endToolsHeight = initialToolsHeight
//         }
//         navigationBarHeight = insets?.navigationBarHeight ?: 0
//         isGestureNavigationBar = insets?.isGestureNavigationBar(container) ?: false
//         snapshot.textView?.get()?.alpha = 0f
//     }
//
//     override fun onUpdate(state: FigureSnapshotState) = with(state) {
//         val binding = binding ?: return
//         val fraction = interpolatedFraction
//         binding.root.updatePadding(
//             left = calculatePadding(startPaddings.left, endPaddings.left, fraction),
//             top = calculatePadding(startPaddings.top, endPaddings.top, fraction),
//             right = calculatePadding(startPaddings.right, endPaddings.right, fraction),
//             bottom = calculatePadding(startPaddings.bottom, endPaddings.bottom, fraction)
//         )
//         binding.llTools.alpha = when {
//             previous != null && current != null -> 1f
//             current != null -> interpolatedFraction
//             else -> 1f - interpolatedFraction
//         }
//         val toolsHeight = startToolsHeight + (endToolsHeight - startToolsHeight) * fraction
//         binding.llTools.updateLayoutSize(height = toolsHeight.toInt())
//         binding.llTools.updateMargins(bottom = calculateToolsMarginBottom(state))
//     }
//
//     override fun onEnd(state: FigureSnapshotState) {
//         state.snapshot.textView?.get()?.alpha = 1f
//     }
//
//     private fun calculatePadding(start: Int, end: Int, fraction: Float): Int {
//         return (start + (end - start) * fraction).toInt()
//     }
//
//     private fun calculateToolsMarginBottom(state: FigureSnapshotState) = with(state) {
//         val fraction = when {
//             navigationBarHeight <= 0 || !isGestureNavigationBar -> 0f
//             // 退出INPUT_IDLE，按interpolatedFraction减小marginBottom
//             previous === ImeIdle && current == null -> 1 - interpolatedFraction
//             // currentAnchorY处于navigationBar的范围，计算当前marginBottom
//             currentAnchorY in (initialAnchorY - navigationBarHeight)..initialAnchorY -> {
//                 val current = currentAnchorY - (initialAnchorY - navigationBarHeight)
//                 current.toFloat() / navigationBarHeight
//             }
//             else -> 0f
//         }
//         (navigationBarHeight * fraction).toInt()
//     }
//
//     private fun Rect.set(bounds: ViewBounds) {
//         bounds.apply { set(left, top, right, bottom) }
//     }
// }