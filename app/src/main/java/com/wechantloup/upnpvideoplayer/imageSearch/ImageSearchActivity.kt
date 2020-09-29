package com.wechantloup.upnpvideoplayer.imageSearch

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.data.repository.ImageSearchRepository

class ImageSearchActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("TEST", "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_search)

        val search = requireNotNull(intent.getStringExtra(ARG_SEARCH))

        val repository = ImageSearchRepository()

        lifecycleScope.launchWhenCreated {
            val images = repository.getImages(search)
        }
    }

    companion object {
        const val ARG_SEARCH = "search"
    }
}