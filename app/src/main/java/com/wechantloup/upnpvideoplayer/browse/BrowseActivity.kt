package com.wechantloup.upnpvideoplayer.browse

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import android.view.KeyEvent
import android.widget.Button
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.wechantloup.upnpvideoplayer.R
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

class BrowseActivity : Activity(), RetrieveDeviceThreadListener {

//    private lateinit var layoutManager: FlexboxLayoutManager
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var adapter: BrowseAdapter
    private lateinit var remoteService: RemoteService
    private lateinit var list: RecyclerView
    private val elements = mutableListOf<Any>()
    var mCurrent: VideoElement? = null
    private val mHandler = Handler()
    var mRoot = "root"
    private var bound = false
    private var mUpnpService: AndroidUpnpService? = null
    private val mRegistryListener: BrowseRegistryListener = BrowseRegistryListener(this)
    private var previouslySelected: Int? = null

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

    private fun findDevice() {
        Log.i(TAG, "Trying to connect to DLNA server")

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)

        list = findViewById(R.id.list)
        val directoriesButton: Button = findViewById(R.id.directories_button)
        val videosButton: Button = findViewById(R.id.movies_button)
        list.adapter = BrowseAdapter(elements, ::onItemClicked, ::onItemSelected, directoriesButton.id, videosButton.id).also {
            adapter = it
        }
        layoutManager = BrowserLayoutManager(this, NUMBER_OF_COLUMNS, RecyclerView.VERTICAL, false)
//        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
//            override fun getSpanSize(position: Int): Int {
//                if (adapter.getItemViewType(position) == BrowseAdapter.TYPE_TITLE)
//                    return layoutManager.spanCount
//                return 1
//            }
//        }
//        layoutManager = FlexboxLayoutManager(this)
//        layoutManager.flexWrap = FlexWrap.WRAP
//        layoutManager.flexDirection = FlexDirection.ROW
//        layoutManager.justifyContent = JustifyContent.FLEX_START
//        layoutManager.alignItems = AlignItems.FLEX_START
        list.layoutManager = layoutManager

        list.setOnFocusChangeListener { v, hasFocus -> Log.i(TAG, "Focus changed for ${v.id}: $hasFocus") }
        directoriesButton.setOnFocusChangeListener { v, hasFocus -> Log.i(TAG, "Focus changed for ${v.id}: $hasFocus") }
        videosButton.setOnFocusChangeListener { v, hasFocus -> Log.i(TAG, "Focus changed for ${v.id}: $hasFocus") }

