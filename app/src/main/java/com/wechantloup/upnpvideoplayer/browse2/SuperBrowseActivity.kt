package com.wechantloup.upnpvideoplayer.browse2

import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.dialog.DialogFragment

class SuperBrowseActivity : FragmentActivity() {

    private val DIALOG_FRAGMENT: String = "dialog_fragment"
    private lateinit var fragment: GridBrowseFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_super_browse)
        fragment = supportFragmentManager.findFragmentById(R.id.main_browse_fragment) as GridBrowseFragment
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (fragment.onKeyDown(keyCode, event)) true
        else super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {
        fragment.onBackPressed()
    }

    fun showDialog(params: DialogFragment.Params) {
        val dialogFragment: DialogFragment = DialogFragment()
        dialogFragment.bind(params)
        supportFragmentManager.beginTransaction().add(android.R.id.content, dialogFragment, DIALOG_FRAGMENT).commit()
    }

    fun removeDialog() {
        val dialogFragment = supportFragmentManager.findFragmentByTag(DIALOG_FRAGMENT)
        dialogFragment?.let {
            supportFragmentManager.beginTransaction().remove(dialogFragment).commit()
        }
    }
}