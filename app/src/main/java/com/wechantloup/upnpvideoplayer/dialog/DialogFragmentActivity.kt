package com.wechantloup.upnpvideoplayer.dialog

import androidx.fragment.app.FragmentActivity

open class DialogFragmentActivity : FragmentActivity() {

    fun showDialog(params: DialogFragment.Params) {
        val dialogFragment = DialogFragment()
        dialogFragment.bind(params)
        supportFragmentManager.beginTransaction().add(android.R.id.content, dialogFragment,
            TAG_DIALOG_FRAGMENT
        ).commit()
    }

    fun removeDialog() {
        val dialogFragment = supportFragmentManager.findFragmentByTag(TAG_DIALOG_FRAGMENT)
        dialogFragment?.let {
            supportFragmentManager.beginTransaction().remove(dialogFragment).commit()
        }
    }

    companion object {
        private const val TAG_DIALOG_FRAGMENT: String = "dialog_fragment"
    }
}