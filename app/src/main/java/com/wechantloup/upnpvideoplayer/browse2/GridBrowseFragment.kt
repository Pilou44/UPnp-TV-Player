package com.wechantloup.upnpvideoplayer.browse2

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import androidx.leanback.app.VerticalGridFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.VerticalGridPresenter
import com.wechantloup.upnpvideoplayer.browse.RetrieveDeviceThread
import com.wechantloup.upnpvideoplayer.browse.RetrieveDeviceThreadListener
import com.wechantloup.upnpvideoplayer.dataholder.DlnaRoot
import com.wechantloup.upnpvideoplayer.dataholder.VideoElement
import com.wechantloup.upnpvideoplayer.utils.Serializer.deserialize
import com.wechantloup.upnpvideoplayer.videoPlayer.VideoPlayerActivity
import org.fourthline.cling.android.AndroidUpnpService
import org.fourthline.cling.android.AndroidUpnpServiceImpl
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.LocalDevice
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.meta.RemoteService
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import org.fourthline.cling.support.contentdirectory.callback.Browse
import org.fourthline.cling.support.model.BrowseFlag
import org.fourthline.cling.support.model.DIDLContent

class GridBrowseFragment : VerticalGridFragment(), RetrieveDeviceThreadListener {

    private val videos = mutableListOf<VideoElement>()
    private val mHandler = Handler()
    var mRoot = "root"
    private var bound = false
    private lateinit var remoteService: RemoteService
    var mCurrent: VideoElement? = null
    private var mUpnpService: AndroidUpnpService? = null
    private val mRegistryListener: BrowseRegistryListener =
        BrowseRegistryListener(this)

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.i(TAG, "Service connected")
            val upnpService = (service as AndroidUpnpService).also { mUpnpService = it }

            // Get ready for future device advertisements
            upnpService.registry.addListener(mRegistryListener)
            findDevice()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            mUpnpService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        //        ContentContainer mContentContainer = ContentBrowser.getInstance(getActivity())
//                                                           .getLastSelectedContentContainer();
//        setTitle(mContentContainer.getName());
        setTitle("Toto")
        setupFragment()
    }

    override fun onResume() {
        super.onResume()

        // This will start the UPnP service if it wasn't already started
        Log.i(TAG, "Start UPnP Service")
        activity.applicationContext.bindService(
            Intent(activity, AndroidUpnpServiceImpl::class.java),
            mServiceConnection,
            Context.BIND_AUTO_CREATE
        )
        bound = true
    }

    override fun onPause() {
        super.onPause()
        mUpnpService?.registry?.removeListener(mRegistryListener)
        // This will stop the UPnP service if nobody else is bound to it
        if (bound) {
            activity.applicationContext.unbindService(mServiceConnection)
        }
    }

    override fun onDeviceNotFound() {
        TODO("Not yet implemented")
    }

    fun onBackPressed() {
        if (mCurrent == null || mCurrent!!.path == mRoot) {
            activity.finish()
        } else {
            parseAndUpdate(mCurrent!!.parent, mCurrent)
        }
    }

    private fun setupFragment() {
        val gridPresenter = VerticalGridPresenter()
        gridPresenter.numberOfColumns = NUM_COLUMNS
        setGridPresenter(gridPresenter)
        val mAdapter = ArrayObjectAdapter(CardPresenter())

//        ContentContainer contentContainer = ContentBrowser.getInstance(getActivity())
//                                                          .getLastSelectedContentContainer();
//        for (Content content : contentContainer) {
//            mAdapter.add(content);
//        }
        setAdapter(mAdapter)

        setOnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row -> onItemClicked(item) }

