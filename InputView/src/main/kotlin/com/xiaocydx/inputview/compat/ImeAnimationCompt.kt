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

package com.xiaocydx.inputview.compat

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.view.*
import android.view.animation.Interpolator
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.core.view.*
import androidx.core.view.WindowInsetsCompat.Type.ime
import com.xiaocydx.inputview.R

/**
 * 修改Android 11及以上IME动画的`durationMillis`和`interpolator`
 *
 * ### 主要作用
 * 该函数的主要作用是结合实际场景需求，微调IME动画的属性，让Android 11及以上的设备有更好的交互体验，
 * 不支持修改Android 11以下IME动画的属性，原因是Android 11以下无法跟IME完全贴合，保持兼容代码即可。
 *
 * ### 修改失败
 * 当修改IME动画的属性失败时，会在[WindowInsetsAnimationCompat.Callback.onPrepare]之后，
 * [WindowInsetsAnimationCompat.Callback.onStart]之前恢复`durationMillis`和`interpolator`，
 * 修改成功或修改失败，都会按[TAG]打印日志。
 *
 * ### 兼容场景
 * 当[WindowInsetsAnimationCompat]的初始`durationMillis <= 0`时，不修改`durationMillis`和`interpolator`，
 * 目的是兼容[WindowInsetsControllerCompat.controlWindowInsetsAnimation]的`durationMillis <= 0`的场景，
 * 例如通过[WindowInsetsAnimationControlListenerCompat.onReady]获取[WindowInsetsAnimationControllerCompat]，
 * 调用[WindowInsetsAnimationControllerCompat.setInsetsAndAlpha]实现手势拖动显示IME。
 *
 * **注意**：若要对`window.decorView`设置[WindowInsetsAnimationCompat.Callback]，
 * 则调用[setWindowInsetsAnimationCallbackCompat]进行设置，避免跟该函数产生冲突。
 *
 * @param durationMillis IME动画的时长，默认值[NO_VALUE]表示保持系统原本的时长，
 * Android 11以下兼容代码的`durationMillis = 160ms`，
 * Android 11及以上系统代码的`durationMillis = 285ms`。
 *
 * @param interpolator   IME动画的插值器，默认值`null`表示保持系统原本的插值器，
 * Android 11以下兼容代码的`interpolator = DecelerateInterpolator()`，
 * Android 11及以上系统代码的`interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)`。
 */
internal fun Window.modifyImeAnimationCompat(
    durationMillis: Long = NO_VALUE,
    interpolator: Interpolator? = null,
) {
    if (!supportModifyImeAnimation) return
    decorView.doOnAttach {
        // doOnAttach()确保insetsController是InsetsController，而不是PendingInsetsController
        val controller = requireNotNull(insetsController) { "InsetsController为null" }
        imeAnimationCallback = ImeAnimationCallback(
            durationMillis, interpolator,
            controller, insetsAnimationCallback
        )
        // 对视图树的根View（排除ViewRootImpl）decorView设置imeAnimationCallback，
        // 目的是避免WindowInsetsAnimationCompat.Callback的分发逻辑产生歧义，例如：
        // ViewCompat.setWindowInsetsAnimationCallback(decorView, callback)
        // ViewCompat.setWindowInsetsAnimationCallback(childView, imeAnimationCallback)
        // 1. childView是decorView的间接子View。
        // 2. callback的dispatchMode是DISPATCH_MODE_CONTINUE_ON_SUBTREE。
        // 若对childView设置imeAnimationCallback，则imeAnimationCallback修改IME动画的属性，
        // 会影响到decorView的callback函数，callback.onPrepare()获取的是IME动画原本的属性，
        // 而callback.onStart()获取的却是IME动画修改后的属性，分发逻辑产生歧义。
        decorView.setWindowInsetsAnimationCallback(imeAnimationCallback)
    }
}

/**
 * 恢复[modifyImeAnimationCompat]修改的`durationMillis`和`interpolator`
 */
internal fun Window.restoreImeAnimationCompat() {
    if (!supportModifyImeAnimation) return
    decorView.doOnAttach { decorView.setWindowInsetsAnimationCallback(insetsAnimationCallback) }
}

