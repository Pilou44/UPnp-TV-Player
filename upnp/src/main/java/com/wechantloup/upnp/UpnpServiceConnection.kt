package com.wechantloup.upnp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.bugsnag.android.Bugsnag
import com.wechantloup.upnp.dataholder.DlnaRoot
import com.wechantloup.upnp.dataholder.PlayableItem
import com.wechantloup.upnp.dataholder.UpnpContainerData
import com.wechantloup.upnp.dataholder.UpnpElement
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
import java.io.FileNotFoundException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class UpnpServiceConnection(
    private var root: DlnaRoot?,
    private val scope: CoroutineScope,
    private val callback: Callback
) : ServiceConnection, RetrieveDeviceThreadListener {

//    private var lastPlayedElement: VideoElement? = null
    private lateinit var remoteService: RemoteService
    private var currentElement: UpnpElement? = null
    private var upnpService: AndroidUpnpService? = null
    private var bound: Boolean = false
    private val registryListener: BrowseRegistryListener =
        BrowseRegistryListener(::deviceAdded)

    override fun onServiceConnected(className: ComponentName, service: IBinder) {
        Log.i(TAG, "Service connected")
        val upnpService = (service as AndroidUpnpService).also { upnpService = it }

        // Get ready for future device advertisements
        upnpService.registry.addListener(registryListener)
        scope.launch {
            findDevice()
        }
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
        Log.i(TAG, "bind")
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

    private suspend fun findDevice() {
        Log.i(TAG, "findDevice")
        root?.let {
            Log.i(TAG, "Trying to connect root ${it.mName}")
            if (currentElement == null) {
                currentElement = UpnpElement(UpnpElement.Type.CONTAINER, it.mPath, it.mName, null)
            }

            val thread =
                RetrieveDeviceThread(upnpService, it.mUdn, it.mUrl, it.mMaxAge, this)
            // ToDo to improve
            withContext(Dispatchers.IO) { thread.run() }
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
                                    callback.onReady()
//                                    parseAndUpdate(didl, element, lastPlayedElement)
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
                                    callback.onErrorConnectingServer()
                                }
                            })
                    }
                }
            }
        }
    }

    suspend fun parseAndUpdate(element: UpnpElement): UpnpContainerData =
        suspendCoroutine { continuation ->
            upnpService?.controlPoint
                ?.execute(object : Browse(remoteService, element.path, BrowseFlag.DIRECT_CHILDREN) {
                    override fun received(
                        arg0: ActionInvocation<*>?,
                        didl: DIDLContent
                    ) {
                        continuation.resume(parseAndUpdate(didl, element))
                    }

                    override fun updateStatus(status: Status) {}
                    override fun failure(arg0: ActionInvocation<*>?, arg1: UpnpResponse, arg2: String) {
                        continuation.resumeWithException(UpnpException(arg1))
                    }
                })
        }

//    fun parseAndUpdate(element: ContainerElement, selectedElement: BrowsableElement?) {
//        upnpService?.controlPoint
//            ?.execute(object : Browse(remoteService, element.path, BrowseFlag.DIRECT_CHILDREN) {
//                override fun received(
//                    arg0: ActionInvocation<*>?,
//                    didl: DIDLContent
//                ) {
//                    parseAndUpdate(didl, element, selectedElement)
//                }
//
//                override fun updateStatus(status: Status) {}
//                override fun failure(arg0: ActionInvocation<*>?, arg1: UpnpResponse, arg2: String) {}
//            })
//    }

    private fun parseAndUpdate(
        didl: DIDLContent,
        openedElement: UpnpElement
    ): UpnpContainerData {
        Log.i(TAG, "found " + didl.containers.size + " items.")
        val directories = didl.containers.map {
            UpnpElement(
                UpnpElement.Type.CONTAINER,
                it.id,
                it.title,
                openedElement
            )
        }

        Log.i(TAG, "found " + didl.items.size + " items.")
        val movies = didl.items.map {
            UpnpElement(
                UpnpElement.Type.FILE,
                it.resources[0].value,
                it.title,
                openedElement
            )
        }

        currentElement = openedElement

        return UpnpContainerData(openedElement, directories, movies)
    }

    suspend fun launch(element: UpnpElement): PlayableItem =
        suspendCoroutine { continuation ->
            val parent = requireNotNull(element.parent)
            if (element.type == UpnpElement.Type.CONTAINER) continuation.resumeWithException(IllegalStateException())

            upnpService?.controlPoint
                ?.execute(object : Browse(remoteService, parent.path, BrowseFlag.DIRECT_CHILDREN) {
                    override fun received(
                        arg0: ActionInvocation<*>?,
                        didl: DIDLContent
                    ) {
                        Log.i(TAG, "found " + didl.items.size + " items.")
                        val movies = mutableListOf<UpnpElement>()
                        didl.items.forEach {
                            UpnpElement(
                                UpnpElement.Type.FILE,
                                it.resources[0].value,
                                it.title,
                                parent
                            ).also { element ->
                                movies.add(element)
                            }
                        }
                        val index = movies.indexOfFirst { it.path == element.path }

                        if (index < 0) continuation.resumeWithException(FileNotFoundException())

                        val item = PlayableItem(movies, index)
                        continuation.resume(item)
                    }

                    override fun updateStatus(status: Status) {}
                    override fun failure(arg0: ActionInvocation<*>?, arg1: UpnpResponse, arg2: String) {
                        continuation.resumeWithException(UpnpException(arg1))
                    }
                })
        }

//    fun setLastPlayedElement(lastPlayedElement: VideoElement) {
//        this.lastPlayedElement = lastPlayedElement
//    }

    fun resetRoot(newRoot: DlnaRoot?) {
        Log.i(TAG, "resetRoot to ${newRoot?.mName}")
        currentElement = null
        root = newRoot
    }

    interface Callback {
        fun onReady()
        fun onErrorConnectingServer()
    }

    internal class BrowseRegistryListener(private val onDeviceAdded: (Device<*, *, *>) -> Unit) :
        DefaultRegistryListener() {

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

class UpnpException(upnpResponse: UpnpResponse) : Throwable(upnpResponse.statusMessage)
