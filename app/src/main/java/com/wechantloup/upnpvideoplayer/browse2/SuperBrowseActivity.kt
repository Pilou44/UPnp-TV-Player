package com.wechantloup.upnpvideoplayer.browse2

import android.app.Activity
import android.os.Bundle
import com.wechantloup.upnpvideoplayer.R

class SuperBrowseActivity : Activity() {

    private lateinit var fragment: SuperBrowseFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_super_browse)
        fragment = this.fragmentManager.findFragmentById(R.id.main_browse_fragment) as SuperBrowseFragment
    }

    override fun onBackPressed() {
        fragment.onBackPressed()
    }
}