/**
 * 对`window.decorView`设置[WindowInsetsAnimationCompat.Callback]，
 * 该函数能避免跟[modifyImeAnimationCompat]产生冲突，实际效果等同于：
 * ```
 * ViewCompat.setWindowInsetsAnimationCallback(window.decorView, callback)
 * ```
 */
internal fun Window.setWindowInsetsAnimationCallbackCompat(callback: WindowInsetsAnimationCompat.Callback?) {
    if (!supportModifyImeAnimation) {
        return ViewCompat.setWindowInsetsAnimationCallback(decorView, callback)
    }
    val proxyCallback = if (callback == null) null else {
        InsetsReflectCache.insetsAnimationCompat?.proxyCallbackConstructor
            ?.newInstance(callback) as? WindowInsetsAnimation.Callback
    }
    insetsAnimationCallback = proxyCallback
    imeAnimationCallback?.apply { modifyImeAnimationCompat(durationMillis, interpolator) }
            ?: run { decorView.setWindowInsetsAnimationCallback(insetsAnimationCallback) }
}

@ChecksSdkIntAtLeast(api = 30)
private val supportModifyImeAnimation = Build.VERSION.SDK_INT >= 30

@get:RequiresApi(30)
private var Window.imeAnimationCallback: ImeAnimationCallback?
    get() = decorView.getTag(R.id.tag_decor_view_ime_animation_callback) as? ImeAnimationCallback
    set(value) {
        decorView.setTag(R.id.tag_decor_view_ime_animation_callback, value)
    }

@get:RequiresApi(30)
private var Window.insetsAnimationCallback: WindowInsetsAnimation.Callback?
    get() = decorView.getTag(R.id.tag_decor_view_insets_animation_callback) as? WindowInsetsAnimation.Callback
    set(value) {
        decorView.setTag(R.id.tag_decor_view_insets_animation_callback, value)
    }

/**
 * 修改Android 11及以上IME动画的`durationMillis`和`interpolator`
 *
 * `InsetsController.show()`或`InsetsController.hide()`的执行顺序：
 * 1. 构建`InsetsController.InternalAnimationControlListener`。
 *
 * 2. 构建`InsetsController.InsetsAnimationControlImpl`（因为设置了WindowInsetsAnimationCompat.Callback）。
 *
 * 3. `InsetsController.InsetsAnimationControlImpl`的构造函数调用`InsetsController.startAnimation()`。
 *
 * 4. `InsetsController.startAnimation()`调用至`WindowInsetsAnimationCompat.Callback.onPrepare()`，
 * 然后添加`doOnPreDraw()`，在下一帧调用至`WindowInsetsAnimationCompat.Callback.onStart()`。
 *
 * 5. `InsetsController.mRunningAnimations.add()`添加`RunningAnimation`，
 * `RunningAnimation`包含第2步构建的`InsetsController.InsetsAnimationControlImpl`.
 *
 * 6. 下一帧`doOnPreDraw()`执行，调用至`WindowInsetsAnimationCompat.Callback.onStart()`，
 * 然后调用`InsetsController.InternalAnimationControlListener.onReady()`构建属性动画。
 *
 * [onPrepare]修改第4步的[WindowInsetsAnimation]，[onStart]修改第6步构建的属性动画。
 */
