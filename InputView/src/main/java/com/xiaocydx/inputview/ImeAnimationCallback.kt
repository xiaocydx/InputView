package com.xiaocydx.inputview

import android.os.Build
import android.util.Log
import android.view.*
import android.view.animation.Interpolator
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.core.view.*
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.Field

/**
 * 设置IME动画的[WindowInsetsAnimationCompat.Callback]
 *
 * **注意**：仅支持修改Android 11及以上IME动画的`durationMillis`和`interpolator`，当修改失败时，
 * 会在[WindowInsetsAnimationCompat.Callback.onStart]之前恢复`durationMillis`和`interpolator`，
 * 修改成功或失败，会按[TAG]打印日志。
 *
 * 当[WindowInsetsAnimationCompat]的初始`durationMillis`小于等于0时，不修改`durationMillis`和`interpolator`，
 * 目的是兼容[WindowInsetsControllerCompat.controlWindowInsetsAnimation]的`durationMillis`小于等于0的场景，
 * 例如通过[WindowInsetsAnimationControlListenerCompat.onReady]获取[WindowInsetsAnimationControllerCompat]，
 * 调用[WindowInsetsAnimationControllerCompat.setInsetsAndAlpha]实现手势拖动显示IME。
 *
 * @param durationMillis 修改后的`durationMillis`，默认值[NO_VALUE]表示不修改。
 * @param interpolator   修改后的`interpolator`，默认值`null`表示不修改。
 * @param callback       当[WindowInsetsAnimationCompat.getTypeMask]包含IME类型时，才调用回调函数。
 */
internal fun View.setImeAnimationCallbackCompat(
    durationMillis: Long = NO_VALUE,
    interpolator: Interpolator? = null,
    callback: WindowInsetsAnimationCompat.Callback?
) {
    val finalCallback = when {
        callback == null -> null
        Build.VERSION.SDK_INT < 30 -> ImeAnimationCallback(callback)
        else -> ImeAnimationCallback30(this, durationMillis, interpolator, callback)
    }
    ViewCompat.setWindowInsetsAnimationCallback(this, finalCallback)
}

/**
 * 当[WindowInsetsAnimationCompat.getTypeMask]包含IME类型时，才调用回调函数
 */
