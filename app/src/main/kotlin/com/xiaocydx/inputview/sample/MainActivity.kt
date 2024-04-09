package com.xiaocydx.inputview.sample

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.binding.bindingDelegate
import com.xiaocydx.cxrv.concat.Concat
import com.xiaocydx.cxrv.concat.toAdapter
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.cxrv.multitype.listAdapter
import com.xiaocydx.cxrv.multitype.register
import com.xiaocydx.inputview.sample.databinding.ItemSampleCategoryBinding
import com.xiaocydx.inputview.sample.databinding.ItemSampleElementBinding
import com.xiaocydx.inputview.sample.databinding.SmapleHeaderBinding

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
        setContentView(contentView())
    }

    private fun contentView(): View {
        val header = SmapleHeaderBinding
            .inflate(layoutInflater).root
            .layoutParams(matchParent, 100.dp)
            .toAdapter()

        val sampleList = SampleList()
        val content = listAdapter {
            submitList(sampleList.filter())
            register(bindingDelegate(
                uniqueId = SampleItem.Category::title,
                inflate = ItemSampleCategoryBinding::inflate
            ) {
                onBindView {
                    tvTitle.text = it.title
                    ivSelected.setImageResource(it.selectedResId)
                }
                getChangePayload(sampleList::categoryPayload)
                doOnSimpleItemClick { submitList(sampleList.toggle(it)) }
            })

            register(bindingDelegate(
                uniqueId = SampleItem.Element::title,
                inflate = ItemSampleElementBinding::inflate
            ) {
                onBindView {
                    tvTitle.text = it.title
                    tvDesc.text = it.desc
                }
                doOnSimpleItemClick { it.perform(this@MainActivity) }
            })
        }

        return RecyclerView(this)
            .linear().divider(height = 1.dp)
            .layoutParams(matchParent, matchParent)
            .adapter(Concat.header(header).content(content).concat())
    }
}