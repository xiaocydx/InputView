package com.xiaocydx.inputview

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * [InputView]编辑区的Fragment适配器，负责创建和通知显示[Editor]的Fragment，
 * [current]的Fragment，生命周期状态是[RESUMED]，其它的Fragment是[STARTED]。
 *
 * ```
 * enum class MessageEditor : Editor {
 *     IME, VOICE, EMOJI
 * }
 *
 * class MessageFragmentEditorAdapter(
 *     lifecycle: Lifecycle,
 *     fragmentManager: FragmentManager
 * ) : FragmentEditorAdapter<MessageEditor>(lifecycle, fragmentManager) {
 *     override val ime = MessageEditor.IME
 *
 *     override fun getEditorKey(editor: MessageEditor) = editor.name
 *
 *     override fun onCreateFragment(editor: MessageEditor): Fragment? {
 *         return when(editor) {
 *             MessageEditor.IME -> null
 *             MessageEditor.VOICE -> VoiceFragment()
 *             MessageEditor.EMOJI -> EmojiFragment()
 *         }
 *     }
 * }
 * ```
 *
 * @author xcc
 * @date 2023/10/11
 */
abstract class FragmentEditorAdapter<T : Editor>(
    private val lifecycle: Lifecycle,
    private val fragmentManager: FragmentManager
) : EditorAdapter<T>() {
    private val fragments = mutableMapOf<T, Fragment?>()
    private val restoreFragments = mutableMapOf<String, Fragment>()
    private val fragmentRestoreEnforcer = FragmentRestoreEnforcer()
    private val fragmentMaxLifecycleEnforcer = FragmentMaxLifecycleEnforcer()

    constructor(fragmentActivity: FragmentActivity) : this(
        lifecycle = fragmentActivity.lifecycle,
        fragmentManager = fragmentActivity.supportFragmentManager
    )

    constructor(fragment: Fragment) : this(
        lifecycle = fragment.lifecycle,
        fragmentManager = fragment.childFragmentManager
    )

    /**
     * 获取[editor]的Key，Key将作为`Fragment.tag`的一部分
     */
    protected abstract fun getEditorKey(editor: T): String

    /**
     * 创建[editor]的Fragment，返回`null`表示不需要Fragment，当[editor]表示IME时，该函数不会被调用
     *
     * 创建的Fragment，在[InputView.editorAnimator]的动画结束时，生命周期状态才会转换为[RESUMED]，
     * 当执行Fragment重建流程时，若[InputView]还未添加到视图树，则移除重建的Fragment，放弃恢复状态。
     *
     * **注意**：重建且可恢复状态的Fragment会被使用，不会调用该函数再次创建Fragment，
     * 在创建Fragment时，不要对Fragment的构造函数传参，重建的Fragment会缺少这些传参，
     * 除非在重建Fragment之前，设置自定义[FragmentFactory]，重新对构造函数进行传参，
     * 不过这种做法有些麻烦，一般不会考虑。
     */
    protected abstract fun onCreateFragment(editor: T): Fragment?

    final override fun onCreateView(parent: ViewGroup, editor: T): View? {
        if (editor === ime) return null
        val tag = "$KEY_PREFIX_FRAGMENT${getEditorKey(editor)}"
        val fragment = restoreFragments.remove(tag) ?: onCreateFragment(editor)
        fragments[editor] = fragment
        if (fragment != null) placeFragmentInContainer(parent, fragment, tag)
        return fragment?.view
    }

    private fun shouldDelayFragmentTransactions() = fragmentManager.isStateSaved

    private fun placeFragmentInContainer(container: ViewGroup, fragment: Fragment, tag: String) {
        // InputView确保在常规布局流程调用onCreateView()
        require(container.id != View.NO_ID) { "container未设置id" }
        require(!shouldDelayFragmentTransactions()) { "当前未处于常规布局流程" }
        var view = fragment.view
        when {
            fragment.isAdded && view == null -> {
                throw IllegalStateException("Fragment生命周期状态转换的异常情况")
            }
            fragment.isAdded && view != null && view.parent != null -> {
                throw IllegalStateException("FragmentRestoreEnforcer未移除View")
            }
            fragment.isAdded && view != null && view.layoutParams == null -> {
                throw IllegalStateException("重建的Fragment未对container添加View")
            }
            !fragment.isAdded && view != null -> {
                throw IllegalStateException("违背常规流程的异常情况")
            }
            !fragment.isAdded && view == null -> {
                // 不使用add(fragment, tag) + 手动添加Fragment.view的方案，
                // 原因是创建view时会丢失layoutParams，直观表现就是丢失尺寸。
                // 使用add(containerViewId, fragment, tag)的方案，其缺陷是
                // 重建流程会将Fragment.view添加到container，不符合当前设计，
                // FragmentRestoreEnforcer在重建完成后，移除Fragment.view。
                fragmentManager.beginTransaction()
                    .add(container.id, fragment, tag)
                    .setMaxLifecycle(fragment, STARTED)
                    .commitNow()
            }
        }
        view = fragment.view
        assert(view != null)
        val parent = view?.parent
        assert(parent == null || parent === container)
        // 移除view，在onCreateView()之后重新添加view，确保跟重建流程表现一致
        view?.let(container::removeView)
    }

    final override fun onAttachToEditorHost(host: EditorHost) {
        super.onAttachToEditorHost(host)
        fragmentRestoreEnforcer.register()
        fragmentMaxLifecycleEnforcer.register(host)
    }

    final override fun onDetachFromEditorHost(host: EditorHost) {
        super.onDetachFromEditorHost(host)
        fragmentRestoreEnforcer.unregister()
        fragmentMaxLifecycleEnforcer.unregister(host)
    }

    private inner class FragmentRestoreEnforcer : LifecycleEventObserver {

        fun register() {
            lifecycle.addObserver(this)
        }

        fun unregister() {
            lifecycle.removeObserver(this)
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            val currentState = source.lifecycle.currentState
            if (shouldDelayFragmentTransactions() || !currentState.isAtLeast(STARTED)) return
            unregister()

            val container = host?.container ?: return
            val toRemove = mutableListOf<Fragment>()
            require(container.id != View.NO_ID) { "container未设置id" }
            fragmentManager.fragments.forEach action@{ fragment ->
                val tag = fragment.tag ?: return@action
                if (!tag.contains(KEY_PREFIX_FRAGMENT)) return@action
                if (fragments.containsValue(fragment)) return@action
                val view = fragment.view
                if (view?.parent === container) {
                    restoreFragments[tag] = fragment
                } else {
                    // view = null或parent = null
                    toRemove.add(fragment)
                }
            }

            if (restoreFragments.isEmpty() && toRemove.isEmpty()) return
            val transaction = fragmentManager.beginTransaction()
            restoreFragments.forEach {
                val fragment = it.value
                fragment.view?.let(container::removeView)
                // 重建的Fragment，生命周期状态可能是RESUMED，
                // 此时未显示Fragment，状态需要回退到STARTED。
                transaction.setMaxLifecycle(it.value, STARTED)
                fragment.setMenuVisibility(false)
            }
            toRemove.forEach { fragment ->
                // 执行重建流程，container还未添加到视图树，
                // 此时重建的Fragment，其view缺少container，
                // view丢失layoutParams，直观表现就是丢失尺寸，
                // 移除重建的Fragment，放弃恢复状态，确保表现一致。
                transaction.remove(fragment)
            }
            transaction.takeIf { !it.isEmpty }?.commitNow()
        }
    }

    private inner class FragmentMaxLifecycleEnforcer {
        private var isAnimationRunning = false
        private var adjustObserver: LifecycleObserver? = null
        private var animationCallback: ReplicableAnimationCallback? = null

        fun register(host: EditorHost) {
            adjustObserver = LifecycleEventObserver { _, _ ->
                // 当动画结束时，可能错过了saveState，不允许提交事务，
                // 因此观察Lifecycle的状态更改，尝试提交事务修正状态。
                updateFragmentMaxLifecycle(host.current)
            }
            animationCallback = object : ReplicableAnimationCallback {
                override fun onAnimationPrepare(previous: Editor?, current: Editor?) {
                    isAnimationRunning = true
                }

                override fun onAnimationEnd(state: AnimationState) {
                    // 当动画结束时，才转换Fragment的生命周期状态，
                    // 目的是对调用者提供一个协调动画卡顿问题的时机，
                    // 例如当Fragment的生命周期状态转换为RESUMED时，
                    // 才设置数据，申请下一帧重新布局，创建大量视图。
                    isAnimationRunning = false
                    updateFragmentMaxLifecycle(state.current)
                }
            }
            adjustObserver?.let(lifecycle::addObserver)
            animationCallback?.let(host::addAnimationCallback)
        }

        fun unregister(host: EditorHost) {
            adjustObserver?.let(lifecycle::removeObserver)
            animationCallback?.let(host::removeAnimationCallback)
            adjustObserver = null
            animationCallback = null
            isAnimationRunning = false
        }

        private fun updateFragmentMaxLifecycle(current: Editor?) {
            if (shouldDelayFragmentTransactions() || isAnimationRunning || fragments.isEmpty()) return
            val transaction = fragmentManager.beginTransaction()
            var toResume: Fragment? = null
            fragments.forEach action@{
                val editor = it.key
                val fragment = it.value
                if (fragment == null || !fragment.isAdded) return@action
                if (editor !== current) {
                    transaction.setMaxLifecycle(fragment, STARTED)
                } else {
                    toResume = fragment
                }
                fragment.setMenuVisibility(editor === current)
            }
            toResume?.let { transaction.setMaxLifecycle(it, RESUMED) }
            transaction.takeIf { !it.isEmpty }?.commitNow()
        }
    }

    private companion object {
        const val KEY_PREFIX_FRAGMENT = "com.xiaocydx.inputview.FragmentEditorAdapter.f#"
    }
}