@RequiresApi(30)
private class ImeAnimationCallback(
    val durationMillis: Long,
    val interpolator: Interpolator?,
    private val controller: WindowInsetsController,
    private val delegate: WindowInsetsAnimation.Callback?,
    dispatchMode: Int = delegate?.dispatchMode ?: DISPATCH_MODE_CONTINUE_ON_SUBTREE
) : WindowInsetsAnimation.Callback(dispatchMode) {
    private val trackers = mutableMapOf<WindowInsetsAnimation, Tracker>()

    /**
     * 对视图树分发[animation]之前，修改[animation]的属性值
     */
    override fun onPrepare(animation: WindowInsetsAnimation) {
        val tracker = getTracker(animation)
        if (tracker.step === Step.ON_PREPARE) {
            InsetsReflectCache.insetsAnimation?.apply {
                tracker.modifyInterpolatorIfNecessary { mInterpolatorField.set(animation, it) }
                tracker.modifyDurationMillisIfNecessary { mDurationMillisField.set(animation, it) }
            }
            tracker.checkOnPrepare(animation)
        }
        delegate?.onPrepare(animation)
    }

    /**
     * 由于[onStart]是在下一帧`doOnPreDraw()`执行，因此：
     * 1. 此时`InsetsController.mRunningAnimations`已添加`RunningAnimation`。
     *
     * 2. 从`RunningAnimation`获取`InsetsController.InsetsAnimationControlImpl`，即`runner`。
     *
     * 3. 从`runner`获取[WindowInsetsAnimation]，若跟[animation]的[WindowInsetsAnimation]相同，
     * 则为目标`runner`。
     *
     * 4. 从目标`runner`获取`InsetsController.InternalAnimationControlListener`，
     * 修改计算好的`InsetsController.InternalAnimationControlListener.mDurationMs`。
     *
     * 5. [onStart]之后调用的`InsetsController.InternalAnimationControlListener.onReady()`,
     * 会通过`InsetsController.InternalAnimationControlListener.getInsetsInterpolator()`，
     * 获取属性动画的插值器，即`InsetsController.SYNC_IME_INTERPOLATOR`，因此在获取插值器之前，
     * 先修改`InsetsController.SYNC_IME_INTERPOLATOR`，在属性动画开始后，即下一帧动画更新之前，
     * 再恢复`InsetsController.SYNC_IME_INTERPOLATOR`，尽可能减少修改静态变量造成的影响。
     */
    override fun onStart(
        animation: WindowInsetsAnimation,
        bounds: WindowInsetsAnimation.Bounds
    ): WindowInsetsAnimation.Bounds = with(InsetsReflectCache) {
        val tracker = getTracker(animation)
        if (tracker.step === Step.ON_START) {
            val runningAnimations = insetsController?.mRunningAnimationsField
                ?.get(controller)?.let { it as? ArrayList<*> } ?: emptyList<Any>()

            var listener: Any? = null
            for (index in runningAnimations.indices) {
                val runningAnimation = runningAnimations[index]
                val runner = insetsController?.runnerField?.get(runningAnimation)
                val target = insetsAnimationRunner?.mAnimationField?.get(runner)
                if (target !== animation) continue

                listener = insetsAnimationRunner?.mListenerField?.get(runner) ?: continue
                tracker.modifyInterpolatorIfNecessary {
                    insetsController?.syncImeInterpolatorField?.set(null, it)
                }
                tracker.modifyDurationMillisIfNecessary {
                    animationControlListener?.mDurationMsField?.set(listener, it)
                }
                break
            }
            tracker.checkOnStart(animation, listener)
        }
        return delegate?.onStart(animation, bounds) ?: bounds
    }

    override fun onProgress(
        insets: WindowInsets,
        runningAnimations: MutableList<WindowInsetsAnimation>
    ): WindowInsets {
        return delegate?.onProgress(insets, runningAnimations) ?: insets
    }

    override fun onEnd(animation: WindowInsetsAnimation) {
        trackers.remove(animation)
        delegate?.onEnd(animation)
    }

    private fun getTracker(animation: WindowInsetsAnimation): Tracker {
        var tracker: Tracker? = trackers[animation]
        if (tracker == null) {
            tracker = Tracker(animation)
            trackers[animation] = tracker
        }
        return tracker
    }

    /**
     * 跟踪[animation]的回调函数，检查每一步是否修改成功，若修改失败，则恢复初始值
     */
    private inner class Tracker(private val animation: WindowInsetsAnimation) {
        private val initialInterpolator = animation.interpolator
        private val initialDurationMillis = animation.durationMillis
        private var modifyInterpolatorOutcome = false
        private var modifyDurationMillisOutcome = false
        var step: Step = Step.COMPLETED
            private set

        init {
            if (!InsetsReflectCache.reflectSucceed) {
                Log.d(TAG, "ReflectionCache反射失败，不做修改")
            } else if (animation.typeMask and ime() == 0) {
                Log.d(TAG, "WindowInsetsAnimationCompat不包含IME类型，不做修改")
            } else if (initialDurationMillis <= 0) {
                Log.d(TAG, "兼容initialDurationMillis <= 0的场景，不做修改")
            } else {
                step = Step.ON_PREPARE
            }
        }

        inline fun modifyInterpolatorIfNecessary(action: (Interpolator) -> Boolean?) {
            modifyInterpolatorOutcome = when (interpolator) {
                null -> true
                else -> action(interpolator) == true
            }
        }

        inline fun modifyDurationMillisIfNecessary(action: (Long) -> Boolean?) {
            modifyDurationMillisOutcome = when {
                durationMillis <= NO_VALUE -> true
                else -> action(durationMillis) == true
            }
        }

        fun checkOnPrepare(wrapped: WindowInsetsAnimation?) {
            if (step !== Step.ON_PREPARE) return
            val succeed = wrapped != null && consumeModifyOutcome()
            if (!succeed) wrapped?.let(::restoreInsetsAnimation)
            if (succeed) step = Step.ON_START
            val outcome = if (succeed) "成功" else "失败"
            Log.d(TAG, "onPrepare()修改WindowInsetsAnimation$outcome")
        }

        fun checkOnStart(wrapped: WindowInsetsAnimation?, listener: Any?) {
            if (step !== Step.ON_START) return
            val succeed = wrapped != null && listener != null && consumeModifyOutcome()
            if (!succeed) {
                wrapped?.let(::restoreInsetsAnimation)
                listener?.let(::restoreControlListener)
                restoreSyncImeInterpolator()
            } else {
                // 将恢复操作插到下一帧动画更新之前，尽可能减少修改静态变量造成的影响
                Choreographer.getInstance().postFrameCallbackDelayed({
                    restoreSyncImeInterpolator()
                }, Long.MIN_VALUE)
            }
            step = Step.COMPLETED
            val outcome = if (succeed) "成功" else "失败"
            Log.d(TAG, "onStart()修改InternalAnimationControlListener$outcome")
        }

        private fun consumeModifyOutcome(): Boolean {
            val outcome = modifyInterpolatorOutcome && modifyDurationMillisOutcome
            modifyInterpolatorOutcome = false
            modifyDurationMillisOutcome = false
            return outcome
        }

        private fun restoreInsetsAnimation(wrapped: WindowInsetsAnimation) {
            InsetsReflectCache.insetsAnimation?.apply {
                mInterpolatorField.set(wrapped, initialInterpolator)
                mDurationMillisField.set(wrapped, initialDurationMillis)
            }
        }

        private fun restoreControlListener(listener: Any) {
            InsetsReflectCache.animationControlListener
                ?.mDurationMsField?.set(listener, initialDurationMillis)
        }

        private fun restoreSyncImeInterpolator() {
            InsetsReflectCache.insetsController?.apply {
                syncImeInterpolatorField.set(null, initialSyncImeInterpolator)
            }
        }
    }

    private enum class Step {
        ON_PREPARE, ON_START, COMPLETED
    }
}

