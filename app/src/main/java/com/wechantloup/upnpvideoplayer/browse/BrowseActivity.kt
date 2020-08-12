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

    private lateinit var adapter: BrowseAdapter
    private lateinit var remoteService: RemoteService
    private lateinit var list: RecyclerView
    private val elements = mutableListOf<VideoElement>()
    var mCurrent: VideoElement? = null
    private val mHandler = Handler()
    var mRoot = "root"
    private var bound = false
    private var mUpnpService: AndroidUpnpService? = null
    private val mRegistryListener: BrowseRegistryListener = BrowseRegistryListener(this)

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
        list.adapter = BrowseAdapter(elements, ::onItemClicked).also {
            adapter = it
        }
        val layoutManager = FlexboxLayoutManager(this)
        layoutManager.flexWrap = FlexWrap.WRAP
        layoutManager.flexDirection = FlexDirection.ROW
        layoutManager.justifyContent = JustifyContent.SPACE_AROUND
        layoutManager.alignItems = AlignItems.FLEX_START
        list.layoutManager = layoutManager
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
            parseAndUpdate(mCurrent!!.parent, mCurrent)
        }
    }

    companion object {
        private val TAG = BrowseActivity::class.java.simpleName
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
            val playerList = mutableListOf<VideoElement>()
            val index = elements.indexOf(element)
            playerList.addAll(elements.subList(index, elements.size).filter { !it.isDirectory })
            if (index > 0) {
                playerList.addAll(elements.subList(0, index).filter { !it.isDirectory })
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
                                                VideoElement(true, mRoot, "Root", null, this@BrowseActivity)
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
            elements.clear()
            Log.i(TAG, "found " + didl.containers.size + " items.")
            for (i in didl.containers.indices) {
                val element = VideoElement(
                    true,
                    didl.containers[i].id,
                    didl.containers[i].title,
                    clickedElement,
                    this
                )
                element.pathFromRoot = clickedElement.pathFromRoot.toString() + "/" + element.name
                elements.add(element)
            }
            Log.i(TAG, "found " + didl.items.size + " items.")
            for (i in didl.items.indices) {
                val element = VideoElement(
                    false,
                    didl.items[i].resources[0].value,
                    didl.items[i].title,
                    clickedElement,
                    this,
                    null
//                    mListView
                )
                element.pathFromRoot = clickedElement.pathFromRoot.toString() + "/" + element.name
                for (resource in didl.items[i].resources) {
                    if (resource.size != null) element.size = resource.size
                }
                elements.add(element)
            }
            adapter.notifyDataSetChanged()

            var pos = caller?.let { elements.indexOf(caller) } ?: -1
            Log.i(TAG, "Scroll to pos $pos")
            if (pos == -1) pos = 0
            list.scrollToPosition(pos)
            adapter.requestFocusFor(pos)

            mCurrent = clickedElement
        }
    }

    override fun onDeviceNotFound() {
        TODO("Not yet implemented")
    }
}