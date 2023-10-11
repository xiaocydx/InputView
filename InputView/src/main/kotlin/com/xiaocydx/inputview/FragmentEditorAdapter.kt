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
import androidx.lifecycle.LifecycleOwner

/**
 * [InputView]编辑区[Editor]的Fragment适配器，负责创建和通知显示[Editor]的Fragment
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
    private val fragmentMaxLifecycleEnforcer = FragmentMaxLifecycleEnforcer()
    private val fragments = mutableMapOf<T, Fragment?>()

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
     * 创建的Fragment，在[InputView.editorAnimator]的动画结束时，生命周期状态才会转换为[RESUMED]。
     *
     * **注意**：[editor]重建后的Fragment会被直接使用，不会调用该函数重新创建Fragment，
     * 在创建Fragment时，不要对Fragment的构造函数传参，重建后的Fragment会缺少这些传参，
     * 除非在重建Fragment之前，设置自定义的[FragmentFactory]，重新对构造函数进行传参，
     * 不过这种做法有些麻烦，一般不会考虑。
     */
    protected abstract fun onCreateFragment(editor: T): Fragment?

    final override fun createView(parent: ViewGroup, editor: T): CreateResult {
        return CreateResult(onCreateView(parent, editor), isAdded = true)
    }

    final override fun onCreateView(parent: ViewGroup, editor: T): View? {
        if (editor === ime) return null
        val tag = "$KEY_PREFIX_FRAGMENT${getEditorKey(editor)}"
        val fragment = fragmentManager.findFragmentByTag(tag) ?: onCreateFragment(editor)
        fragments[editor] = fragment
        if (fragment != null) placeFragment(parent, fragment, tag)
        return fragment?.view
    }

    private fun shouldDelayFragmentTransactions() = fragmentManager.isStateSaved

    private fun placeFragment(container: ViewGroup, fragment: Fragment, tag: String) {
        require(container.id != View.NO_ID) { "container未设置id" }
        // FIXME: onCreateView执行时机能否做到绝对的保证？
        require(!shouldDelayFragmentTransactions()) { "当前未处于布局流程" }
        val view = fragment.view
        when {
            fragment.isAdded && view != null -> when {
                view.parent != null -> throw IllegalStateException("container未移除子View")
                view.layoutParams == null -> throw IllegalStateException("container未添加过子View")
                else -> container.addView(view)
            }
            fragment.isAdded && view == null -> {
                fragmentManager.beginTransaction()
                    .setMaxLifecycle(fragment, STARTED)
                    .commitNow()
            }
            !fragment.isAdded -> if (view != null) {
                throw IllegalStateException("不符合设计的异常情况")
            } else {
                fragmentManager.beginTransaction()
                    .add(container.id, fragment, tag)
                    .setMaxLifecycle(fragment, STARTED)
                    .commitNow()
            }
        }
        requireNotNull(fragment.view?.parent) { "添加fragment.view失败" }
    }

    final override fun onAttachToEditorHost(host: EditorHost) {
        super.onAttachToEditorHost(host)
        fragmentMaxLifecycleEnforcer.register(host, lifecycle)
    }

    final override fun onDetachFromEditorHost(host: EditorHost) {
        super.onDetachFromEditorHost(host)
        fragmentMaxLifecycleEnforcer.unregister(host, lifecycle)
    }

    private inner class FragmentMaxLifecycleEnforcer :
            ReplicableAnimationCallback, LifecycleEventObserver {
        private var isAnimationRunning = false

        fun register(host: EditorHost, lifecycle: Lifecycle) {
            host.addAnimationCallback(this)
            lifecycle.addObserver(this)
        }

        fun unregister(host: EditorHost, lifecycle: Lifecycle) {
            host.removeAnimationCallback(this)
            lifecycle.removeObserver(this)
        }

        override fun onAnimationPrepare(previous: Editor?, current: Editor?) {
            isAnimationRunning = true
        }

        override fun onAnimationEnd(state: AnimationState) {
            // 在动画结束时，才转换Fragment的生命周期状态，
            // 目的是对调用者提供一个协调动画卡顿问题的时机，
            // 例如在Fragment的生命周期状态转换为RESUMED时，
            // 才设置数据，申请下一帧重新布局，创建大量视图。
            isAnimationRunning = false
            updateFragmentMaxLifecycle(state.current)
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            updateFragmentMaxLifecycle(host?.current)
        }

        fun updateFragmentMaxLifecycle(current: Editor?) {
            if (shouldDelayFragmentTransactions() || isAnimationRunning || fragments.isEmpty()) return
            val transaction = fragmentManager.beginTransaction()
            var toResume: Fragment? = null
            fragments.forEach action@{
                val editor = it.key
                val fragment = it.value
                if (fragment == null || !fragment.isAdded) return@action
                if (editor != current) {
                    transaction.setMaxLifecycle(fragment, STARTED)
                } else {
                    toResume = fragment
                }
                fragment.setMenuVisibility(editor == current)
            }
            toResume?.let { transaction.setMaxLifecycle(it, RESUMED) }
            transaction.takeIf { !it.isEmpty }?.commitNow()
        }
    }

    private companion object {
        const val KEY_PREFIX_FRAGMENT = "f#"
    }
}