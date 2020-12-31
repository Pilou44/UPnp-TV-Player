package com.wechantloup.upnpvideoplayer.imageSearch

import android.os.Bundle
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.lifecycleScope
import com.wechantloup.upnpvideoplayer.browse2.CardPresenter
import com.wechantloup.upnpvideoplayer.browse2.GridBrowseFragment
import com.wechantloup.upnpvideoplayer.data.repository.ImageSearchRepository
import com.wechantloup.upnpvideoplayer.widgets.BrowseTitleView
import kotlinx.coroutines.launch

class ImageSearchFragment : VerticalGridSupportFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        initViewModel()
        setupFragment()
    }

    fun search(search: String) {
        val repository = ImageSearchRepository()

        lifecycleScope.launch {
            val images = repository.getImages(search)
            (adapter as ArrayObjectAdapter).addAll(0, images.value)
            adapter.notifyItemRangeChanged(0, images.value.size)
        }
    }

    private fun setupFragment() {
        val gridPresenter = VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_LARGE, false)
        gridPresenter.numberOfColumns = NUM_COLUMNS
        setGridPresenter(gridPresenter)
        adapter = ArrayObjectAdapter(ImageSearchPresenter())

        titleView = BrowseTitleView(requireContext())

//        setOnItemViewClickedListener { _, item, _, _ -> onItemClicked(item) }
//        setOnItemViewSelectedListener { _, item, _, _ -> selectedElement = item }
    }

    companion object {
        private const val TAG = "ImageSearchFragment"

        private const val NUM_COLUMNS = 5
    }
}
