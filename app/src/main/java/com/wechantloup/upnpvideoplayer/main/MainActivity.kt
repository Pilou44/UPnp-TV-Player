package com.wechantloup.upnpvideoplayer.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Button
import android.widget.CheckBox
import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.browse2.SuperBrowseActivity
import com.wechantloup.upnpvideoplayer.rootSetter.RootSetterActivity

class MainActivity : Activity() {

    private lateinit var root: Button
    private lateinit var test: Button
    private lateinit var next: CheckBox
    private lateinit var loop: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_root)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        root = findViewById(R.id.root)
        test = findViewById(R.id.test)
        next = findViewById(R.id.go_next)
        loop = findViewById(R.id.loop)

        root.setOnClickListener {
            launchRootSetter()
        }
        test.setOnClickListener {
            launchTest()
        }

        next.isChecked = prefs.getBoolean("next", true)
        next.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("next", isChecked).apply()
        }
        loop.isChecked = prefs.getBoolean("loop", false)
        loop.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("loop", isChecked).apply()
        }

    }

    override fun onBackPressed() {
        launchNewBrowser()
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