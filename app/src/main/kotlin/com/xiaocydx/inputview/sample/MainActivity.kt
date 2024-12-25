package com.xiaocydx.inputview.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.xiaocydx.inputview.sample.basic.InitCompatActivity
import com.xiaocydx.inputview.sample.basic.dialog.MessageListBottomSheetDialog
import com.xiaocydx.inputview.sample.basic.dialog.MessageListDialog
import com.xiaocydx.inputview.sample.basic.message.MessageListActivity
import com.xiaocydx.inputview.sample.basic.viewpager2.ViewPager2Activity
import com.xiaocydx.inputview.sample.editor_adapter.StatefulActivity
import com.xiaocydx.inputview.sample.editor_adapter.fragment.FragmentEditorAdapterActivity
import com.xiaocydx.inputview.sample.editor_animator.AnimationInterceptorActivity1
import com.xiaocydx.inputview.sample.editor_animator.AnimationInterceptorActivity2
import com.xiaocydx.inputview.sample.editor_animator.ImeAnimatorActivity
import com.xiaocydx.inputview.sample.transform.figure.FigureEditActivity
import com.xiaocydx.inputview.sample.transform.video.VideoEditActivity

/**
 * **注意**：需要确保`androidx.core`的版本足够高，因为高版本修复了[WindowInsetsCompat]一些常见的问题，
 * 例如高版本修复了应用退至后台，再重新显示，调用[WindowInsetsControllerCompat.show]显示IME无效的问题。
 *
 * @author xcc
 * @date 2023/4/26
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sample = Sample(source(), this)
        setContentView(sample.contentView())
    }

    private fun source() = listOf(
        "Basic".elements(
            "Activity" desc "消息列表Activity" start MessageListActivity::class,
            "Dialog" desc "消息列表Dialog，主题包含windowIsFloating = false" show ::MessageListDialog,
            "BottomSheetDialog" desc "消息列表BottomSheetDialog，实现状态栏Edge-to-Edge" show ::MessageListBottomSheetDialog,
            "ViewPager2" desc "ViewPager2中使用InputView" start ViewPager2Activity::class,
            "InitCompat" desc "初始化Window，兼容已有的WindowInsets处理方案" start InitCompatActivity::class
        ),

        "EditorAnimator".elements(
            "AnimationInterceptor1" desc "处理多Window的交互冲突问题" start AnimationInterceptorActivity1::class,
            "AnimationInterceptor2" desc "实现动画时长和插值器的差异化" start AnimationInterceptorActivity2::class,
            "ImeAnimator" desc "脱离InputView使用EditorAnimator" start ImeAnimatorActivity::class
        ),

        "EditorAdapter".elements(
            "EditorAdapter-Stateful" desc """
                页面重建时（因Activity配置更改或进程被杀掉）：
                1. 不运行动画（IME除外），恢复之前显示的Editor。
                2. 不恢复Editor视图的状态，该功能由FragmentEditorAdapter完成。
                """.trimIndent() start StatefulActivity::class,
            "FragmentEditorAdapter" desc """
                1. 动画结束时，Lifecycle的状态才会转换为RESUMED。
                2. 页面重建时，使用可恢复状态的Fragment，不会调用函数再次创建Fragment。
                3. 页面重建时，Stateful恢复之前显示的Editor，Fragment恢复Editor视图的状态。
                """.trimIndent() start FragmentEditorAdapterActivity::class
        ),

        "inputview-transform".elements(
            "PreviewEdit" desc "预览编辑的交互案例" start VideoEditActivity::class,
            "SelectEdit" desc "选中编辑的交互案例" start FigureEditActivity::class
        )
    )
}