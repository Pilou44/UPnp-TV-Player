package com.wechantloup.upnpvideoplayer.browse2

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wechantloup.upnpvideoplayer.browse.RetrieveDeviceThread
import com.wechantloup.upnpvideoplayer.browse.RetrieveDeviceThreadListener
import com.wechantloup.upnpvideoplayer.data.dataholder.BrowsableVideoElement
import com.wechantloup.upnpvideoplayer.data.dataholder.DlnaRoot
import com.wechantloup.upnpvideoplayer.data.dataholder.VideoElement
import com.wechantloup.upnpvideoplayer.data.repository.VideoRepository
import com.wechantloup.upnpvideoplayer.utils.Serializer.deserialize
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

internal class BrowseViewModel(
    private val videoRepository: VideoRepository
) : ViewModel(), BrowseContract.ViewModel, RetrieveDeviceThreadListener {

    private lateinit var view: BrowseContract.View
    private lateinit var context: Context // ToDo Must be removed once repositories have been created

    private var rootPath = "root"
    private var rootName = ""
    private var bound = false
    private lateinit var remoteService: RemoteService
    private var currentElement: BrowsableVideoElement? = null
    private var upnpService: AndroidUpnpService? = null
    private val registryListener: BrowseRegistryListener =
        BrowseRegistryListener(::deviceAdded)

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.i(TAG, "Service connected")
            val upnpService = (service as AndroidUpnpService).also { upnpService = it }

            // Get ready for future device advertisements
            upnpService.registry.addListener(registryListener)
            findDevice()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Log.i(TAG, "Service disconnected")
            upnpService = null
        }
    }

    override fun setView(view: BrowseContract.View) {
        this.view = view
    }

    override fun onViewResumed(context: Context) {
        this.context = context // ToDo Must be removed once repositories have been created

        // This will start the UPnP service if it wasn't already started
        Log.i(TAG, "Start UPnP Service")
        context.applicationContext?.bindService(
            Intent(context, AndroidUpnpServiceImpl::class.java),
            mServiceConnection,
            Context.BIND_AUTO_CREATE
        )
        bound = true
    }

    override fun onViewPaused(context: Context) {
        upnpService?.registry?.removeListener(registryListener)
        // This will stop the UPnP service if nobody else is bound to it
        if (bound) {
            context.applicationContext?.unbindService(mServiceConnection)
        }
    }

    override fun parse(item: BrowsableVideoElement) {
        viewModelScope.launch {
            parseAndUpdate(item)
        }
    }

    override fun goBack(): Boolean {
        val currentNow = currentElement ?: return false
        if (currentNow.parent == null) return false
        parseAndUpdate(currentNow.parent, currentNow)
        return true
    }

    override fun convertToBrowsableVideoElement(item: VideoElement) {
        viewModelScope.launch {
            videoRepository.removeVideo(item)
            upnpService?.controlPoint
                ?.execute(object : Browse(remoteService, item.containerId, BrowseFlag.DIRECT_CHILDREN) {
                    override fun received(
                        arg0: ActionInvocation<*>?,
                        didl: DIDLContent
                    ) {
                        Log.i(TAG, "found " + didl.items.size + " items.")
                        val parent = BrowsableVideoElement(
                            true,
                            item.containerId,
                            "parent",
                            null
                        )
                        val movies = ArrayList<BrowsableVideoElement>()
                        didl.items.forEach {
                            BrowsableVideoElement(
                                false,
                                it.resources[0].value,
                                it.title,
                                parent
                            ).also { element ->
                                movies.add(element)
                            }
                        }
                        val index = movies.indexOfFirst { it.path == item.path }

                        if (index < 0) return

                        view.launch(movies, index, item.position)
                    }

                    override fun updateStatus(status: Status) {}
                    override fun failure(arg0: ActionInvocation<*>?, arg1: UpnpResponse, arg2: String) {}
                })
        }
    }

    private fun deviceAdded(device: Device<*, *, *>) {
        if (device.type.type == "MediaServer") {
            Log.i(TAG, "Found media server")
            if (device.isFullyHydrated) {
                for (service in device.services as Array<RemoteService>) {
                    if (service.serviceType.type == "ContentDirectory") {
                        Log.i(TAG, "ContentDirectory found")
                        remoteService = service
                        Log.i(TAG, "Browse root $rootPath")
                        upnpService?.controlPoint
                            ?.execute(object : Browse(service, rootPath, BrowseFlag.DIRECT_CHILDREN) {
                                override fun received(
                                    arg0: ActionInvocation<*>?,
                                    didl: DIDLContent
                                ) {
                                    viewModelScope.launch {
                                        if (currentElement == null) {
                                            val current =
                                                BrowsableVideoElement(  true, rootPath, rootName, null)
                                            parseAndUpdate(didl, current)
                                        } else {
                                            refreshStarted()
                                        }
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
        }
    }

    private fun parseAndUpdate(element: BrowsableVideoElement, caller: BrowsableVideoElement? = null) {
        upnpService?.controlPoint
            ?.execute(object : Browse(remoteService, element.path, BrowseFlag.DIRECT_CHILDREN) {
                override fun received(
                    arg0: ActionInvocation<*>?,
                    didl: DIDLContent
                ) {
                    viewModelScope.launch {
                        parseAndUpdate(didl, element, caller)
                    }
                }

                override fun updateStatus(status: Status) {}
                override fun failure(arg0: ActionInvocation<*>?, arg1: UpnpResponse, arg2: String) {}
            })
    }

    private suspend fun refreshStarted() {
        val startedMovies = videoRepository.getAllVideo()
        view.updateStarted(startedMovies)
    }

    private suspend fun parseAndUpdate(didl: DIDLContent, clickedElement: BrowsableVideoElement, caller: BrowsableVideoElement? = null) {
        val title = clickedElement.name

        val startedMovies = videoRepository.getAllVideo()
        Log.i("TAG", "${startedMovies.size} started elements found")

        Log.i(TAG, "found " + didl.containers.size + " items.")
        val directories = didl.containers.map {
            BrowsableVideoElement(
                true,
                it.id,
                it.title,
                clickedElement
            )
        }

        Log.i(TAG, "found " + didl.items.size + " items.")
        val movies = didl.items.map {
            BrowsableVideoElement(
                false,
                it.resources[0].value,
                it.title,
                clickedElement
            )
        }

        currentElement = clickedElement

        view.displayContent(title, startedMovies, directories, movies, caller)
    }

    private fun findDevice() {
        Log.i(TAG, "Trying to connect to DLNA server")

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val rootJson = prefs.getString("ROOT", null)
        val root: DlnaRoot? = rootJson?.deserialize()
        root?.let {
            Log.i(TAG, "Trying to connect")
            rootPath = root.mPath
            rootName = root.mName
            val thread =
                RetrieveDeviceThread(upnpService, root.mUdn, root.mUrl, root.mMaxAge, this)
            // ToDo to improve
//            thread.start()
            viewModelScope.launch {
                withContext(Dispatchers.IO) { thread.run() }
            }
        }
    }

    override fun onDeviceNotFound() {
        TODO("Not yet implemented")
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
        private val TAG = BrowseViewModel::class.java.simpleName
    }
}