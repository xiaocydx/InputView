/*
 * Copyright 2023 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("PackageDirectoryMismatch")

package com.xiaocydx.inputview.transform

import android.animation.RectEvaluator
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.transition.getBounds

/**
 * @author xcc
 * @date 2024/7/24
 */
open class ContentBackground(
    private val drawable: Drawable,
    match: ((Content?) -> Boolean)? = null
) : Transformer {
    private val startBounds = Rect()
    private val endBounds = Rect()
    private val currentBounds = Rect()
    private val boundsEvaluator = RectEvaluator(currentBounds)
    private var match: ((Content?) -> Boolean)? = null
    private var matchStart = false
    private var matchEnd = false
    private var canTransform = false
    private lateinit var backgroundView: View

    init {
        setMatch(match)
    }

    constructor(
        @ColorInt color: Int,
        match: ((Content?) -> Boolean)? = null
    ) : this(ColorDrawable(color), match)

    fun setMatch(match: ((Content?) -> Boolean)?) {
        this.match = match
    }

    @CallSuper
    override fun match(state: ImperfectState) = with(state) {
        matchStart = match?.invoke(previous?.content) ?: true
        matchEnd = match?.invoke(current?.content) ?: true
        startView() != null || endView() != null
    }

    override fun onPrepare(state: ImperfectState) = with(state) {
        backgroundView = findBackgroundView()
        backgroundView.overlay.remove(drawable)
        backgroundView.overlay.add(drawable)
    }

    override fun onStart(state: TransformState) = with(state) {
        startBounds.setEmpty()
        endBounds.setEmpty()
        startView()?.getBounds(startBounds)
        endView()?.getBounds(endBounds)
        canTransform = startBounds != endBounds
    }

    override fun onUpdate(state: TransformState) = with(state) {
        if (!canTransform) return@with
        // startView()?.apply {
        //     startBounds.offset(translationX.toInt(), translationY.toInt())
        // }
        // endView()?.apply {
        //     endBounds.offset(translationX.toInt(), translationY.toInt())
        // }
        boundsEvaluator.evaluate(interpolatedFraction, startBounds, endBounds)
        drawable.bounds = currentBounds
    }

    private fun ImperfectState.findBackgroundView(): View {
        for (i in 0 until contentView.childCount) {
            val child = contentView.getChildAt(i)
            if (child !is BackgroundView) continue
            if (i != 0) contentView.removeViewAt(i)
            if (i != 0) contentView.addView(child, 0)
            return child
        }
        val view = BackgroundView(contentView.context)
        contentView.addView(view, 0, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        return view
    }

    protected fun ImperfectState.startView(): View? {
        return startViews.content?.takeIf { matchStart }
    }

    protected fun ImperfectState.endView(): View? {
        return endViews.content?.takeIf { matchEnd }
    }
}

private class BackgroundView(context: Context) : View(context)