        videosButton.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                val firstVideo: VideoElement? = elements.filterIsInstance<VideoElement>().firstOrNull { !it.isDirectory }
                Log.i(TAG, "Scroll to video ${firstVideo?.name}")
                firstVideo?.let {
                    val pos = elements.indexOf(it) - 1
                    layoutManager.scrollToPositionWithOffset(pos, 0)
//                    list.smoothScrollToPosition(pos)
                }
            }
        }
        videosButton.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT/* && event.action == KeyEvent.ACTION_DOWN*/) {
                val firstVideo: VideoElement? = elements.filterIsInstance<VideoElement>().firstOrNull { !it.isDirectory }
                Log.i(TAG, "Go to video ${firstVideo?.name}")
                firstVideo?.let {
                    val pos = elements.indexOf(it)
                    adapter.requestFocusFor(pos)
                }
                true
            } else {
                false
            }
        }
        directoriesButton.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                val firstDir: VideoElement? = elements.filterIsInstance<VideoElement>().firstOrNull { it.isDirectory }
                Log.i(TAG, "Scroll to dir ${firstDir?.name}")
                firstDir?.let {
                    val pos = elements.indexOf(it) - 1
                    layoutManager.scrollToPositionWithOffset(pos, 0)
//                    list.smoothScrollToPosition(pos)
                }
            }
        }
        directoriesButton.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT/* && event.action == KeyEvent.ACTION_DOWN*/) {
                val firstDir: VideoElement? = elements.filterIsInstance<VideoElement>().firstOrNull { it.isDirectory }
                Log.i(TAG, "Go to dir ${firstDir?.name}")
                firstDir?.let {
                    val pos = elements.indexOf(it)
                    adapter.requestFocusFor(pos)
                }
                true
            } else {
                false
            }
        }
    }

    private fun onItemSelected(selected: Int) {
//        if (selected <= NUMBER_OF_COLUMNS) {
//            layoutManager.scrollToPositionWithOffset(0, 0)
//        }
//        Log.i(TAG, "onItemSelected $selected")
//        val previous = previouslySelected
//        if (previous == null) {
//            scrollToNextLine(selected)
//            scrollToPreviousLine(selected)
//            previouslySelected = selected
//            return
//        }
//        val diff = selected - previous
//        if (diff > 0) {
//            scrollToNextLine(selected)
//        } else if (diff < 0) {
//            scrollToPreviousLine(selected)
//        }
//        previouslySelected = selected
    }

    private fun scrollToPreviousLine(selected: Int) {
        var position = selected - NUMBER_OF_COLUMNS
        if (position >= 0 && elements[position] !is VideoElement) {
            position--
        }
        if (position < 0) {
            position = 0
        }
        Log.i(TAG, "smoothScrollToPosition $position")
        list.smoothScrollToPosition(position)
    }

    private fun scrollToNextLine(selected: Int) {
        var position = selected + NUMBER_OF_COLUMNS
        if (position < elements.size && elements[position] !is VideoElement) {
            position++
        }
        if (position >= elements.size) {
            position = elements.size - 1
        }
        Log.i(TAG, "smoothScrollToPosition $position")
        list.smoothScrollToPosition(position)
    }

    override fun onResume() {
        super.onResume()

        // This will start the UPnP service if it wasn't already started
        Log.i(TAG, "Start UPnP Service")
        applicationContext.bindService(
            Intent(this, AndroidUpnpServiceImpl::class.java),
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
            applicationContext.unbindService(mServiceConnection)
        }
    }

    override fun onBackPressed() {
        if (mCurrent == null || mCurrent!!.path == mRoot) {
            super.onBackPressed()
        } else {
            Log.i(TAG, "Back to ${mCurrent?.name}")
            parseAndUpdate(mCurrent!!.parent, mCurrent)
        }
    }

    companion object {
        private val TAG = BrowseActivity::class.java.simpleName
        internal const val NUMBER_OF_COLUMNS = 6
    }

    internal class BrowseRegistryListener(private var activity: BrowseActivity) : DefaultRegistryListener() {
        override fun remoteDeviceDiscoveryStarted(registry: Registry, device: RemoteDevice) {
            // Nothing to do
        }

        override fun remoteDeviceDiscoveryFailed(registry: Registry, device: RemoteDevice, ex: Exception) {
            // Nothing to do
        }

        override fun remoteDeviceAdded(registry: Registry, device: RemoteDevice) {
            activity.deviceAdded(device)
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

    private fun onItemClicked(element: VideoElement) {
        if (element.isDirectory) {
            parseAndUpdate(element)
        } else {
            val videoList = elements.filterIsInstance<VideoElement>().filter { !it.isDirectory }
            val playerList = mutableListOf<VideoElement>()
            val index = videoList.indexOf(element)
            playerList.addAll(videoList.subList(index, elements.size))
            if (index > 0) {
                playerList.addAll(videoList.subList(0, index))
            }
            val playerUrls = playerList.map {
                it.path
            }
            val intent = Intent(this, VideoPlayerActivity::class.java)
            intent.putExtra(VideoPlayerActivity.EXTRA_URLS, playerUrls.toTypedArray())
            startActivity(intent)
        }
    }

    private fun deviceAdded(device: Device<*, *, *>) {
        if (device.type.type == "MediaServer") {
            Log.i(TAG, "Found media server")
            runOnUiThread(Runnable {
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
                                                VideoElement(true, mRoot, "Root", null)
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
                    Log.i(TAG, "Parse ${caller?.name}")
                    parseAndUpdate(didl, element, caller)
                }

                override fun updateStatus(status: Status) {}
                override fun failure(arg0: ActionInvocation<*>?, arg1: UpnpResponse, arg2: String) {}
            })
    }

    private fun parseAndUpdate(didl: DIDLContent, clickedElement: VideoElement, caller: VideoElement? = null) {
        mHandler.post {
            elements.clear()
            Log.i(TAG, "found " + didl.containers.size + " directories.")
//            if (didl.containers.isNotEmpty()) {
//                elements.add("Dossiers")
//            }
            for (i in didl.containers.indices) {
                val element = VideoElement(
                    true,
                    didl.containers[i].id,
                    didl.containers[i].title,
                    clickedElement
                )
                elements.add(element)
            }
            val numberOfDirectoriesOnLastLine = didl.containers.size % NUMBER_OF_COLUMNS
            val numberOfEmptyElements = NUMBER_OF_COLUMNS - numberOfDirectoriesOnLastLine
            repeat (numberOfEmptyElements) {
                elements.add("toto")
            }
            Log.i(TAG, "found " + didl.items.size + " videos.")
//            if (didl.items.isNotEmpty()) {
//                elements.add("Videos")
//            }
            for (i in didl.items.indices) {
                val element = VideoElement(
                    false,
                    didl.items[i].resources[0].value,
                    didl.items[i].title,
                    clickedElement
                )
                elements.add(element)
            }

            Log.i(TAG, "parseAndUpdate caller = ${caller?.name}")
            adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    previouslySelected = null
                    val pos = caller?.let { elements.indexOf(caller) } ?: -1
                    if (pos >= 0) {
                        list.scrollToPosition(pos)
                        adapter.requestFocusFor(pos)
                    }
                    adapter.unregisterAdapterDataObserver(this)
                }
            })
            adapter.notifyDataSetChanged()

            mCurrent = clickedElement
        }
    }

    override fun onDeviceNotFound() {
        TODO("Not yet implemented")
    }
}