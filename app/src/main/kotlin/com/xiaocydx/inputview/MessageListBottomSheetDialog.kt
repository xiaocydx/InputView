package com.xiaocydx.inputview

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowInsets
import androidx.annotation.StyleRes
import androidx.core.graphics.Insets
import androidx.core.view.*
import com.google.android.material.bottomsheet.*
import com.xiaocydx.inputview.databinding.MessageListBinding
import com.xiaocydx.sample.suppressLayoutCompat

private const val statusBarEdgeToEdge = true
private const val gestureNavBarEdgeToEdge = true

/**
 * 视图初始化阶段调用[disableEdgeToEdgeAndFitsSystemWindows]，
 * 禁用[BottomSheetDialog]的EdgeToEdge和FitsSystemWindows，
 * 自行处理[WindowInsets]和实现EdgeToEdge。
 *
 * 这种处理方式更为灵活和自由，不需要兼容[BottomSheetDialog]的实现逻辑。
 */
class MessageListBottomSheetDialog(
    context: Context,
    @StyleRes theme: Int = 0
) : BottomSheetDialog(context, theme) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disableEdgeToEdgeAndFitsSystemWindows()

        val window = window!!
        val binding = MessageListBinding.inflate(layoutInflater)

        // 1. 初始化InputView所需的配置
        InputView.init(window, statusBarEdgeToEdge, gestureNavBarEdgeToEdge)

        val color = 0xFF8F9AD5.toInt()
        binding.tvTitle.setBackgroundColor(color)
        binding.root.doOnLayout {
            val bottomSheet = binding.root.parent as View
            behavior.peekHeight = bottomSheet.height
            if (!statusBarEdgeToEdge) bottomSheet.background = null
        }
        if (statusBarEdgeToEdge) {
            StatusBarEdgeToEdgeCallback(window, color, binding).attach(behavior)
        }

        setContentView(binding.init(window).root)
    }
}

class MessageListBottomSheetDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MessageListBottomSheetDialog(requireContext(), theme)
    }
}

private class StatusBarEdgeToEdgeCallback(
    window: Window,
    private val color: Int,
    private val binding: MessageListBinding
) : OnApplyWindowInsetsListener, SimpleBottomSheetCallback() {
    private var lastStatusBars = Insets.NONE
    private var background: Drawable? = null
    private val controller = WindowInsetsControllerCompat(window, window.decorView)
    private var WindowInsetsControllerCompat.isDarkStyleStatusBars: Boolean
        get() = isAppearanceLightStatusBars
        set(value) {
            if (isAppearanceLightStatusBars == value) return
            isAppearanceLightStatusBars = value
        }

    init {
        controller.isDarkStyleStatusBars = true
    }

    fun attach(behavior: BottomSheetBehavior<*>) {
        behavior.addBottomSheetCallback(this)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, this)
    }

    override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        lastStatusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
        return insets
    }

    override fun onStateChanged(bottomSheet: View, newState: Int) {
        suppressLayout(newState)
        updatePadding(bottomSheet)
    }

    override fun onSlide(bottomSheet: View, slideOffset: Float) {
        updatePadding(bottomSheet)
    }

    override fun onLayout(bottomSheet: View) {
        updatePadding(bottomSheet)
        setupBackground(bottomSheet)
    }

    /**
     * 用[ColorDrawable]替换[bottomSheet]的默认`background`，并减小过度绘制范围
     */
    private fun setupBackground(bottomSheet: View) {
        if (background == null) {
            background = object : ColorDrawable(color) {
                override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
                    val finalBottom = bottom.coerceAtMost(lastStatusBars.top)
                    super.setBounds(left, top, right, finalBottom)
                }
            }
        }
        if (bottomSheet.background !== background) {
            bottomSheet.background = background
        }
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
        if (bottomSheet.top < lastStatusBars.top) {
            controller.isDarkStyleStatusBars = true
            bottomSheet.updatePadding(top = lastStatusBars.top - bottomSheet.top)
        } else {
            controller.isDarkStyleStatusBars = false
            bottomSheet.updatePadding(top = 0)
        }
    }
}