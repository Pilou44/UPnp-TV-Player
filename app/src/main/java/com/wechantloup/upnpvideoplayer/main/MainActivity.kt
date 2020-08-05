package com.wechantloup.upnpvideoplayer.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.browse.BrowseActivity
import com.wechantloup.upnpvideoplayer.rootSetter.RootSetterActivity

class MainActivity : Activity() {

    private lateinit var browse: Button
    private lateinit var root: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_root)

        browse = findViewById(R.id.browse)
        root = findViewById(R.id.root)

        browse.setOnClickListener {
            launchBrowser()
        }
        root.setOnClickListener {
            launchRootSetter()
        }
    }

    private fun launchBrowser() {
        val intent = Intent(this, BrowseActivity::class.java)
        startActivity(intent)
    }

    private fun launchRootSetter() {
        val intent = Intent(this, RootSetterActivity::class.java)
        startActivity(intent)
    }
}