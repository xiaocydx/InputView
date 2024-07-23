package com.xiaocydx.inputview.overlay

import android.view.View

/**
 * @author xcc
 * @date 2024/7/23
 */
interface OverlayMediator {

    fun View.hasTransformer(transformer: OverlayTransformer): Boolean {
        return pendingOwner().hasTransformer(transformer)
    }

    fun View.addTransformer(transformer: OverlayTransformer) {
        pendingOwner().addTransformer(transformer)

    }

    fun View.removeTransformer(transformer: OverlayTransformer) {
        pendingOwner().removeTransformer(transformer)
    }
}

internal interface TransformerOwner {
    fun hasTransformer(transformer: OverlayTransformer): Boolean
    fun addTransformer(transformer: OverlayTransformer)
    fun removeTransformer(transformer: OverlayTransformer)
    interface Host : TransformerOwner
}

internal fun View.pendingOwner(): TransformerOwner {
    var owner = getTransformerOwner()
    if (owner == null) {
        owner = PendingOwner(this)
        setTransformerOwner(owner)
    }
    return owner
}

internal fun View.getTransformerOwner(): TransformerOwner? {
    return getTag(R.id.tag_transformer_enforcer) as? TransformerOwner
}

internal fun View.setTransformerOwner(enforcer: TransformerOwner) {
    setTag(R.id.tag_transformer_enforcer, enforcer)
}

private class PendingOwner(
    private val view: View
) : TransformerOwner, View.OnAttachStateChangeListener {
    private var hostOwner: TransformerOwner? = null
    private val transformers = mutableListOf<OverlayTransformer>()

    init {
        view.addOnAttachStateChangeListener(this)
        if (view.isAttachedToWindow) onViewAttachedToWindow(view)
    }

    override fun onViewAttachedToWindow(v: View) {
        if (hostOwner == null) {
            hostOwner = findHostOwner()
        }
        val host = hostOwner ?: return
        for (i in transformers.indices) {
            // TODO: 检查分发调度流程会不会产生问题
            host.addTransformer(transformers[i])
        }
    }

    override fun onViewDetachedFromWindow(v: View) {
        val host = hostOwner ?: return
        for (i in transformers.indices) {
            // TODO: 检查分发调度流程会不会产生问题
            host.removeTransformer(transformers[i])
        }
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

    private fun findHostOwner(): TransformerOwner? {
        var parent = view.parent as? View
        while (parent != null) {
            val enforcer = parent.getTransformerOwner()
            if (enforcer is TransformerOwner.Host) break
            parent = parent.parent as? View
        }
        return parent?.getTransformerOwner() as? TransformerOwner.Host
    }
}