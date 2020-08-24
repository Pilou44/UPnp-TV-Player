package com.wechantloup.upnpvideoplayer.browse2

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.wechantloup.upnpvideoplayer.R

class SuperBrowseActivity : FragmentActivity() {

    private lateinit var fragment: GridBrowseFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_super_browse)
        fragment = supportFragmentManager.findFragmentById(R.id.main_browse_fragment) as GridBrowseFragment
    }

    override fun onBackPressed() {
        fragment.onBackPressed()
    }
}