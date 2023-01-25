package com.xiaocydx.inputview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.CheckResult
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.Insets
import androidx.core.view.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.xiaocydx.inputview.databinding.MessageListBinding
import com.xiaocydx.sample.suppressLayoutCompat

/**
 * [InputView]的消息列表示例代码
 *
 * **注意**：需要确保`androidx.appcompat`的版本足够高，因为高版本修复了[WindowInsetsCompat]常见的问题，
 * 例如高版本修复了应用退至后台，再重新显示，调用[WindowInsetsControllerCompat.show]显示IME无效的问题。
 *
 * @author xcc
 * @date 2023/1/24
 */
class MessageListDialogFragment : BottomSheetDialogFragment() {

    /**
     * 是否状态栏边到边
     */
    private val statusBarEdgeToEdge = true

    /**
     * `InputView.init()`对`window.decorView`到`fragment.view`之间的View不分发insets，
     * 目的是去除[BottomSheetDialogFragment]的边到边实现，自行实现状态栏和导航栏边到边，
     * 并确保`fragment.view`的子视图树insets分发正常、Android 11以下insets动画回调正常，
     * 自行实现边到边虽然有点麻烦，但是会更加灵活，能满足实际场景不同的需求。
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 1. 初始化InputView所需的配置
        InputView.init(
            dialogFragment = this,
            gestureNavBarEdgeToEdge = true,
            onApplyWindowInsetsListener = listener@{ _, insets ->
                if (!statusBarEdgeToEdge) return@listener insets
                // 消费statusBar的insets，去除顶部状态栏高度的间距
                insets.consumeInsets(WindowInsetsCompat.Type.statusBars())
            }
        )
        return MessageListBinding.inflate(inflater).init(requireDialog().window!!).root
    }

    @CheckResult
    private fun WindowInsetsCompat.consumeInsets(typeMask: Int): WindowInsetsCompat {
        return WindowInsetsCompat.Builder(this).setInsets(typeMask, Insets.NONE).build()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val behavior = view.parent?.let { it as? ViewGroup }
            ?.layoutParams?.let { it as? CoordinatorLayout.LayoutParams }
            ?.let { it.behavior as? BottomSheetBehavior<View> } ?: return

        val binding = MessageListBinding.bind(view)
        val bottomSheet = binding.root.parent as View
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.peekHeight = requireActivity().window.decorView.height
        bottomSheet.doOnPreDraw { it.setBackgroundColor(0xFF8F9AD5.toInt()) }
        if (statusBarEdgeToEdge) {
            behavior.addBottomSheetCallback(StatusBarEdgeToEdgeCallback(dialog!!.window!!, binding))
        }
    }

    private class StatusBarEdgeToEdgeCallback(
        window: Window,
        private val binding: MessageListBinding
    ) : BottomSheetBehavior.BottomSheetCallback() {
        private val controller = WindowInsetsControllerCompat(window, window.decorView)

        init {
            val bottomSheet = binding.root.parent as View
            bottomSheet.doOnAttach(::updatePadding)
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            suppressLayout(newState)
            updatePadding(bottomSheet)
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            updatePadding(bottomSheet)
        }

        /**
         * 手势拖动`Dialog`会改变`binding.root`的高度，进而改变`binding.inputView`的高度，
         * 重新布局`binding.rvMessage`和`binding.inputBar`，让手势拖动看起来像是产生了偏移，
         * 当[newState]是`STATE_DRAGGING`或`STATE_SETTLING`时，抑制`binding.inputView`布局，
         */
        private fun suppressLayout(newState: Int) {
            binding.inputView.suppressLayoutCompat(when (newState) {
                BottomSheetBehavior.STATE_DRAGGING,
                BottomSheetBehavior.STATE_SETTLING -> true
                else -> false
            })
        }

        private fun updatePadding(bottomSheet: View) {
            val rootInsets = ViewCompat.getRootWindowInsets(bottomSheet) ?: return
            val statusBars = rootInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            if (bottomSheet.top < statusBars.top) {
                controller.isAppearanceLightStatusBars = true
                bottomSheet.updatePadding(top = statusBars.top - bottomSheet.top)
            } else {
                controller.isAppearanceLightStatusBars = false
                bottomSheet.updatePadding(top = 0)
            }
        }
    }
}