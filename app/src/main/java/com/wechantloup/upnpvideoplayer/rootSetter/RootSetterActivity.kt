package com.wechantloup.upnpvideoplayer.rootSetter

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
import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.dataholder.DlnaElement
import com.wechantloup.upnpvideoplayer.dataholder.DlnaRoot
import com.wechantloup.upnpvideoplayer.utils.Serializer.serialize
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

class RootSetterActivity : Activity() {

    private var selectedElement: DlnaElement? = null
    private lateinit var list: RecyclerView
    private lateinit var adapter: RootSetterAdapter
    private var bound: Boolean = false
    val mHandler = Handler()
    private var mUpnpService: AndroidUpnpService? = null
    private val mRegistryListener = BrowseRegistryListener(mHandler, ::addDevice)
    private val mAllFiles = mutableListOf<DlnaElement>()

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.i(TAG, "Service connected")
            val upnpService = (service as AndroidUpnpService)
                .also { mUpnpService = it }

            // Get ready for future device advertisements
            upnpService.registry.addListener(mRegistryListener)

            // Now add all devices to the list we already know about
            for (device in upnpService.registry.devices) {
                mRegistryListener.newDeviceAdded(device)
            }

            // Search asynchronously for all devices, they will respond soon
            upnpService.controlPoint.search()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Log.i(TAG, "Service disconnected")
            mUpnpService = null
        }

        override fun onBindingDied(name: ComponentName?) {
            Log.i(TAG, "Service onBindingDied")
        }

        override fun onNullBinding(name: ComponentName?) {
            Log.i(TAG, "Service onNullBinding")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_root_setter)

        list = findViewById(R.id.list)
        adapter = RootSetterAdapter(mAllFiles, ::onItemClick)
        list.adapter = adapter
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

    override fun onDestroy() {
        exportRoot()
        super.onDestroy()
    }

    private fun exportRoot() {
        val element = selectedElement ?: return
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val root = DlnaRoot(
            element.name,
            element.udn,
            element.url,
            element.path,
            element.maxAge
        )
        prefs.edit().putString("ROOT", root.serialize()).apply()
    }

    private fun onItemClick(element: DlnaElement) {
        val position = mAllFiles.indexOf(element)
        if (!element.isExpanded) {
            mUpnpService!!.controlPoint
                .execute(object : Browse(element.service, element.path, BrowseFlag.DIRECT_CHILDREN) {
                    override fun received(
                        arg0: ActionInvocation<*>?,
                        didl: DIDLContent
                    ) {
                        mHandler.post {
                            val itemsQuantity = didl.containers.size
                            Log.i(TAG, "found $itemsQuantity items.")
                            for (i in didl.containers.indices) {
                                val newElement = DlnaElement(
                                    didl.containers[i].title,
                                    didl.containers[i].id,
                                    element
                                )
                                mAllFiles.add(position + i + 1, newElement)
                            }
                            adapter.notifyItemRangeInserted(position + 1, itemsQuantity)
                        }
                    }

                    override fun updateStatus(status: Status) {}
                    override fun failure(
                        arg0: ActionInvocation<*>?,
                        arg1: UpnpResponse,
                        arg2: String
                    ) {
                    }
                })
            element.isExpanded = true
        }
        selectedElement = element
        adapter.setSelectedElement(position)
    }

    private fun addDevice(device: Device<*,*,*>) {
        if (device.isFullyHydrated) {
            for (service in device.services) {
                if (service.serviceType.type == "ContentDirectory") {
                    val d = DlnaElement(
                        device,
                        service as RemoteService
                    )
                    var position: Int = mAllFiles.indexOf(d)
                    if (position >= 0) {
                        Log.i(TAG, "Replace device")
                        // Device already in the list, re-set new value at same position
                        mAllFiles.remove(d)
                        mAllFiles.add(position, d)
                        adapter.notifyItemChanged(position)
                    } else {
                        Log.i(TAG, "Add new device")
                        mAllFiles.add(d)
                        position = mAllFiles.indexOf(d)
                        adapter.notifyItemInserted(position)
                    }
                }
            }
        }
    }

    companion object {
        private val TAG = RootSetterActivity::class.java.simpleName
    }

    internal class BrowseRegistryListener(private val handler: Handler, private var function: (Device<*, *, *>) -> Unit) : DefaultRegistryListener() {

        /* Discovery performance optimization for very slow Android devices! */
        override fun remoteDeviceDiscoveryStarted(registry: Registry?, device: RemoteDevice) {
            newDeviceAdded(device)
        }

        override fun remoteDeviceDiscoveryFailed(
            registry: Registry?,
            device: RemoteDevice?,
            ex: Exception?
        ) {
        }

        /* End of optimization, you can remove the whole block if your Android handset is fast (>= 600 Mhz) */
        override fun remoteDeviceAdded(registry: Registry?, device: RemoteDevice) {
            newDeviceAdded(device)
        }

        override fun remoteDeviceRemoved(registry: Registry?, device: RemoteDevice?) {}
        override fun localDeviceAdded(registry: Registry?, device: LocalDevice) {
            newDeviceAdded(device)
        }

        override fun localDeviceRemoved(registry: Registry?, device: LocalDevice?) {}

        fun newDeviceAdded(device: Device<*,*,*>) {
            handler.post {
                function(device)
            }
        }
    }
}