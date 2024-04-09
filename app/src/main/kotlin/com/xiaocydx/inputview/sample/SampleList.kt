@file:Suppress("MayBeConstant")

package com.xiaocydx.inputview.sample

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import androidx.annotation.CheckResult
import androidx.fragment.app.FragmentActivity
import com.xiaocydx.inputview.sample.SampleItem.Category
import com.xiaocydx.inputview.sample.SampleItem.Element
import com.xiaocydx.inputview.sample.dialog.MessageListBottomSheetDialog
import com.xiaocydx.inputview.sample.dialog.MessageListDialog
import com.xiaocydx.inputview.sample.edit.VideoEditActivity
import com.xiaocydx.inputview.sample.fragment.FragmentEditorAdapterActivity
import kotlin.reflect.KClass

/**
 * @author xcc
 * @date 2024/4/9
 */
class SampleList {
    private val selectedId = R.drawable.ic_sample_selected
    private val unselectedId = R.drawable.ic_sample_unselected
    private val source = listOf(
        basicList(), dialogList(),
        adapterList(), animatorList(),
        statefulList(), complexList()
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
            title = "Basic",
            desc = "InputView的基础用法",
            clazz = MessageListActivity::class
        ),
        StartActivity(
            title = "MessageListActivity",
            desc = "消息列表Activity",
            clazz = MessageListActivity::class
        ),
        StartActivity(
            title = "InitCompat",
            desc = "兼容已有的WindowInsets处理方案",
            clazz = InitCompatActivity::class
        )
    )

    private fun adapterList() = listOf(
        Category(title = "EditorAdapter", selectedResId = unselectedId),
        StartActivity(
            title = "FragmentEditorAdapter",
            desc = "Editor的Fragment适配器",
            clazz = FragmentEditorAdapterActivity::class
        )
    )

    private fun animatorList() = listOf(
        Category(title = "EditorAnimator", selectedResId = unselectedId),
        StartActivity(
            title = "AnimationInterceptor",
            desc = "处理多Window的交互冲突问题",
            clazz = OverlayInputActivity::class
        ),
        StartActivity(
            title = "ImeAnimator",
            desc = "脱离InputView使用EditorAnimator",
            clazz = ImeAnimatorActivity::class
        )
    )

    private fun dialogList() = listOf(
        Category(title = "Dialog", selectedResId = unselectedId),
        ShowDialog(
            title = "Dialog",
            desc = "消息列表Dialog，主题包含windowIsFloating = false",
            create = ::MessageListDialog
        ),
        ShowDialog(
            title = "BottomSheetDialog",
            desc = "消息列表BottomSheetDialog，实现状态栏Edge-to-Edge",
            create = ::MessageListBottomSheetDialog
        )
    )

    private fun statefulList() = listOf(
        Category(title = "Stateful", selectedResId = unselectedId),
        StartActivity(title = "Stateful标题1", desc = "描述1", MessageListActivity::class),
        StartActivity(title = "Stateful标题2", desc = "描述2", MessageListActivity::class),
        StartActivity(title = "Stateful标题3", desc = "描述3", MessageListActivity::class)
    )

    private fun complexList() = listOf(
        Category(title = "Scene", selectedResId = unselectedId),
        StartActivity(title = "VideoEdit", desc = "剪辑类的交互场景", VideoEditActivity::class)
    )
}

sealed class SampleItem {
    data class Category(val title: String, val selectedResId: Int) : SampleItem()
    sealed class Element(open val title: String, open val desc: String) : SampleItem() {
        abstract fun perform(activity: FragmentActivity)
    }
}

data class StartActivity(
    override val title: String,
    override val desc: String,
    val clazz: KClass<out Activity>
) : Element(title, desc) {

    override fun perform(activity: FragmentActivity) {
        activity.startActivity(Intent(activity, clazz.java))
    }
}

data class ShowDialog(
    override val title: String,
    override val desc: String,
    val create: (context: Context) -> Dialog
) : Element(title, desc) {

    override fun perform(activity: FragmentActivity) {
        create(activity).show()
    }
}