private const val TAG = "ImeAnimationCallback"

private const val NO_VALUE = -1L

@RequiresApi(30)
@SuppressLint("PrivateApi")
private object InsetsReflectCache : ReflectHelper {
    var insetsAnimation: WindowInsetsAnimationCache? = null; private set
    var insetsAnimationCompat: WindowInsetsAnimationCompatCache? = null; private set
    var insetsController: InsetsControllerCache? = null; private set
    var insetsAnimationRunner: InsetsAnimationControlImplCache? = null; private set
    var animationControlListener: InternalAnimationControlListenerCache? = null; private set
    var reflectSucceed: Boolean = false; private set

    init {
        runCatching {
            reflectInsetsAnimation()
            reflectInsetsAnimationCompat()
            reflectInsetsController()
            reflectInsetsAnimationRunner()
            reflectAnimationControlListener()
            reflectSucceed = true
        }.onFailure {
            insetsAnimation = null
            insetsController = null
            insetsAnimationRunner = null
            animationControlListener = null
        }
    }

    private fun reflectInsetsAnimation() {
        val fields = WindowInsetsAnimation::class.java.declaredInstanceFields
        insetsAnimation = WindowInsetsAnimationCache(
            mInterpolatorField = fields.find(name = "mInterpolator").toCache(),
            mDurationMillisField = fields.find(name = "mDurationMillis").toCache()
        )
    }

    private fun reflectInsetsAnimationCompat() {
        val proxyCallbackClass = Class.forName(
            "androidx.core.view.WindowInsetsAnimationCompat\$Impl30\$ProxyCallback")
        val proxyCallbackConstructor = proxyCallbackClass
            .getDeclaredConstructor(WindowInsetsAnimationCompat.Callback::class.java).toCache()
        insetsAnimationCompat = WindowInsetsAnimationCompatCache(proxyCallbackConstructor)
    }

