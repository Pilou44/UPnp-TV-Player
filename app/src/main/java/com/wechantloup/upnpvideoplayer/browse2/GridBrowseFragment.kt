package com.wechantloup.upnpvideoplayer.browse2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.ViewModelProvider
import com.wechantloup.upnpvideoplayer.UPnPApplication
import com.wechantloup.upnpvideoplayer.data.dataholder.BrowsableVideoElement
import com.wechantloup.upnpvideoplayer.data.dataholder.VideoElement
import com.wechantloup.upnpvideoplayer.main.MainActivity
import com.wechantloup.upnpvideoplayer.videoPlayer.VideoPlayerActivity

class GridBrowseFragment : VerticalGridSupportFragment(), BrowseContract.View {

    private lateinit var viewModel: BrowseContract.ViewModel
    private val videos = ArrayList<BrowsableVideoElement>()
    private val mHandler = Handler()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initViewModel()
        setupFragment()
    }

    private fun initViewModel() {
        val application = (requireContext().applicationContext) as UPnPApplication
        BrowseViewModelFactory.createViewModelFactory(
            application
        )
            .let { ViewModelProvider(viewModelStore, it)[BrowseViewModel::class.java] }
            .apply { setView(this@GridBrowseFragment) }
            .also { viewModel = it }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onViewResumed(requireContext())
    }

    override fun onPause() {
        super.onPause()
        viewModel.onViewPaused(requireContext())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == MEDIA_PLAYER) {
            val lastPlayedElement = data?.getParcelableExtra<BrowsableVideoElement>(VideoPlayerActivity.ELEMENT)
            lastPlayedElement?.let {
                val pos = (adapter as ArrayObjectAdapter).indexOf(it)
                setSelectedPosition(pos)
                showTitle(pos < NUM_COLUMNS)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun onBackPressed() {
        if (viewModel.goBack()) return

        activity?.finish()
    }

    private fun setupFragment() {
        val gridPresenter = VerticalGridPresenter()
        gridPresenter.numberOfColumns = NUM_COLUMNS
        setGridPresenter(gridPresenter)
        val mAdapter = ArrayObjectAdapter(CardPresenter())

        adapter = mAdapter
        setOnSearchClickedListener {
            val intent = Intent(activity, MainActivity::class.java)
            startActivity(intent)
            activity?.finish()
        }
        titleView = BrowseTitleView(requireContext())

        setOnItemViewClickedListener { _, item, _, _ -> onItemClicked(item) }
    }

    private fun onItemClicked(item: Any) {
        if (item is BrowsableVideoElement) {
            if (item.isDirectory) {
                viewModel.parse(item)
            } else {
                val index = videos.indexOf(item)
                launch(videos, index, 0L)
            }
        } else if (item is VideoElement) {
            viewModel.convertToBrowsableVideoElement(item)
        }
    }

    override fun launch(movies: ArrayList<BrowsableVideoElement>, index: Int, position: Long) {
        val intent = Intent(activity, VideoPlayerActivity::class.java)
        intent.putExtra(VideoPlayerActivity.EXTRA_URLS, movies)
        intent.putExtra(VideoPlayerActivity.EXTRA_INDEX, index)
        intent.putExtra(VideoPlayerActivity.EXTRA_POSITION, position)
        startActivityForResult(intent, MEDIA_PLAYER)
    }

    override fun updateStarted(startedMovies: List<VideoElement>) {
        val adapter = adapter as ArrayObjectAdapter
        var count = 0
        while (count < adapter.size() && adapter[count] !is BrowsableVideoElement) {
            count++
        }
        if (count == adapter.size()) return
        adapter.removeItems(0, count)
        adapter.addAll(0, startedMovies)

        val lastLine = startedMovies.size % NUM_COLUMNS
        if (lastLine > 0) {
            val rest = NUM_COLUMNS - lastLine
            repeat(rest) { adapter.add(startedMovies.size, Any()) }
        }
    }

    override fun displayContent(
        title: String,
        startedMovies: List<VideoElement>,
        directories: List<BrowsableVideoElement>,
        movies: List<BrowsableVideoElement>,
        selectedElement: BrowsableVideoElement?
    ) {
        mHandler.post {
            this.title = title
            val adapter = ArrayObjectAdapter(CardPresenter())

            adapter.addAll(startedMovies)

            var lastLine = startedMovies.size % NUM_COLUMNS
            if (lastLine > 0) {
                val rest = NUM_COLUMNS - lastLine
                repeat(rest) { adapter.add(Any()) }
            }

            adapter.addAll(directories)

            lastLine = directories.size % NUM_COLUMNS
            if (lastLine > 0) {
                val rest = NUM_COLUMNS - lastLine
                repeat(rest) { adapter.add(Any()) }
            }

            videos.clear()
            videos.addAll(movies)
            adapter.addAll(movies)

            var pos = 0
            selectedElement?.let { pos = adapter.indexOf(it) }
            setSelectedPosition(pos)
            showTitle(pos < NUM_COLUMNS)

            this.adapter = adapter
        }
    }

    private fun <E> ArrayObjectAdapter.addAll(items: Collection<E>) {
        addAll(size(), items)
    }

    companion object {
        private val TAG = GridBrowseFragment::class.java.simpleName
        private const val NUM_COLUMNS = 5
        private const val MEDIA_PLAYER = 2911
    }
}