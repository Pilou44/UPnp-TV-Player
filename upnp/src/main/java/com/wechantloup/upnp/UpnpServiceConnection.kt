package com.wechantloup.upnp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.bugsnag.android.Bugsnag
import com.wechantloup.upnp.dataholder.DlnaRoot
import com.wechantloup.upnp.dataholder.DlnaServer
import com.wechantloup.upnp.dataholder.UpnpElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fourthline.cling.android.AndroidUpnpService
import org.fourthline.cling.android.AndroidUpnpServiceImpl
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.LocalDevice
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.meta.RemoteDeviceIdentity
import org.fourthline.cling.model.meta.RemoteService
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import org.fourthline.cling.support.contentdirectory.callback.Browse
import org.fourthline.cling.support.model.BrowseFlag
import org.fourthline.cling.support.model.DIDLContent
import java.util.Timer
import java.util.concurrent.TimeoutException
import kotlin.concurrent.schedule
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class UpnpServiceConnection(private val callback: Callback) : ServiceConnection, RetrieveDeviceThreadListener {

    private var usedListener: DefaultRegistryListener? = null

    private var upnpService: AndroidUpnpService? = null
    private var bound: Boolean = false

    override fun onServiceConnected(className: ComponentName, service: IBinder) {
        Log.i(TAG, "Service connected")
        upnpService = (service as AndroidUpnpService)

        callback.onServiceConnected()
    }

    override fun onServiceDisconnected(className: ComponentName) {
        Log.w(TAG, "Service disconnected")
        upnpService = null
    }

    override fun onDeviceNotFound() {
        Bugsnag.notify(Exception("Device not found"))
    }

    suspend fun getRootContainer(root: DlnaRoot): UpnpElement {
        val remoteService = withContext(Dispatchers.IO) {
            retrieveService(root)
        }
        val server = DlnaServer(root, remoteService)
        return UpnpElement(
            UpnpElement.Type.CONTAINER,
            root.mPath,
            root.mName,
            null,
            server
        )
    }

    private suspend fun retrieveService(root: DlnaRoot): RemoteService =
        suspendCoroutine { continuation ->
            val timeOutTimer = Timer("TimeOut", false).schedule(30000) {
                continuation.resumeWithException(TimeoutException())
            }
            val service = requireNotNull(upnpService)
            // Get ready for future device advertisements
            val listener = BrowseRegistryListener { listener: BrowseRegistryListener, device: Device<*, *, *> ->
                if (device.isFullyHydrated) {
                    @Suppress("UNCHECKED_CAST")
                    for (remoteService in device.services as Array<RemoteService>) {
                        if (remoteService.serviceType.type == "ContentDirectory") {
                            timeOutTimer.cancel()
                            service.registry.removeListener(listener)
                            continuation.resume(remoteService)
                        }
                    }
                }

            }
            service.registry.addListener(listener)
            val thread =
            RetrieveDeviceThread(upnpService, root.mUdn, root.mUrl, root.mMaxAge, this)
            thread.run()
        }

    fun findDevices(scope: CoroutineScope): Channel<UpnpElement> {
        val service = requireNotNull(upnpService)
        val channel = Channel<UpnpElement>()
        usedListener = RootRegistryListener(channel, scope, ::addDevice)
        service.registry.addListener(usedListener)// Now add all devices to the list we already know about
        for (device in service.registry.devices) {
            addDevice(channel, scope, device)
        }

        // Search asynchronously for all devices, they will respond soon
        service.controlPoint.search()
        return channel
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
//        upnpService?.registry?.removeListener(registryListener)
        usedListener?.let {
            upnpService?.registry?.removeListener(it)
        }
        // This will stop the UPnP service if nobody else is bound to it
        if (bound) {
            context.applicationContext?.unbindService(this)
        }
    }

    private fun addDevice(channel: Channel<UpnpElement>, scope: CoroutineScope, device: Device<*, *, *>) {
        if (device.isFullyHydrated) {
            for (service in device.services) {
                if (service.serviceType.type == "ContentDirectory") {
                    val root = DlnaRoot(
                        device.displayString,
                        device.identity.udn.identifierString,
                        (device.identity as RemoteDeviceIdentity).descriptorURL.toString(),
                        "0",
                        device.identity.maxAgeSeconds
                    )
                    val server = DlnaServer(
                        root,
                        service as RemoteService
                    )
                    val rootContainer = UpnpElement(
                        UpnpElement.Type.CONTAINER,
                        "0",
                        device.displayString,
                        null,
                        server
                    )
                    scope.launch {
                        channel.send(rootContainer)
                    }
                }
            }
        }
    }

    suspend fun parseAndUpdate(element: UpnpElement): List<UpnpElement> {
        val checkedElement = checkService(element)
        return suspendCoroutine { continuation ->
            upnpService?.controlPoint
                ?.execute(object : Browse(checkedElement.server.service, checkedElement.path, BrowseFlag.DIRECT_CHILDREN) {
                    override fun received(
                        arg0: ActionInvocation<*>?,
                        didl: DIDLContent
                    ) {
                        continuation.resume(parseAndUpdate(didl, checkedElement))
                    }

                    override fun updateStatus(status: Status) {}
                    override fun failure(arg0: ActionInvocation<*>?, arg1: UpnpResponse, arg2: String) {
                        continuation.resumeWithException(UpnpException(arg1))
                    }
                })
        }
    }

    private suspend fun checkService(element: UpnpElement): UpnpElement {
        if (element.server.service != null) return element

        val remoteService = retrieveService(element.server.info)
        val server = DlnaServer(element.server.info, remoteService)
        return element.copy(server = server)
    }

    private fun parseAndUpdate(
        didl: DIDLContent,
        openedElement: UpnpElement
    ): List<UpnpElement> {
        Log.i(TAG, "found " + didl.containers.size + " items.")
        val directories = didl.containers.map {
            UpnpElement(
                UpnpElement.Type.CONTAINER,
                it.id,
                it.title,
                openedElement,
                openedElement.server
            )
        }

        Log.i(TAG, "found " + didl.items.size + " items.")
        val movies = didl.items.map {
            UpnpElement(
                UpnpElement.Type.FILE,
                it.resources[0].value,
                it.title,
                openedElement,
                openedElement.server
            )
        }

        return directories + movies
    }

    interface Callback {
        fun onServiceConnected()
    }

    private class BrowseRegistryListener(
        private val onDeviceAdded: (BrowseRegistryListener, Device<*, *, *>) -> Unit
    ) : DefaultRegistryListener() {

        override fun remoteDeviceDiscoveryStarted(registry: Registry, device: RemoteDevice) {
            // Nothing to do
        }

        override fun remoteDeviceDiscoveryFailed(registry: Registry, device: RemoteDevice, ex: Exception) {
            // Nothing to do
        }

        override fun remoteDeviceAdded(registry: Registry, device: RemoteDevice) {
            onDeviceAdded(this, device)
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

    private class RootRegistryListener(
        private val channel: Channel<UpnpElement>,
        private val scope: CoroutineScope,
        private val addDevice: (Channel<UpnpElement>, scope: CoroutineScope, Device<*, *, *>) -> Unit
    ) : DefaultRegistryListener() {

        /* Discovery performance optimization for very slow Android devices! */
        override fun remoteDeviceDiscoveryStarted(registry: Registry?, device: RemoteDevice) {
            addDevice(channel, scope, device)
        }

        override fun remoteDeviceDiscoveryFailed(registry: Registry?, device: RemoteDevice?, ex: Exception?) {
            // Nothing to do
        }

        /* End of optimization, you can remove the whole block if your Android handset is fast (>= 600 Mhz) */
        override fun remoteDeviceAdded(registry: Registry?, device: RemoteDevice) {
            addDevice(channel, scope, device)
        }

        override fun remoteDeviceRemoved(registry: Registry?, device: RemoteDevice?) {
            // Nothing to do
        }

        override fun localDeviceAdded(registry: Registry?, device: LocalDevice) {
            addDevice(channel, scope, device)
        }

        override fun localDeviceRemoved(registry: Registry?, device: LocalDevice?) {
            // Nothing to do
        }
    }

    companion object {
        private val TAG = UpnpServiceConnection::class.java.simpleName
    }
}

class UpnpException(upnpResponse: UpnpResponse) : Throwable(upnpResponse.statusMessage)