private open class ImeAnimationCallback(
    private val delegate: WindowInsetsAnimationCompat.Callback
) : WindowInsetsAnimationCompat.Callback(delegate.dispatchMode) {

    override fun onPrepare(animation: WindowInsetsAnimationCompat) {
        if (animation.containsImeType()) delegate.onPrepare(animation)
    }

    override fun onStart(
        animation: WindowInsetsAnimationCompat,
        bounds: WindowInsetsAnimationCompat.BoundsCompat
    ): WindowInsetsAnimationCompat.BoundsCompat {
        return if (animation.containsImeType()) delegate.onStart(animation, bounds) else bounds
    }

    override fun onProgress(
        insets: WindowInsetsCompat,
        runningAnimations: List<WindowInsetsAnimationCompat>
    ): WindowInsetsCompat {
        val animations = runningAnimations.filter { it.containsImeType() }
        return if (animations.isNotEmpty()) delegate.onProgress(insets, animations) else insets
    }

    override fun onEnd(animation: WindowInsetsAnimationCompat) {
        if (animation.containsImeType()) delegate.onEnd(animation)
    }

    protected fun WindowInsetsAnimationCompat.containsImeType(): Boolean {
        val imeType = WindowInsetsCompat.Type.ime()
        return typeMask and imeType == imeType
    }
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
 * 然后添加doOnPreDraw，在下一帧调用至`WindowInsetsAnimationCompat.Callback.onStart()`。
 *
 * 5. `InsetsController.mRunningAnimations.add()`添加`RunningAnimation`，
 * `RunningAnimation`包含第2步构建的`InsetsController.InsetsAnimationControlImpl`.
 *
 * 6. 下一帧doOnPreDraw执行，调用至`WindowInsetsAnimationCompat.Callback.onStart()`，
 * 然后调用`InsetsController.InternalAnimationControlListener.onReady()`构建属性动画。
 *
 * [onPrepare]修改第4步的[WindowInsetsAnimation]，[onStart]修改第6步构建的属性动画。
 */
@RequiresApi(30)
private class ImeAnimationCallback30(
    private val view: View,
    private val durationMillis: Long,
    private val interpolator: Interpolator?,
    delegate: WindowInsetsAnimationCompat.Callback
) : ImeAnimationCallback(delegate) {
    private val cache: InsetsReflectCache
        get() = cacheThreadLocal.get()!!
    private val trackers = mutableMapOf<WindowInsetsAnimationCompat, Tracker>()

    /**
     * 修改[WindowInsetsAnimationCompat]中[WindowInsetsAnimation]的属性值，
     * 确保[delegate]访问[WindowInsetsAnimationCompat]，获取到的是修改结果。
     */
    override fun onPrepare(animation: WindowInsetsAnimationCompat) = cache.run {
        val tracker = getTracker(animation)
        if (tracker.step === Step.ON_PREPARE) {
            val wrapped = animation.getWrapped()
            ensureInsetsAnimation(wrapped)
            insetsAnimation?.apply {
                modifyInterpolatorIfNecessary { mInterpolatorField.set(wrapped, it) }
                modifyDurationMillisIfNecessary { mDurationMillisField.set(wrapped, it) }
            }
            tracker.checkOnPrepare(wrapped)
        }
        super.onPrepare(animation)
    }

    /**
     * 由于[onStart]是在下一帧doOnPreDraw执行，因此：
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
        animation: WindowInsetsAnimationCompat,
        bounds: WindowInsetsAnimationCompat.BoundsCompat
    ): WindowInsetsAnimationCompat.BoundsCompat = cache.run {
        val tracker = getTracker(animation)
        if (tracker.step === Step.ON_START) {
            val wrapped = animation.getWrapped()
            val controller = view.windowInsetsController
            ensureInsetsController(controller)
            val runningAnimations = insetsController?.mRunningAnimationsField
                ?.get(controller)?.let { it as? ArrayList<*> } ?: emptyList<Any>()

            var listener: Any? = null
            for (index in runningAnimations.indices) {
                val runningAnimation = runningAnimations[index]
                val runner = insetsController?.runnerField?.get(runningAnimation)
                ensureInsetsAnimationRunner(runner)
                val target = insetsAnimationRunner?.mAnimationField?.get(runner)
                if (target !== wrapped) continue

                listener = insetsAnimationRunner?.mListenerField?.get(runner) ?: continue
                ensureAnimationControlListener(listener)
                modifyInterpolatorIfNecessary {
                    insetsController?.syncImeInterpolator?.set(null, it)
                }
                modifyDurationMillisIfNecessary {
                    animationControlListener?.mDurationMsField?.set(listener, it)
                }
                break
            }

            if (listener != null && modifyInterpolatorIfNecessary()) {
                // 将恢复操作插到下一帧动画更新之前，尽可能减少修改静态变量造成的影响
                Choreographer.getInstance().postFrameCallback {
                    // 恢复InsetsController.SYNC_IME_INTERPOLATOR原本的值
                    tracker.resumeSyncImeInterpolator()
                }
            }

            tracker.checkOnStart(wrapped, listener)
        }
        return super.onStart(animation, bounds)
    }

    override fun onEnd(animation: WindowInsetsAnimationCompat) {
        removeTracker(animation)
        super.onEnd(animation)
    }

    private fun getTracker(animation: WindowInsetsAnimationCompat): Tracker {
        var tracker: Tracker? = trackers[animation]
        if (tracker == null) {
            tracker = Tracker(animation)
            trackers[animation] = tracker
        }
        return tracker
    }

    private fun removeTracker(animation: WindowInsetsAnimationCompat) {
        trackers.remove(animation)
    }

    private fun WindowInsetsAnimationCompat.getWrapped(): WindowInsetsAnimation? = cache.run {
        if (view.windowInsetsController == null) return null
        val animation = this@getWrapped
        ensureInsetsAnimationCompat(animation)
        val impl = insetsAnimationCompat?.mImplField?.get(animation) ?: return null
        return insetsAnimationCompat?.mWrappedField?.get(impl) as? WindowInsetsAnimation
    }

    private inline fun modifyInterpolatorIfNecessary(action: (Interpolator) -> Unit = {}): Boolean {
        return interpolator?.apply(action) != null
    }

    private inline fun modifyDurationMillisIfNecessary(action: (Long) -> Unit = {}): Boolean {
        return durationMillis.takeIf { it > NO_VALUE }?.apply(action) != null
    }

    /**
     * 跟踪[animation]的回调，检查每一步是否修改成功，若修改失败，则恢复初始值
     */
    private inner class Tracker(private val animation: WindowInsetsAnimationCompat) {
        private val initialInterpolator = animation.interpolator
        private val initialDurationMillis = animation.durationMillis
        var step: Step = Step.COMPLETED
            private set

        init {
            if (!animation.containsImeType()) {
                Log.d(TAG, "WindowInsetsAnimationCompat不包含IME类型，不做修改")
            } else if (initialDurationMillis <= 0) {
                Log.d(TAG, "兼容initialDurationMillis小于等于0的场景，不做修改")
            } else {
                step = Step.ON_PREPARE
            }
        }

        fun checkOnPrepare(wrapped: WindowInsetsAnimation?) {
            assert(step === Step.ON_PREPARE)
            var succeed = wrapped != null
            val checkInterpolator = !modifyInterpolatorIfNecessary()
                    || animation.interpolator === interpolator
            val checkDurationMillis = !modifyDurationMillisIfNecessary()
                    || animation.durationMillis == durationMillis
            if (!checkInterpolator || !checkDurationMillis) {
                succeed = false
                wrapped?.let(::resumeInsetsAnimation)
            }
            if (succeed) step = Step.ON_START
            val outcome = if (succeed) "成功" else "失败"
            Log.d(TAG, "onPrepare()修改WindowInsetsAnimation$outcome")
        }

        fun checkOnStart(wrapped: WindowInsetsAnimation?, listener: Any?): Unit = with(cache) {
            assert(step === Step.ON_START)
            var succeed = wrapped != null && listener != null
            val checkInterpolator = !modifyInterpolatorIfNecessary()
                    || insetsController?.syncImeInterpolator?.get(null) === interpolator
            val checkDurationMillis = !modifyDurationMillisIfNecessary()
                    || animationControlListener?.mDurationMsField?.get(listener) == durationMillis
            if (!checkInterpolator || !checkDurationMillis) {
                succeed = false
                wrapped?.let(::resumeInsetsAnimation)
                listener?.let(::resumeControlListener)
                resumeSyncImeInterpolator()
            }
            step = Step.COMPLETED
            val outcome = if (succeed) "成功" else "失败"
            Log.d(TAG, "onStart()修改InternalAnimationControlListener$outcome")
        }

        fun resumeInsetsAnimation(wrapped: WindowInsetsAnimation): Unit = with(cache) {
            insetsAnimation?.apply {
                mInterpolatorField.set(wrapped, initialInterpolator)
                mDurationMillisField.set(wrapped, initialDurationMillis)
            }
        }

        fun resumeControlListener(listener: Any): Unit = with(cache) {
            animationControlListener?.mDurationMsField?.set(listener, initialDurationMillis)
        }

        fun resumeSyncImeInterpolator(): Unit = with(cache) {
            insetsController?.apply { syncImeInterpolator.set(null, initialSyncImeInterpolator) }
        }
    }

    private enum class Step {
        ON_PREPARE, ON_START, COMPLETED
    }
}

internal const val TAG = "ImeAnimationCallback"

private const val NO_VALUE = -1L

@RequiresApi(30)
private val cacheThreadLocal = object : ThreadLocal<InsetsReflectCache>() {
    override fun initialValue(): InsetsReflectCache = InsetsReflectCache()
}

@UiThread
@RequiresApi(30)
private class InsetsReflectCache {
    var insetsAnimation: WindowInsetsAnimationCache? = null; private set
    var insetsAnimationCompat: WindowInsetsAnimationCompatCache? = null; private set
    var insetsController: InsetsControllerCache? = null; private set
    var insetsAnimationRunner: InsetsAnimationControlImplCache? = null; private set
    var animationControlListener: InternalAnimationControlListenerCache? = null; private set

    fun ensureInsetsAnimation(obj: WindowInsetsAnimation?) {
        if (insetsAnimation != null) return
        val fields = obj?.javaClass?.let(::getInstanceFields)
        insetsAnimation = WindowInsetsAnimationCache(
            mInterpolatorField = fields?.find(name = "mInterpolator").let(::FieldCache),
            mDurationMillisField = fields?.find(name = "mDurationMillis").let(::FieldCache)
        )
    }

    fun ensureInsetsAnimationCompat(obj: WindowInsetsAnimationCompat?) {
        if (insetsAnimationCompat != null) return
        val mImplField = runCatching {
            obj?.javaClass?.getDeclaredField("mImpl")
        }.getOrNull().let(::FieldCache)
        insetsAnimationCompat = WindowInsetsAnimationCompatCache(
            mImplField = mImplField,
            mWrappedField = runCatching {
                mImplField.get(obj)?.javaClass?.getDeclaredField("mWrapped")
            }.getOrNull().let(::FieldCache)
        )
    }

    fun ensureInsetsController(obj: WindowInsetsController?) {
        if (insetsController != null) return
        val mRunningAnimationsField = obj?.javaClass?.let(::getInstanceFields)
            ?.find(name = "mRunningAnimations").let(::FieldCache)
        val runningAnimation = mRunningAnimationsField.get(obj)
            ?.let { it as? ArrayList<*> }?.firstOrNull()
        insetsController = InsetsControllerCache(
            syncImeInterpolator = obj?.javaClass?.let(::getStaticFields)
                ?.find(name = "SYNC_IME_INTERPOLATOR").let(::FieldCache),
            mRunningAnimationsField = mRunningAnimationsField,
            runnerField = runningAnimation?.javaClass?.let(::getInstanceFields)
                ?.find(name = "runner").let(::FieldCache)
        )
    }

    fun ensureInsetsAnimationRunner(obj: Any?) {
        if (insetsAnimationRunner != null) return
        val fields = obj?.javaClass?.let(::getInstanceFields)
        insetsAnimationRunner = InsetsAnimationControlImplCache(
            mListenerField = fields?.find(name = "mListener").let(::FieldCache),
            mAnimationField = fields?.find(name = "mAnimation").let(::FieldCache)
        )
    }

    fun ensureAnimationControlListener(obj: Any?) {
        if (animationControlListener != null) return
        animationControlListener = InternalAnimationControlListenerCache(
            mDurationMsField = obj?.javaClass?.let(::getInstanceFields)
                ?.find(name = "mDurationMs").let(::FieldCache)
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun getStaticFields(clazz: Class<*>): List<Field> {
        return HiddenApiBypass.getStaticFields(clazz) as List<Field>
    }

    @Suppress("UNCHECKED_CAST")
    private fun getInstanceFields(clazz: Class<*>): List<Field> {
        return HiddenApiBypass.getInstanceFields(clazz) as List<Field>
    }

    private fun List<Field>.find(name: String): Field? = find { it.name == name }
}

private class FieldCache(private val field: Field?) {
    init {
        field?.isAccessible = true
    }

    fun get(obj: Any?): Any? {
        return runCatching { field?.get(obj) }.getOrNull()
    }

    fun set(obj: Any?, value: Any?) {
        runCatching { field?.set(obj, value) }
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
 *     private Impl mImpl;
 * }
 *
 * @RequiresApi(30)
 * private static class Impl30 extends Impl {
 *     @NonNull
 *     private final WindowInsetsAnimation mWrapped;
 * }
 * ```
 */
private class WindowInsetsAnimationCompatCache(
    val mImplField: FieldCache,
    val mWrappedField: FieldCache
)

/**
 * ```
 * public class InsetsController implements WindowInsetsController {
 *     private static final Interpolator SYNC_IME_INTERPOLATOR =
 *             new PathInterpolator(0.2f, 0f, 0f, 1f);
 *     private final ArrayList<RunningAnimation> mRunningAnimations = new ArrayList<>();
 * }
 *
 * private static class RunningAnimation {
 *     final InsetsAnimationControlRunner runner;
 * }
 * ```
 */
@RequiresApi(30)
private class InsetsControllerCache(
    val syncImeInterpolator: FieldCache,
    val mRunningAnimationsField: FieldCache,
    val runnerField: FieldCache
) {
    val initialSyncImeInterpolator: Any? = syncImeInterpolator.get(null)
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