//        setOnItemViewClickedListener((itemViewHolder, item, rowViewHolder, row) -> {
//            Log.i(TAG, "item clicked: " + ((Content) item).getTitle());
//            if (item instanceof Content) {
//                Content content = (Content) item;
//                Log.d(TAG, "Content with title " + content.getTitle() + " was clicked");
//
//                ContentBrowser.getInstance(getActivity())
//                              .setLastSelectedContent(content)
//                              .switchToScreen(ContentBrowser.CONTENT_DETAILS_SCREEN, content);
//            }
//        });
//
//        setOnItemViewSelectedListener((itemViewHolder, item, rowViewHolder, row) ->
//                                              Log.i(TAG, "item selected: " +
//                                                      ((Content) item).getTitle())
//        );
//
//        setOnSearchClickedListener(view -> ContentBrowser.getInstance(
//                                           getActivity()).switchToScreen(
//                                           ContentBrowser.CONTENT_SEARCH_SCREEN)
//        );
    }

    private fun onItemClicked(item: Any) {
        if (item is VideoElement) {
            if (item.isDirectory) {
                parseAndUpdate(item)
            } else {
                val playerList = mutableListOf<VideoElement>()
                val index = videos.indexOf(item)
                playerList.addAll(videos.subList(index, videos.size).filter { !it.isDirectory })
                if (index > 0) {
                    playerList.addAll(videos.subList(0, index).filter { !it.isDirectory })
                }
                val playerUrls = playerList.map {
                    it.path
                }
                val intent = Intent(activity, VideoPlayerActivity::class.java)
                intent.putExtra(VideoPlayerActivity.EXTRA_URLS, playerUrls.toTypedArray())
                startActivity(intent)
            }
        }
    }

    internal class BrowseRegistryListener(private var fragment: GridBrowseFragment) : DefaultRegistryListener() {
        override fun remoteDeviceDiscoveryStarted(registry: Registry, device: RemoteDevice) {
            // Nothing to do
        }

        override fun remoteDeviceDiscoveryFailed(registry: Registry, device: RemoteDevice, ex: Exception) {
            // Nothing to do
        }

        override fun remoteDeviceAdded(registry: Registry, device: RemoteDevice) {
            fragment.deviceAdded(device)
        }

        override fun remoteDeviceRemoved(registry: Registry, device: RemoteDevice) {
            // Nothing to do
        }

        override fun localDeviceAdded(registry: Registry, device: LocalDevice) {
            // Nothing to do
        }

        override fun localDeviceRemoved(registry: Registry, device: LocalDevice) {
            // Nothing to do
        }
    }

    private fun findDevice() {
        Log.i(TAG, "Trying to connect to DLNA server")

        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)

        val rootJson = prefs.getString("ROOT", null)
        val root: DlnaRoot? = rootJson?.deserialize()
        root?.let { root ->
            Log.i(TAG, "Trying to connect")
            mRoot = root.mPath
            val thread =
                RetrieveDeviceThread(mUpnpService, root.mUdn, root.mUrl, root.mMaxAge, this)
            thread.start()
        }
    }

    private fun deviceAdded(device: Device<*, *, *>) {
        if (device.type.type == "MediaServer") {
            Log.i(TAG, "Found media server")
            activity.runOnUiThread(Runnable {
                if (device.isFullyHydrated) {
                    for (service in device.services as Array<RemoteService>) {
                        if (service.serviceType.type == "ContentDirectory") {
                            Log.i(TAG, "ContentDirectory found")
                            remoteService = service
                            Log.i(TAG, "Browse root $mRoot")
                            mUpnpService?.controlPoint
                                ?.execute(object : Browse(service, mRoot, BrowseFlag.DIRECT_CHILDREN) {
                                    override fun received(
                                        arg0: ActionInvocation<*>?,
                                        didl: DIDLContent
                                    ) {
                                        if (mCurrent == null) {
                                            val current =
                                                VideoElement(true, mRoot, "Root", null, activity)
                                            current.pathFromRoot = ""
                                            parseAndUpdate(didl, current)
                                        }
                                    }

                                    override fun updateStatus(status: Status) {
                                        Log.i(TAG, "updateStatus")
                                    }
                                    override fun failure(
                                        arg0: ActionInvocation<*>?,
                                        arg1: UpnpResponse,
                                        arg2: String
                                    ) {
                                        Log.i(TAG, "failure")
                                    }
                                })
                        }
                    }
                }
            })
        }
    }

    private fun parseAndUpdate(element: VideoElement, caller: VideoElement? = null) {
        mUpnpService?.controlPoint
            ?.execute(object : Browse(remoteService, element.path, BrowseFlag.DIRECT_CHILDREN) {
                override fun received(
                    arg0: ActionInvocation<*>?,
                    didl: DIDLContent
                ) {
                    parseAndUpdate(didl, element, caller)
                }

                override fun updateStatus(status: Status) {}
                override fun failure(arg0: ActionInvocation<*>?, arg1: UpnpResponse, arg2: String) {}
            })
    }

    private fun parseAndUpdate(didl: DIDLContent, clickedElement: VideoElement, caller: VideoElement? = null) {
        mHandler.post {
            var row = 0
            var selectedRow = -1
            var selectedItem = -1
            val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
            val cardPresenter = CardPresenter()

            val adapter = ArrayObjectAdapter(CardPresenter())
//            val directoryHeader = HeaderItem(1.toLong(), MovieList.MOVIE_CATEGORY[1])
//            val directoryListRowAdapter = ArrayObjectAdapter(cardPresenter)
            Log.i(TAG, "found " + didl.containers.size + " items.")
            for (i in didl.containers.indices) {
                val element = VideoElement(
                    true,
                    didl.containers[i].id,
                    didl.containers[i].title,
                    clickedElement,
                    activity
                )

                element.pathFromRoot = clickedElement.pathFromRoot.toString() + "/" + element.name
                adapter.add(element)
                if (element == caller){
                    selectedRow = row
                    selectedItem = i
                }
            }
//            if (directoryListRowAdapter.size() > 0) {
//                rowsAdapter.add(ListRow(directoryHeader, directoryListRowAdapter))
//                row++
//            }

//            val videoHeader = HeaderItem(1.toLong(), MovieList.MOVIE_CATEGORY[2])
//            val videoListRowAdapter = ArrayObjectAdapter(cardPresenter)
            videos.clear()
            Log.i(TAG, "found " + didl.items.size + " items.")
            for (i in didl.items.indices) {
                val element = VideoElement(
                    false,
                    didl.items[i].resources[0].value,
                    didl.items[i].title,
                    clickedElement,
                    activity,
                    null
                )
                element.pathFromRoot = clickedElement.pathFromRoot.toString() + "/" + element.name
                for (resource in didl.items[i].resources) {
                    if (resource.size != null) element.size = resource.size
                }
                videos.add(element)
                adapter.add(element)
//                if (element == caller){
//                    selectedRow = row
//                    selectedItem = i
//                }
            }
//            if (videoListRowAdapter.size() > 0) {
//                rowsAdapter.add(ListRow(/*videoHeader, */videoListRowAdapter))
//            }

//            val gridHeader = HeaderItem(SuperBrowseFragment.NUM_ROWS.toLong(), "PREFERENCES")

//            val mGridPresenter = GridItemPresenter()
//            val gridRowAdapter = ArrayObjectAdapter(mGridPresenter)
//            gridRowAdapter.add(resources.getString(R.string.grid_view))
//            gridRowAdapter.add(getString(R.string.error_fragment))
//            gridRowAdapter.add(resources.getString(R.string.personal_settings))
//            rowsAdapter.add(ListRow(gridHeader, gridRowAdapter))

            this.adapter = adapter

//            if (selectedItem >= 0 && selectedRow >=0) {
//                setSelectedPosition(selectedRow, false, ListRowPresenter.SelectItemViewHolderTask(selectedItem))
//            }

            mCurrent = clickedElement
        }
    }

    companion object {
        private val TAG = GridBrowseFragment::class.java.simpleName
        private const val NUM_COLUMNS = 10
    }
}