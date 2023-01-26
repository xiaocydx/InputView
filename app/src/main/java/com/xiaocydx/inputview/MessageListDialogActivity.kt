package com.xiaocydx.inputview

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialog
import androidx.core.view.*
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.xiaocydx.inputview.databinding.ActivityMessageListDialogBinding
import com.xiaocydx.inputview.databinding.MessageListBinding
import com.xiaocydx.sample.onClick
import com.xiaocydx.sample.suppressLayoutCompat

/**
 * [InputView]的消息列表Dialog示例代码
 *
 * **注意**：需要确保`androidx.appcompat`的版本足够高，因为高版本修复了[WindowInsetsCompat]常见的问题，
 * 例如高版本修复了应用退至后台，再重新显示，调用[WindowInsetsControllerCompat.show]显示IME无效的问题。
 *
 * @author xcc
 * @date 2023/1/26
 */
class MessageListDialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMessageListDialogBinding.inflate(layoutInflater)
        binding.apply {
            val context = root.context
            btnDialog.onClick { MessageListDialog(context).show() }
            btnDialogFragment.onClick {
                MessageListDialogFragment().show(supportFragmentManager, null)
            }
            btnBottomSheetDialog.onClick {
                MessageListBottomSheetDialog(context).show()
            }
            btnBottomSheetDialogFragment.onClick {
                MessageListBottomSheetDialogFragment().show(supportFragmentManager, null)
            }
        }
        setContentView(binding.root)
    }
}

private const val statusBarEdgeToEdge = true
private const val gestureNavBarEdgeToEdge = true

/**
 * [R.style.MessageListDialog]最重要的是`windowIsFloating = false`，这能让Dialog的视图树自行处理insets，
 * 例如[BottomSheetDialog]的默认主题包含`windowIsFloating = false`，布局文件是`design_bottom_sheet_dialog.xml`
 * id为`coordinator`和`design_bottom_sheet`的View实现边到边，id为`touch_outside`的View实现`canceledOnTouchOutside`。
 */
class MessageListDialog(
    context: Context,
    @StyleRes theme: Int = R.style.MessageListDialog
) : AppCompatDialog(context, theme) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val window = window!!
        val binding = MessageListBinding.inflate(layoutInflater)

        // 1. 初始化InputView所需的配置
        InputView.init(
            window = window,
            statusBarEdgeToEdge = statusBarEdgeToEdge,
            gestureNavBarEdgeToEdge = gestureNavBarEdgeToEdge
        )

        binding.tvTitle.apply {
            setBackgroundColor(0xFFD5A7AE.toInt())
            if (!statusBarEdgeToEdge) return@apply
            doOnAttach {
                val rootInsets = ViewCompat.getRootWindowInsets(it) ?: return@doOnAttach
                val statusBars = rootInsets.getInsets(WindowInsetsCompat.Type.statusBars())
                updatePadding(top = statusBars.top)
                updateLayoutParams { height += statusBars.top }
            }
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        }

        setContentView(binding.init(window).root)
    }
}

class MessageListDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MessageListDialog(requireContext(), R.style.MessageListDialog)
    }
}

class MessageListBottomSheetDialog(
    context: Context,
    @StyleRes theme: Int = 0
) : BottomSheetDialog(context, theme) {

    /**
     * `InputView.init()`不对`decorView`到`binding.root`之间的View分发insets，
     * 作用是去除[BottomSheetDialog]的边到边实现，自行实现状态栏和导航栏边到边，
     * 以及确保`binding.root`的insets分发正常和Android 11以下insets动画回调正常，
     * 自行实现边到边虽然有点麻烦，但是会更加灵活，能满足实际场景不同的需求。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val window = window!!
        val binding = MessageListBinding.inflate(layoutInflater)

        // 1. 初始化InputView所需的配置
        InputView.init(
            window = window,
            statusBarEdgeToEdge = statusBarEdgeToEdge,
            gestureNavBarEdgeToEdge = gestureNavBarEdgeToEdge,
            dispatchApplyWindowInsetsRoot = binding.root
        )

        binding.root.doOnLayout {
            val bottomSheet = binding.root.parent as View
            behavior.peekHeight = bottomSheet.height
            bottomSheet.setBackgroundColor(0xFF8F9AD5.toInt())
        }
        if (statusBarEdgeToEdge) {
            behavior.addBottomSheetCallback(StatusBarEdgeToEdgeCallback(window, binding))
        }

        setContentView(binding.init(window).root)
    }

    private class StatusBarEdgeToEdgeCallback(
        window: Window,
        private val binding: MessageListBinding
    ) : BottomSheetBehavior.BottomSheetCallback() {
        private val controller = WindowInsetsControllerCompat(window, window.decorView)

        init {
            binding.root.doOnAttach { (it.parent as View).let(::updatePadding) }
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

class MessageListBottomSheetDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MessageListBottomSheetDialog(requireContext(), theme)
    }
}