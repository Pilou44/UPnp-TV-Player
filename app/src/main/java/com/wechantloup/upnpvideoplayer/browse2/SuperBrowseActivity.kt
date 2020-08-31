package com.wechantloup.upnpvideoplayer.browse2

import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.dialog.DialogFragment
import com.wechantloup.upnpvideoplayer.dialog.DialogFragmentActivity

class SuperBrowseActivity : DialogFragmentActivity() {

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
}