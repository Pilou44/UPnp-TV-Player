package com.wechantloup.upnpvideoplayer.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.wechantloup.upnpvideoplayer.MainActivity
import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.browse.BrowseActivity
import com.wechantloup.upnpvideoplayer.browse2.SuperBrowseActivity
import com.wechantloup.upnpvideoplayer.rootSetter.RootSetterActivity

class MainActivity : Activity() {

    private lateinit var browse: Button
    private lateinit var browse2: Button
    private lateinit var root: Button
    private lateinit var test: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_root)

        browse = findViewById(R.id.browse)
        browse2 = findViewById(R.id.browse2)
        root = findViewById(R.id.root)
        test = findViewById(R.id.test)

        browse.setOnClickListener {
            launchBrowser()
        }

        browse2.setOnClickListener {
            launchNewBrowser()
        }
        root.setOnClickListener {
            launchRootSetter()
        }
        test.setOnClickListener {
            launchTest()
        }
    }

    private fun launchBrowser() {
        val intent = Intent(this, BrowseActivity::class.java)
        startActivity(intent)
    }

    private fun launchNewBrowser() {
        val intent = Intent(this, SuperBrowseActivity::class.java)
        startActivity(intent)
    }

    private fun launchRootSetter() {
        val intent = Intent(this, RootSetterActivity::class.java)
        startActivity(intent)
    }

    private fun launchTest() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}