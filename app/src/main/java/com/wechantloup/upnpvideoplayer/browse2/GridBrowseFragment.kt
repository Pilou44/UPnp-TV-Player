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
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.VerticalGridPresenter
import com.wechantloup.upnpvideoplayer.browse.RetrieveDeviceThread
import com.wechantloup.upnpvideoplayer.browse.RetrieveDeviceThreadListener
import com.wechantloup.upnpvideoplayer.data.dataholder.BrowsableVideoElement
import com.wechantloup.upnpvideoplayer.data.dataholder.DlnaRoot
import com.wechantloup.upnpvideoplayer.main.MainActivity
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

class GridBrowseFragment : VerticalGridSupportFragment(), RetrieveDeviceThreadListener {

    private val videos = ArrayList<BrowsableVideoElement>()
    private val mHandler = Handler()
    var rootPath = "root"
    var rootName = ""
    private var bound = false
    private lateinit var remoteService: RemoteService
    var mCurrent: BrowsableVideoElement? = null
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
        super.onCreate(savedInstanceState)
        setupFragment()
    }

    override fun onResume() {
        super.onResume()

        // This will start the UPnP service if it wasn't already started
        Log.i(TAG, "Start UPnP Service")
        activity?.applicationContext?.bindService(
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
            activity?.applicationContext?.unbindService(mServiceConnection)
        }
    }

    override fun onDeviceNotFound() {
        TODO("Not yet implemented")
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
        val current = mCurrent
        if (current?.parent == null) {
            activity?.finish()
        } else {
            parseAndUpdate(current.parent, current)
        }
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
                parseAndUpdate(item)
            } else {
                val index = videos.indexOf(item)
                val intent = Intent(activity, VideoPlayerActivity::class.java)
                intent.putExtra(VideoPlayerActivity.EXTRA_URLS, videos)
                intent.putExtra(VideoPlayerActivity.EXTRA_INDEX, index)
                startActivityForResult(intent, MEDIA_PLAYER)
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
            rootPath = root.mPath
            rootName = root.mName
            val thread =
                RetrieveDeviceThread(mUpnpService, root.mUdn, root.mUrl, root.mMaxAge, this)
            thread.start()
        }
    }

    private fun deviceAdded(device: Device<*, *, *>) {
        if (device.type.type == "MediaServer") {
            Log.i(TAG, "Found media server")
            activity?.runOnUiThread(Runnable {
                if (device.isFullyHydrated) {
                    for (service in device.services as Array<RemoteService>) {
                        if (service.serviceType.type == "ContentDirectory") {
                            Log.i(TAG, "ContentDirectory found")
                            remoteService = service
                            Log.i(TAG, "Browse root $rootPath")
                            mUpnpService?.controlPoint
                                ?.execute(object : Browse(service, rootPath, BrowseFlag.DIRECT_CHILDREN) {
                                    override fun received(
                                        arg0: ActionInvocation<*>?,
                                        didl: DIDLContent
                                    ) {
                                        if (mCurrent == null) {
                                            val current =
                                                BrowsableVideoElement(  true, rootPath, rootName, null)
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

    private fun parseAndUpdate(element: BrowsableVideoElement, caller: BrowsableVideoElement? = null) {
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

    private fun parseAndUpdate(didl: DIDLContent, clickedElement: BrowsableVideoElement, caller: BrowsableVideoElement? = null) {
        mHandler.post {
            title = clickedElement.name
            val adapter = ArrayObjectAdapter(CardPresenter())

            val directoryQuantity = didl.containers.size
            Log.i(TAG, "found " + directoryQuantity + " items.")
            for (i in didl.containers.indices) {
                val element = BrowsableVideoElement(
                    true,
                    didl.containers[i].id,
                    didl.containers[i].title,
                    clickedElement
                )

                adapter.add(element)
            }

            val lastLine = directoryQuantity % NUM_COLUMNS
            if (lastLine > 0) {
                val rest = NUM_COLUMNS - lastLine
                repeat(rest) { adapter.add(Any()) }
            }

            videos.clear()
            Log.i(TAG, "found " + didl.items.size + " items.")
            for (i in didl.items.indices) {
                val element = BrowsableVideoElement(
                    false,
                    didl.items[i].resources[0].value,
                    didl.items[i].title,
                    clickedElement
                )
                videos.add(element)
                adapter.add(element)
            }

            var pos = 0
            caller?.let { pos = adapter.indexOf(it) }
            setSelectedPosition(pos)
            showTitle(pos < NUM_COLUMNS)

            this.adapter = adapter

            mCurrent = clickedElement
        }
    }

    companion object {
        private val TAG = GridBrowseFragment::class.java.simpleName
        private const val NUM_COLUMNS = 5
        private const val MEDIA_PLAYER = 2911
    }
}