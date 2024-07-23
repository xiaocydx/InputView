package com.xiaocydx.inputview.overlay

import android.view.View

/**
 * 提供跟[Overlay]关联的函数
 *
 * @author xcc
 * @date 2024/7/23
 */
interface OverlayMediator {

    /**
     * 是否包含[transformer]
     */
    fun View.hasTransformer(transformer: OverlayTransformer): Boolean {
        return pendingOwner().hasTransformer(transformer)
    }

    /**
     * 添加[transformer]
     *
     * 1. 当前View附加到Window时，[transformer]同步添加到[Overlay]。
     * 2. 当前View从Window分离时，[transformer]从[Overlay]同步移除。
     */
    fun View.addTransformer(transformer: OverlayTransformer) {
        pendingOwner().addTransformer(transformer)
    }

    /**
     * 移除[addTransformer]添加的[transformer]
     */
    fun View.removeTransformer(transformer: OverlayTransformer) {
        pendingOwner().removeTransformer(transformer)
    }

    /**
     * 请求重新分发[OverlayTransformer]的函数
     */
    fun View.requestTransform() {
        pendingOwner().requestTransform()
    }
}

internal interface TransformerOwner {
    fun hasTransformer(transformer: OverlayTransformer): Boolean
    fun addTransformer(transformer: OverlayTransformer)
    fun removeTransformer(transformer: OverlayTransformer)
    fun requestTransform()
    interface Host : TransformerOwner
}

internal fun View.pendingOwner(): TransformerOwner {
    var owner = getTransformerOwner()
    if (owner == null) {
        owner = PendingTransformerOwner(this)
        setTransformerOwner(owner)
    }
    return owner
}

internal fun View.getTransformerOwner(): TransformerOwner? {
    return getTag(R.id.tag_transformer_owner) as? TransformerOwner
}

internal fun View.setTransformerOwner(enforcer: TransformerOwner) {
    setTag(R.id.tag_transformer_owner, enforcer)
}

private class PendingTransformerOwner(
    private val view: View
) : TransformerOwner, View.OnAttachStateChangeListener {
    private var hostOwner: TransformerOwner? = null
    private val transformers = mutableListOf<OverlayTransformer>()

    init {
        view.addOnAttachStateChangeListener(this)
        if (view.isAttachedToWindow) onViewAttachedToWindow(view)
    }

    override fun onViewAttachedToWindow(v: View) {
        // attach在onPrepare()执行，此时transformers的函数还未分发调用
        if (hostOwner == null) hostOwner = findHostOwner()
        val host = hostOwner ?: return
        for (i in transformers.indices) host.addTransformer(transformers[i])
    }

    override fun onViewDetachedFromWindow(v: View) {
        // detach在onPrepare()或onEnd()执行，此时transformers的函数分发调用完毕
        val host = hostOwner ?: return
        for (i in transformers.indices) host.removeTransformer(transformers[i])
    }

    override fun hasTransformer(transformer: OverlayTransformer): Boolean {
        return transformers.contains(transformer)
    }

    override fun addTransformer(transformer: OverlayTransformer) {
        if (hasTransformer(transformer)) return
        transformers.add(transformer)
        if (view.isAttachedToWindow) hostOwner?.addTransformer(transformer)
    }

    override fun removeTransformer(transformer: OverlayTransformer) {
        transformers.remove(transformer)
        hostOwner?.removeTransformer(transformer)
    }

    override fun requestTransform() {
        hostOwner?.requestTransform()
    }

    private fun findHostOwner(): TransformerOwner.Host? {
        var parent = view.parent as? View
        while (parent != null) {
            val enforcer = parent.getTransformerOwner()
            if (enforcer is TransformerOwner.Host) break
            parent = parent.parent as? View
        }
        return parent?.getTransformerOwner() as? TransformerOwner.Host
    }
}