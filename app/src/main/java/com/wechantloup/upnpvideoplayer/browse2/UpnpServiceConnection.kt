package com.wechantloup.upnpvideoplayer.browse2

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.bugsnag.android.Bugsnag
import com.wechantloup.upnpvideoplayer.browse.RetrieveDeviceThread
import com.wechantloup.upnpvideoplayer.browse.RetrieveDeviceThreadListener
import com.wechantloup.upnpvideoplayer.data.dataholder.BrowsableElement
import com.wechantloup.upnpvideoplayer.data.dataholder.BrowsableVideoElement
import com.wechantloup.upnpvideoplayer.data.dataholder.ContainerElement
import com.wechantloup.upnpvideoplayer.data.dataholder.DlnaRoot
import com.wechantloup.upnpvideoplayer.data.dataholder.VideoElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

class UpnpServiceConnection(
    private val root: DlnaRoot?,
    private val scope: CoroutineScope,
    private val callback: Callback
): ServiceConnection, RetrieveDeviceThreadListener {

    private var lastPlayedElement: BrowsableElement? = null
    private lateinit var remoteService: RemoteService
    private var currentElement: ContainerElement? = null
    private var upnpService: AndroidUpnpService? = null
    private var bound: Boolean = false
    private val registryListener: BrowseRegistryListener =
        BrowseRegistryListener(::deviceAdded)

    override fun onServiceConnected(className: ComponentName, service: IBinder) {
        Log.i(TAG, "Service connected")
        val upnpService = (service as AndroidUpnpService).also { upnpService = it }

        // Get ready for future device advertisements
        upnpService.registry.addListener(registryListener)
        findDevice()
    }

    override fun onServiceDisconnected(className: ComponentName) {
        Log.w(TAG, "Service disconnected")
        upnpService = null
    }

    override fun onDeviceNotFound() {
        Bugsnag.notify(Exception("Device not found"))
    }

    fun bind(context: Context) {
        // This will start the UPnP service if it wasn't already started
        Log.i(TAG, "Start UPnP Service")
        context.applicationContext?.bindService(
            Intent(context, AndroidUpnpServiceImpl::class.java),
            this,
            Context.BIND_AUTO_CREATE
        )
        bound = true
    }

    fun unbind(context: Context) {
        upnpService?.registry?.removeListener(registryListener)
        // This will stop the UPnP service if nobody else is bound to it
        if (bound) {
            context.applicationContext?.unbindService(this)
        }
    }

    private fun findDevice() {
        Log.i(TAG, "Trying to connect to DLNA server")
        root?.let {
            Log.i(TAG, "Trying to connect")
            if (currentElement == null) {
                currentElement = ContainerElement(root.mPath, root.mName, null)
            }

            val thread =
                RetrieveDeviceThread(upnpService, root.mUdn, root.mUrl, root.mMaxAge, this)
            // ToDo to improve
//            thread.start()
            scope.launch {
                withContext(Dispatchers.IO) { thread.run() }
            }
        }
    }

    private fun deviceAdded(device: Device<*, *, *>) {
        if (device.type.type == "MediaServer") {
            Log.i(TAG, "Found media server")
            if (device.isFullyHydrated) {
                @Suppress("UNCHECKED_CAST")
                for (service in device.services as Array<RemoteService>) {
                    if (service.serviceType.type == "ContentDirectory") {
                        Log.i(TAG, "ContentDirectory found")
                        remoteService = service
                        val element = currentElement ?: return
                        Log.i(TAG, "Browse current element ${element.name}")
                        upnpService?.controlPoint
                            ?.execute(object : Browse(service, element.path, BrowseFlag.DIRECT_CHILDREN) {
                                override fun received(
                                    arg0: ActionInvocation<*>?,
                                    didl: DIDLContent
                                ) {
                                    parseAndUpdate(didl, element, lastPlayedElement)
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
        }
    }

    fun parseAndUpdate(element: ContainerElement, selectedElement: BrowsableElement?) {
        upnpService?.controlPoint
            ?.execute(object : Browse(remoteService, element.path, BrowseFlag.DIRECT_CHILDREN) {
                override fun received(
                    arg0: ActionInvocation<*>?,
                    didl: DIDLContent
                ) {
                    parseAndUpdate(didl, element, selectedElement)
                }

                override fun updateStatus(status: Status) {}
                override fun failure(arg0: ActionInvocation<*>?, arg1: UpnpResponse, arg2: String) {}
            })
    }

    private fun parseAndUpdate(didl: DIDLContent, openedElement: ContainerElement, selectedElement: BrowsableElement?) {
        val title = openedElement.name

//        val startedMovies = videoRepository.getAllVideo()
//        Log.i("TAG", "${startedMovies.size} started elements found")

        Log.i(TAG, "found " + didl.containers.size + " items.")
        val directories = didl.containers.map {
            ContainerElement(
                it.id,
                it.title,
                openedElement
            )
        }

        Log.i(TAG, "found " + didl.items.size + " items.")
        val movies = didl.items.map {
            BrowsableVideoElement(
                it.resources[0].value,
                it.title,
                openedElement
            )
        }

        currentElement = openedElement

        callback.setNewContent(title, directories, movies, selectedElement)
    }

    fun browseParent(): Boolean {
        currentElement?.let {
            if (it.parent == null) return false
            parseAndUpdate(it.parent, it)
            return true
        }
        return false
    }

    fun launch(element: VideoElement, position: Long) {
        scope.launch {
            upnpService?.controlPoint
                ?.execute(object : Browse(remoteService, element.parentPath, BrowseFlag.DIRECT_CHILDREN) {
                    override fun received(
                        arg0: ActionInvocation<*>?,
                        didl: DIDLContent
                    ) {
                        Log.i(TAG, "found " + didl.items.size + " items.")
                        val movies = ArrayList<VideoElement.ParcelableElement>()
                        didl.items.forEach {
                            VideoElement.ParcelableElement(
                                it.resources[0].value,
                                element.parentPath,
                                it.title
                            ).also { element ->
                                movies.add(element)
                            }
                        }
                        val index = movies.indexOfFirst { it.path == element.path }

                        if (index < 0) return

                        callback.launch(movies, index, position)
                    }

                    override fun updateStatus(status: Status) {}
                    override fun failure(arg0: ActionInvocation<*>?, arg1: UpnpResponse, arg2: String) {}
                })
        }
    }

    fun setLastPlayedElement(lastPlayedElement: BrowsableElement) {
        this.lastPlayedElement = lastPlayedElement
    }

    interface Callback {
        fun setNewContent(
            title: String,
            directories: List<ContainerElement>,
            movies: List<BrowsableVideoElement>,
            selectedElement: BrowsableElement?
        )

        fun launch(movies: ArrayList<VideoElement.ParcelableElement>, index: Int, position: Long)
    }

    internal class BrowseRegistryListener(private val onDeviceAdded: (Device<*, *, *>) -> Unit) : DefaultRegistryListener() {
        override fun remoteDeviceDiscoveryStarted(registry: Registry, device: RemoteDevice) {
            // Nothing to do
        }

        override fun remoteDeviceDiscoveryFailed(registry: Registry, device: RemoteDevice, ex: Exception) {
            // Nothing to do
        }

        override fun remoteDeviceAdded(registry: Registry, device: RemoteDevice) {
            onDeviceAdded(device)
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

    companion object {
        private val TAG = UpnpServiceConnection::class.java.simpleName
    }
}