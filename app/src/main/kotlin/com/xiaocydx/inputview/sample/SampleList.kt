@file:Suppress("MayBeConstant", "FunctionName", "SpellCheckingInspection")

package com.xiaocydx.inputview.sample

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import androidx.annotation.CheckResult
import com.xiaocydx.inputview.sample.SampleItem.Category
import com.xiaocydx.inputview.sample.SampleItem.Element
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
import kotlin.reflect.KClass

/**
 * @author xcc
 * @date 2024/4/9
 */
class SampleList {
    private val selectedId = R.drawable.ic_sample_selected
    private val unselectedId = R.drawable.ic_sample_unselected
    private val source = listOf(
        Basic(),
        EditorAnimator(),
        EditorAdapter(),
        `inputview-transform`()
    ).flatten().toMutableList()

    @CheckResult
    fun toggle(category: Category): List<SampleItem> {
        val position = source.indexOf(category)
        val isSelected = !category.isSelected()
        val selectedResId = if (isSelected) selectedId else unselectedId
        source[position] = category.copy(resId = selectedResId)
        return filter()
    }

    @CheckResult
    fun filter(): List<SampleItem> {
        val outcome = mutableListOf<SampleItem>()
        var isSelected = false
        source.forEach {
            when {
                it is Category -> {
                    isSelected = it.isSelected()
                    outcome.add(it)
                }
                it is Element && isSelected -> outcome.add(it)
            }
        }
        return outcome
    }

    @CheckResult
    fun categoryPayload(oldItem: Category, newItem: Category): Any? {
        return if (oldItem.isSelected() != newItem.isSelected()) "change" else null
    }

    private fun Category.isSelected() = resId == selectedId

    private fun Basic() = listOf(
        "Basic" and unselectedId,
        "Activity" and "消息列表Activity" to MessageListActivity::class,
        "Dialog" and "消息列表Dialog，主题包含windowIsFloating = false" show ::MessageListDialog,
        "BottomSheetDialog" and "消息列表BottomSheetDialog，实现状态栏Edge-to-Edge" show ::MessageListBottomSheetDialog,
        "ViewPager2" and "ViewPager2中使用InputView" to ViewPager2Activity::class,
        "InitCompat" and "兼容已有的WindowInsets处理方案" to InitCompatActivity::class,
    )

    private fun EditorAnimator() = listOf(
        "EditorAnimator" and unselectedId,
        "AnimationInterceptor" and "处理多Window的交互冲突问题" to AnimationInterceptorActivity1::class,
        "AnimationInterceptor" and "实现动画时长和插值器的差异化" to AnimationInterceptorActivity2::class,
        "ImeAnimator" and "脱离InputView使用EditorAnimator" to ImeAnimatorActivity::class
    )

    private fun EditorAdapter() = listOf(
        "EditorAdapter" and unselectedId,
        "EditorAdapter-Stateful"
                and """
                页面重建时（因Activity配置更改或进程被杀掉）：
                1. 不运行动画（IME除外），恢复之前显示的Editor。
                2. 不恢复Editor视图的状态，该功能由FragmentEditorAdapter完成。
                """.trimIndent()
                to StatefulActivity::class,
        "FragmentEditorAdapter"
                and """
                1. 动画结束时，Lifecycle的状态才会转换为RESUMED。
                2. 页面重建时，使用可恢复状态的Fragment，不会调用函数再次创建Fragment。
                3. 页面重建时，Stateful恢复之前显示的Editor，Fragment恢复Editor视图的状态。
                """.trimIndent()
                to FragmentEditorAdapterActivity::class
    )

    private fun `inputview-transform`() = listOf(
        "inputview-transform" and unselectedId,
        "PreviewEdit" and "预览编辑的交互案例" to VideoEditActivity::class,
        "SelectEdit" and "选中编辑的交互案例" to FigureEditActivity::class
    )
}

sealed class SampleItem {
    data class Category(val title: String, val resId: Int) : SampleItem()

    data class Element(
        val title: String,
        val desc: String,
        private val createDialog: ((Context) -> Dialog)? = null,
        private val activityClass: KClass<out Activity>? = null
    ) : SampleItem() {
        fun perform(context: Context) {
            when {
                createDialog != null -> createDialog.invoke(context).show()
                activityClass != null -> context.startActivity(Intent(context, activityClass.java))
            }
        }
    }
}

private infix fun String.and(resId: Int) = Category(title = this, resId = resId)

private infix fun String.and(desc: String) = Element(title = this, desc = desc)

private infix fun Element.show(crate: (Context) -> Dialog) = copy(createDialog = crate)

private infix fun Element.to(clazz: KClass<out Activity>) = copy(activityClass = clazz)