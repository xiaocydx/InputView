package com.xiaocydx.inputview.sample

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.binding.bindingDelegate
import com.xiaocydx.cxrv.concat.Concat
import com.xiaocydx.cxrv.concat.toAdapter
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.reduce.doOnItemClick
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.cxrv.multitype.listAdapter
import com.xiaocydx.cxrv.multitype.register
import com.xiaocydx.inputview.sample.SampleItem.Category
import com.xiaocydx.inputview.sample.SampleItem.Element
import com.xiaocydx.inputview.sample.common.dp
import com.xiaocydx.inputview.sample.common.layoutParams
import com.xiaocydx.inputview.sample.common.matchParent
import com.xiaocydx.inputview.sample.databinding.ItemSampleCategoryBinding
import com.xiaocydx.inputview.sample.databinding.ItemSampleElementBinding
import com.xiaocydx.inputview.sample.databinding.SmapleHeaderBinding
import kotlin.reflect.KClass

/**
 * @author xcc
 * @date 2024/4/9
 */
class Sample(list: List<Elements>, private val activity: FragmentActivity) {
    private val selectedId = R.drawable.ic_sample_selected
    private val unselectedId = R.drawable.ic_sample_unselected
    private val source = mutableListOf<SampleItem>()

    init {
        list.forEach {
            source.add(Category(it.title, unselectedId))
            source.addAll(it.list)
        }
    }

    fun contentView(): View {
        val header = SmapleHeaderBinding
            .inflate(activity.layoutInflater).root
            .layoutParams(matchParent, 100.dp)
            .toAdapter()

        val content = listAdapter {
            submitList(filter())
            register(bindingDelegate(
                uniqueId = Category::title,
                inflate = ItemSampleCategoryBinding::inflate
            ) {
                onBindView {
                    tvTitle.text = it.title
                    ivSelected.setImageResource(it.resId)
                }
                getChangePayload { _, _ -> "change" }
                doOnItemClick { submitList(toggle(it)) }
            })

            register(bindingDelegate(
                uniqueId = Element::title,
                inflate = ItemSampleElementBinding::inflate
            ) {
                onBindView {
                    tvTitle.text = it.title
                    tvDesc.text = it.desc
                }
                doOnItemClick { it.perform(activity) }
            })
        }

        return RecyclerView(activity)
            .linear().divider(height = 2.dp)
            .layoutParams(matchParent, matchParent)
            .adapter(Concat.header(header).content(content).concat())
    }

    private fun toggle(category: Category): List<SampleItem> {
        val position = source.indexOf(category)
        val isSelected = !category.isSelected()
        val selectedResId = if (isSelected) selectedId else unselectedId
        source[position] = category.copy(resId = selectedResId)
        return filter()
    }

    private fun filter(): List<SampleItem> {
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

    private fun Category.isSelected() = resId == selectedId
}

sealed class SampleItem {
    data class Category(val title: String, val resId: Int) : SampleItem()

    data class Element(
        val title: String,
        val desc: String,
        private val createDialog: ((Context) -> Dialog)? = null,
        private val createFragment: (() -> DialogFragment)? = null,
        private val activityClass: KClass<out Activity>? = null
    ) : SampleItem() {
        fun perform(activity: FragmentActivity) {
            val fm = activity.supportFragmentManager
            when {
                createDialog != null -> createDialog.invoke(activity).show()
                createFragment != null -> createFragment.invoke().show(fm, null)
                activityClass != null -> activity.startActivity(Intent(activity, activityClass.java))
            }
        }
    }
}

infix fun String.desc(desc: String) = Element(title = this, desc = desc)

infix fun Element.start(clazz: KClass<out Activity>) = copy(activityClass = clazz)

infix fun Element.show(crate: (Context) -> Dialog) = copy(createDialog = crate)

infix fun Element.show(crate: () -> DialogFragment) = copy(createFragment = crate)

data class Elements(val title: String, val list: List<Element>)

fun String.elements(vararg elements: Element) = Elements(title = this, list = elements.toList())