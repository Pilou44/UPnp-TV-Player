package com.wechantloup.upnpvideoplayer.browse2

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.room.Room
import androidx.lifecycle.lifecycleScope
import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.data.content.AppDatabase
import kotlinx.coroutines.launch

class SuperBrowseActivity : FragmentActivity() {

    private lateinit var fragment: GridBrowseFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_super_browse)
        fragment = supportFragmentManager.findFragmentById(R.id.main_browse_fragment) as GridBrowseFragment

        getUnfinishedVideo()
    }

    private fun getUnfinishedVideo() {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            applicationContext.packageName
        ).build()

        lifecycleScope.launch {
            db.videoDao().all()
        }
    }

    override fun onBackPressed() {
        fragment.onBackPressed()
    }
}