package com.wechantloup.upnpvideoplayer.imageSearch

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.wechantloup.upnpvideoplayer.R

class ImageSearchActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_search)

        val searchEditText = findViewById<TextView>(R.id.search_edit_text)

        val search = requireNotNull(intent.getStringExtra(ARG_SEARCH))

        searchEditText.setText(search)

        val fragment = supportFragmentManager.findFragmentById(R.id.image_search_fragment) as? ImageSearchFragment
        fragment?.search(search)
    }

    companion object {
        const val ARG_SEARCH = "search"
    }
}
