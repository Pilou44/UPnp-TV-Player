package com.wechantloup.upnpvideoplayer.browse2

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.ViewModelProvider
import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.UPnPApplication
import com.wechantloup.upnpvideoplayer.data.dataholder.BrowsableElement
import com.wechantloup.upnpvideoplayer.data.dataholder.BrowsableVideoElement
import com.wechantloup.upnpvideoplayer.data.dataholder.ContainerElement
import com.wechantloup.upnpvideoplayer.data.dataholder.DlnaRoot
import com.wechantloup.upnpvideoplayer.data.dataholder.ParametersElement
import com.wechantloup.upnpvideoplayer.data.dataholder.StartedVideoElement
import com.wechantloup.upnpvideoplayer.data.dataholder.VideoElement
import com.wechantloup.upnpvideoplayer.dialog.DialogFragment
import com.wechantloup.upnpvideoplayer.dialog.DialogFragmentActivity
import com.wechantloup.upnpvideoplayer.main.MainActivity
import com.wechantloup.upnpvideoplayer.rootSetter.RootSetterActivity
import com.wechantloup.upnpvideoplayer.rootSetter.RootSetterActivity.Companion.ARG_ROOT
import com.wechantloup.upnpvideoplayer.utils.Serializer.deserialize
import com.wechantloup.upnpvideoplayer.videoPlayer.VideoPlayerActivity
import com.wechantloup.upnpvideoplayer.widgets.BrowseTitleView

class GridBrowseFragment : VerticalGridSupportFragment(), BrowseContract.View {

    private lateinit var viewModel: BrowseContract.ViewModel
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
        viewModel.onViewResumed(requireContext())
    }

    override fun onPause() {
        super.onPause()
        viewModel.onViewPaused(requireContext())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_MEDIA_PLAYER) {
            if (resultCode == RESULT_OK) {
                val lastPlayedElement: VideoElement.ParcelableElement? =
                    data?.getParcelableExtra(VideoPlayerActivity.ELEMENT)
                lastPlayedElement?.let {
                    viewModel.setLastPlayedElement(lastPlayedElement)
                }
            }
        } else if (requestCode == REQUEST_SETTING_DLNA_ROOT) {
            if (resultCode == RESULT_OK) {
                val newRoot: DlnaRoot = requireNotNull(data?.getStringExtra(ARG_ROOT)?.deserialize())
                viewModel.resetRoot(newRoot)
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
        adapter = ArrayObjectAdapter(CardPresenter(viewModel))

        titleView = BrowseTitleView(requireContext())

        setOnItemViewClickedListener { _, item, _, _ -> onItemClicked(item) }
    }

    private fun onItemClicked(item: Any) {
        when (item) {
            is ContainerElement -> viewModel.parse(item)
            is BrowsableVideoElement -> onBrowsableElementClicked(item)
            is StartedVideoElement -> launchAndContinue(item)
            is ParametersElement -> launcParameter(item)
        }
    }

    private fun launcParameter(item: ParametersElement) {
        startActivityForResult(item.intent, item.requestCode)
    }

    private fun onBrowsableElementClicked(item: BrowsableVideoElement) {val pendingElement = item.hasBeenStarted()
        pendingElement?.let {
            val option1 = DialogFragment.Option(R.string.restart_dialog_button_positive) { launchAndContinue(it) }
            val option2 = DialogFragment.Option(R.string.restart_dialog_button_negative) { launchFromStart(it)}
            val optionList = listOf(option1, option2)
            val params = DialogFragment.Params(
                R.string.restart_dialog_title,
                R.string.restart_dialog_description,
                optionList
            )
            (activity as DialogFragmentActivity).showDialog(params)
            return
        }
        viewModel.launch(item)
    }

    private fun launchAndContinue(element: StartedVideoElement) {
        viewModel.launch(element, element.position)
    }

    private fun launchFromStart(element: StartedVideoElement) {
        viewModel.launch(element)
    }

    override fun launch(movies: ArrayList<VideoElement.ParcelableElement>, index: Int, position: Long) {
        Log.i(TAG, "Launch ${movies[index].name}")
        val intent = Intent(activity, VideoPlayerActivity::class.java)
        intent.putExtra(VideoPlayerActivity.EXTRA_URLS, movies)
        intent.putExtra(VideoPlayerActivity.EXTRA_INDEX, index)
        intent.putExtra(VideoPlayerActivity.EXTRA_POSITION, position)
        startActivityForResult(intent, REQUEST_MEDIA_PLAYER)
    }

    override fun refreshItem(item: Any) {
        activity?.runOnUiThread {
            val index = browsingAdapter.indexOf(item)
            if (index >= 0) {
                browsingAdapter.notifyArrayItemRangeChanged(index, 1)
            }
        }
    }

    override fun displayContent(
        title: String,
        startedMovies: List<StartedVideoElement>,
        directories: List<ContainerElement>,
        movies: List<BrowsableVideoElement>,
        selectedElement: BrowsableElement?
    ) {
        this.title = title
        val adapter = ArrayObjectAdapter(CardPresenter(viewModel))

        adapter.addAll(startedMovies)
        adapter.completeLine()

        adapter.addAll(directories)
        adapter.completeLine()

        adapter.addAll(movies)
        adapter.completeLine()
        
        adapter.addAll(getParameters())

        Log.i(TAG, "Selected: $selectedElement")
        var pos = getInitialPosition(adapter)
        selectedElement?.let { pos = adapter.indexOf(it) }
        setSelectedPosition(pos)
        showTitle(pos < NUM_COLUMNS)

        this.adapter = adapter
    }

    private fun getParameters(): List<ParametersElement> {
        val root = ParametersElement(
            R.string.setting_title_dlna_root,
            R.color.card_background_dlna_root,
            R.drawable.ic_setting_dlna_root,
            Intent(context, RootSetterActivity::class.java),
            REQUEST_SETTING_DLNA_ROOT
        )
        val play = ParametersElement(
            R.string.setting_title_play,
            R.color.card_background_dlna_root,
            R.drawable.ic_setting_player,
            Intent(activity, MainActivity::class.java),
            REQUEST_SETTING_PLAYER_OPTIONS
        )
        return listOf(root, play)
    }

    private fun getInitialPosition(newAdapter: ArrayObjectAdapter): Int {
        return if (adapter.size() == 0) {
            0
        } else {
            newAdapter.unmodifiableList<Any>().indexOfFirst { it is ContainerElement || it is BrowsableVideoElement }
        }
    }

    private fun <E> ArrayObjectAdapter.addAll(items: Collection<E>) {
        addAll(size(), items)
    }

    private fun BrowsableVideoElement.hasBeenStarted(): StartedVideoElement? {
        val started = browsingAdapter.unmodifiableList<Any>().filterIsInstance<StartedVideoElement>()
        return started.firstOrNull { it.path == path }
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode != KeyEvent.KEYCODE_MENU) return false
        // ToDo
        return false
    }

    private fun ArrayObjectAdapter.completeLine() {
        val lastLine = size() % NUM_COLUMNS
        if (lastLine > 0) {
            val rest = NUM_COLUMNS - lastLine
            repeat(rest) { add(Any()) }
        }
    }

    companion object {
        private val TAG = GridBrowseFragment::class.java.simpleName
        private const val NUM_COLUMNS = 5
        private const val REQUEST_MEDIA_PLAYER = 2911
        private const val REQUEST_SETTING_DLNA_ROOT = 1507
        private const val REQUEST_SETTING_PLAYER_OPTIONS = 410
    }
}