    private fun reflectInsetsController() {
        val insetsControllerClass = Class.forName("android.view.InsetsController")
        val runningAnimationClass = Class.forName("android.view.InsetsController\$RunningAnimation")
        val mRunningAnimationsField = insetsControllerClass
            .declaredInstanceFields.find(name = "mRunningAnimations").toCache()
        insetsController = InsetsControllerCache(
            syncImeInterpolatorField = insetsControllerClass
                .declaredStaticFields.find(name = "SYNC_IME_INTERPOLATOR").toCache(),
            mRunningAnimationsField = mRunningAnimationsField,
            runnerField = runningAnimationClass
                .declaredInstanceFields.find(name = "runner").toCache()
        )
    }

    private fun reflectInsetsAnimationRunner() {
        val insetsAnimationRunnerClass = Class.forName(
            "android.view.InsetsAnimationControlImpl")
        val fields = insetsAnimationRunnerClass.declaredInstanceFields
        insetsAnimationRunner = InsetsAnimationControlImplCache(
            mListenerField = fields.find(name = "mListener").toCache(),
            mAnimationField = fields.find(name = "mAnimation").toCache()
        )
    }

    fun reflectAnimationControlListener() {
        val animationControlListenerClass = Class.forName(
            "android.view.InsetsController\$InternalAnimationControlListener")
        animationControlListener = InternalAnimationControlListenerCache(
            mDurationMsField = animationControlListenerClass
                .declaredInstanceFields.find(name = "mDurationMs").toCache()
        )
    }
}

/**
 * ```
 * public final class WindowInsetsAnimation {
 *     private final Interpolator mInterpolator;
 *     private final long mDurationMillis;
 * }
 * ```
 */
@RequiresApi(30)
private class WindowInsetsAnimationCache(
    val mInterpolatorField: FieldCache,
    val mDurationMillisField: FieldCache
)

/**
 * ```
 * public final class WindowInsetsAnimationCompat {
 *
 *     private static class Impl30 extends Impl {
 *         private static class ProxyCallback extends WindowInsetsAnimation.Callback {
 *             ProxyCallback(@NonNull final WindowInsetsAnimationCompat.Callback compat)
 *         }
 *     }
 * }
 * ```
 */
@RequiresApi(30)
private class WindowInsetsAnimationCompatCache(
    val proxyCallbackConstructor: ConstructorCache
)

/**
 * ```
 * public class InsetsController implements WindowInsetsController {
 *     private static final Interpolator SYNC_IME_INTERPOLATOR =
 *             new PathInterpolator(0.2f, 0f, 0f, 1f);
 *     private final ArrayList<RunningAnimation> mRunningAnimations = new ArrayList<>();
 *
 *     private static class RunningAnimation {
 *         final InsetsAnimationControlRunner runner;
 *     }
 * }
 * ```
 */
@RequiresApi(30)
private class InsetsControllerCache(
    val syncImeInterpolatorField: FieldCache,
    val mRunningAnimationsField: FieldCache,
    val runnerField: FieldCache
) {
    val initialSyncImeInterpolator: Any? = syncImeInterpolatorField.get(null)
}

/**
 * ```
 * public class InsetsAnimationControlImpl implements InsetsAnimationControlRunner {
 *     private final WindowInsetsAnimationControlListener mListener;
 *     private final WindowInsetsAnimation mAnimation;
 * }
 * ```
 */
@RequiresApi(30)
private class InsetsAnimationControlImplCache(
    val mListenerField: FieldCache,
    val mAnimationField: FieldCache
)

/**
 * ```
 * public static class InternalAnimationControlListener
 *         implements WindowInsetsAnimationControlListener {
 *     private final long mDurationMs;
 *
 *     protected Interpolator getInsetsInterpolator() {
 *         if ((mRequestedTypes & ime()) != 0) {
 *             if (mHasAnimationCallbacks) {
 *                 return SYNC_IME_INTERPOLATOR;
 *             }
 *             ...
 *         }
 *         ...
 *     }
 * }
 * ```
 */
@RequiresApi(30)
private class InternalAnimationControlListenerCache(val mDurationMsField: FieldCache)