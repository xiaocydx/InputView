package com.xiaocydx.inputview.sample.scene.figure.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.MutableStateList
import com.xiaocydx.cxrv.paging.LoadResult
import com.xiaocydx.cxrv.paging.Pager
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.cxrv.paging.PagingPrefetch
import com.xiaocydx.cxrv.paging.appendPrefetch
import com.xiaocydx.cxrv.paging.storeIn
import com.xiaocydx.inputview.sample.scene.figure.Dubbing
import kotlinx.coroutines.delay

/**
 * @author xcc
 * @date 2024/8/15
 */
class DubbingViewModel : ViewModel() {
    private val list = MutableStateList<Dubbing>()
    private val pager = Pager(
        initKey = 1,
        config = PagingConfig(pageSize = 10)
    ) { params ->
        // 100ms模拟很快的加载
        delay(100)
        val start = params.pageSize * (params.key - 1) + 1
        val end = start + params.pageSize - 1
        val data = (start..end).map { Dubbing(id = it.toString(), name = "配音$it") }
        val nextKey = if (params.key == 10) null else params.key + 1
        LoadResult.Success(data, nextKey)
    }

    val dubbingPagingFlow = pager.flow
        .storeIn(list, viewModelScope)
        .appendPrefetch(PagingPrefetch.ItemCount(3))

    fun findTargetPosition(dubbing: Dubbing): Int {
        return list.indexOf(dubbing).coerceAtLeast(0)
    }
}