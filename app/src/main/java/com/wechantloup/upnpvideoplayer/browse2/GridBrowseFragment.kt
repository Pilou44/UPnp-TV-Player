package com.wechantloup.upnpvideoplayer.browse2

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.ViewModelProvider
import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.UPnPApplication
import com.wechantloup.upnpvideoplayer.data.dataholder.BrowsableVideoElement
import com.wechantloup.upnpvideoplayer.data.dataholder.VideoElement
import com.wechantloup.upnpvideoplayer.dialog.DialogActivity
import com.wechantloup.upnpvideoplayer.main.MainActivity
import com.wechantloup.upnpvideoplayer.videoPlayer.VideoPlayerActivity

class GridBrowseFragment : VerticalGridSupportFragment(), BrowseContract.View {

    private var dialogDisplayed: Boolean = false
    private var pendingElement: VideoElement? = null
    private lateinit var viewModel: BrowseContract.ViewModel
    private val videos = ArrayList<BrowsableVideoElement>()
    private var lastPlayedElement: BrowsableVideoElement? = null
    private lateinit var browsingAdapter: ArrayObjectAdapter

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
        if (!dialogDisplayed) {
            viewModel.onViewResumed(requireContext())
        }
        dialogDisplayed = false
    }

    override fun onPause() {
        super.onPause()
        if (!dialogDisplayed) {
            viewModel.onViewPaused(requireContext())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_MEDIA_PLAYER) {
            if (resultCode == RESULT_OK)
                lastPlayedElement = data?.getParcelableExtra(VideoPlayerActivity.ELEMENT)
        } else if (requestCode == REQUEST_DIALOG_STARTED_ELEMENT) {
            pendingElement?.let {
                if (resultCode == DialogActivity.ACTION_ID_POSITIVE) {
                    launchAndContinue(it)
                } else {
                    launchFromStart(it)
                }
                pendingElement = null
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun onBackPressed() {
        if (viewModel.goBack()) return

        activity?.finish()
    }

    override fun setAdapter(adapter: ObjectAdapter?) {
        super.setAdapter(adapter)
        browsingAdapter = adapter as ArrayObjectAdapter
    }

    private fun setupFragment() {
        val gridPresenter = VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_LARGE, false)
        gridPresenter.numberOfColumns = NUM_COLUMNS
        setGridPresenter(gridPresenter)
        adapter = ArrayObjectAdapter(CardPresenter())

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
                pendingElement = item.hasBeenStarted()
                pendingElement?.let {
                    val params = DialogActivity.Params(
                        REQUEST_DIALOG_STARTED_ELEMENT,
                        R.string.restart_dialog_title,
                        R.string.restart_dialog_description,
                        R.string.restart_dialog_button_positive,
                        R.string.restart_dialog_button_negative
                    )
                    dialogDisplayed = true
                    DialogActivity.launch(this, params)
                    return
                }
                val index = videos.indexOf(item)
                launch(videos, index, 0L)
            }
        } else if (item is VideoElement) {
            launchAndContinue(item)
        }
    }

    private fun launchAndContinue(element: VideoElement) {
        viewModel.convertToBrowsableVideoElement(element)
    }

    private fun launchFromStart(element: VideoElement) {
        val copy = element.copy(position = 0L)
        viewModel.convertToBrowsableVideoElement(copy)
    }

    override fun launch(movies: ArrayList<BrowsableVideoElement>, index: Int, position: Long) {
        val intent = Intent(activity, VideoPlayerActivity::class.java)
        intent.putExtra(VideoPlayerActivity.EXTRA_URLS, movies)
        intent.putExtra(VideoPlayerActivity.EXTRA_INDEX, index)
        intent.putExtra(VideoPlayerActivity.EXTRA_POSITION, position)
        startActivityForResult(intent, REQUEST_MEDIA_PLAYER)
    }

    override fun updateStarted(startedMovies: List<VideoElement>) {
        var count = 0
        while (count < browsingAdapter.size() && browsingAdapter[count] !is BrowsableVideoElement) {
            count++
        }
        if (count == browsingAdapter.size()) return
        browsingAdapter.removeItems(0, count)
        browsingAdapter.addAll(0, startedMovies)

        val lastLine = startedMovies.size % NUM_COLUMNS
        if (lastLine > 0) {
            val rest = NUM_COLUMNS - lastLine
            repeat(rest) { browsingAdapter.add(startedMovies.size, Any()) }
        }

        lastPlayedElement?.let { element ->
            val pos = browsingAdapter.unmodifiableList<Any>().indexOfFirst {
                when (it) {
                    is VideoElement -> it.path == element.path
                    is BrowsableVideoElement -> it == element
                    else -> false
                }
            }
            setSelectedPosition(pos)
            showTitle(pos < NUM_COLUMNS)
            lastPlayedElement = null
        }
    }

    override fun displayContent(
        title: String,
        startedMovies: List<VideoElement>,
        directories: List<BrowsableVideoElement>,
        movies: List<BrowsableVideoElement>,
        selectedElement: BrowsableVideoElement?
    ) {
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

        var pos = getInitialPosition(adapter)
        selectedElement?.let { pos = adapter.indexOf(it) }
        setSelectedPosition(pos)
        showTitle(pos < NUM_COLUMNS)

        this.adapter = adapter
    }

    private fun getInitialPosition(newAdapter: ArrayObjectAdapter): Int {
        if (adapter.size() == 0) {
            return 0
        } else {
            return newAdapter.unmodifiableList<Any>().indexOfFirst { it is BrowsableVideoElement }
        }
    }

    private fun <E> ArrayObjectAdapter.addAll(items: Collection<E>) {
        addAll(size(), items)
    }

    private fun BrowsableVideoElement.hasBeenStarted(): VideoElement? {
        val started = browsingAdapter.unmodifiableList<Any>().filterIsInstance<VideoElement>()
        return started.firstOrNull { it.path == path }
    }

    companion object {
        private val TAG = GridBrowseFragment::class.java.simpleName
        private const val NUM_COLUMNS = 5
        private const val REQUEST_MEDIA_PLAYER = 2911
        private const val REQUEST_DIALOG_STARTED_ELEMENT = 1507
    }
}