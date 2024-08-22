package com.xiaocydx.inputview.sample.scene.viewpager2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.inputview.InputView
import com.xiaocydx.inputview.Insets.Decor
import com.xiaocydx.inputview.init
import com.xiaocydx.insets.doOnApplyWindowInsets
import com.xiaocydx.insets.ime

/**
 * 通过ViewPager2的交互案例，演示[InputView]的使用
 *
 * @author xcc
 * @date 2024/5/27
 */
class ViewPager2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InputView.init(window, Decor(gestureNavBarEdgeToEdge = true))
        val viewPager2 = ViewPager2(this)
        viewPager2.offscreenPageLimit = 1
        viewPager2.adapter = MessageListPageAdapter(this)
        viewPager2.doOnApplyWindowInsets { _, insets, _ ->
            // 显示IME时不允许手势滑动ViewPager2
            viewPager2.isUserInputEnabled = !insets.isVisible(ime())
        }
        setContentView(viewPager2)
    }

    private class MessageListPageAdapter(
        fragmentActivity: FragmentActivity
    ) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount() = 6

        override fun createFragment(position: Int): Fragment {
            return MessageListFragment.newInstance(num = position + 1)
        }
    }
}