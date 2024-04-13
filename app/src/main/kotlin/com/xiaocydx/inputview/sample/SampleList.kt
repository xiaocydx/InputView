@file:Suppress("MayBeConstant")

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
import com.xiaocydx.inputview.sample.editor_adapter.StatefulActivity
import com.xiaocydx.inputview.sample.editor_adapter.fragment.FragmentEditorAdapterActivity
import com.xiaocydx.inputview.sample.editor_animator.AnimationInterceptorActivity
import com.xiaocydx.inputview.sample.editor_animator.ImeAnimatorActivity
import com.xiaocydx.inputview.sample.scene.videoedit.VideoEditActivity
import kotlin.reflect.KClass

/**
 * @author xcc
 * @date 2024/4/9
 */
class SampleList {
    private val selectedId = R.drawable.ic_sample_selected
    private val unselectedId = R.drawable.ic_sample_unselected
    private val source = listOf(
        basicList(), animatorList(),
        adapterList(), sceneList()
    ).flatten().toMutableList()

    @CheckResult
    fun toggle(category: Category): List<SampleItem> {
        val position = source.indexOf(category)
        val isSelected = !category.isSelected()
        val selectedResId = if (isSelected) selectedId else unselectedId
        source[position] = category.copy(selectedResId = selectedResId)
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

    private fun Category.isSelected(): Boolean {
        return selectedResId == selectedId
    }

    private fun basicList() = listOf(
        Category(title = "Basic", selectedResId = unselectedId),
        StartActivity(
            title = "Activity",
            desc = "消息列表Activity",
            clazz = MessageListActivity::class
        ),
        ShowDialog(
            title = "Dialog",
            desc = "消息列表Dialog，主题包含windowIsFloating = false",
            create = ::MessageListDialog
        ),
        ShowDialog(
            title = "BottomSheetDialog",
            desc = "消息列表BottomSheetDialog，实现状态栏Edge-to-Edge",
            create = ::MessageListBottomSheetDialog
        ),
        StartActivity(
            title = "InitCompat",
            desc = "初始化Window，兼容已有的WindowInsets处理方案",
            clazz = InitCompatActivity::class
        )
    )

    private fun animatorList() = listOf(
        Category(title = "EditorAnimator", selectedResId = unselectedId),
        StartActivity(
            title = "AnimationInterceptor",
            desc = "处理多Window的交互冲突问题",
            clazz = AnimationInterceptorActivity::class
        ),
        StartActivity(
            title = "ImeAnimator",
            desc = "脱离InputView使用EditorAnimator",
            clazz = ImeAnimatorActivity::class
        )
    )

    private fun adapterList() = listOf(
        Category(title = "EditorAdapter", selectedResId = unselectedId),
        StartActivity(
            title = "EditorAdapter-Stateful",
            desc = """
                |页面重建时（因Activity配置更改或进程被杀掉）：
                |1. 不运行动画（IME除外），恢复之前显示的Editor。
                |2. 不恢复Editor视图的状态，该功能由FragmentEditorAdapter完成。
            """.trimMargin(),
            StatefulActivity::class
        ),
        StartActivity(
            title = "FragmentEditorAdapter",
            desc = """
                |1. 动画结束时，Lifecycle的状态才会转换为RESUMED。
                |2. 页面重建时，使用可恢复状态的Fragment，不会调用函数再次创建Fragment。
                |3. 页面重建时，Stateful恢复之前显示的Editor，Fragment恢复Editor视图的状态。
            """.trimMargin(),
            clazz = FragmentEditorAdapterActivity::class
        )
    )

    private fun sceneList() = listOf(
        Category(title = "Scene", selectedResId = unselectedId),
        StartActivity(
            title = "VideoEdit",
            desc = "剪辑类的交互场景",
            clazz = VideoEditActivity::class
        )
    )
}

sealed class SampleItem {
    data class Category(val title: String, val selectedResId: Int) : SampleItem()
    sealed class Element(open val title: String, open val desc: String) : SampleItem() {
        abstract fun perform(context: Context)
    }
}

private data class StartActivity(
    override val title: String,
    override val desc: String,
    val clazz: KClass<out Activity>
) : Element(title, desc) {
    override fun perform(context: Context) {
        context.startActivity(Intent(context, clazz.java))
    }
}

private data class ShowDialog(
    override val title: String,
    override val desc: String,
    val create: (context: Context) -> Dialog
) : Element(title, desc) {
    override fun perform(context: Context) {
        create(context).show()
    }
}