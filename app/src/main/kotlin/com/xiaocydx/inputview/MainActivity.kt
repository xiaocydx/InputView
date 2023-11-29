package com.xiaocydx.inputview

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.xiaocydx.inputview.dialog.MessageListBottomSheetDialog
import com.xiaocydx.inputview.dialog.MessageListBottomSheetDialogFragment
import com.xiaocydx.inputview.dialog.MessageListDialog
import com.xiaocydx.inputview.dialog.MessageListDialogFragment
import com.xiaocydx.inputview.fragment.FragmentEditorAdapterActivity

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
        setContentView(R.layout.activity_main)
    }

    fun startOverlayInputActivity(view: View) {
        startActivity(Intent(this, OverlayInputActivity::class.java))
    }

    fun startMessageListActivity(view: View) {
        startActivity(Intent(this, MessageListActivity::class.java))
    }

    fun showMessageListDialog(view: View) {
        MessageListDialog(this).show()
    }

    fun showMessageListDialogFragment(view: View) {
        MessageListDialogFragment().show(supportFragmentManager, null)
    }

    fun showMessageListBottomSheetDialog(view: View) {
        MessageListBottomSheetDialog(this).show()
    }

    fun showMessageListBottomSheetDialogFragment(view: View) {
        MessageListBottomSheetDialogFragment().show(supportFragmentManager, null)
    }

    fun startFragmentEditorAdapterActivity(view: View) {
        startActivity(Intent(this, FragmentEditorAdapterActivity::class.java))
    }
}