package com.wechantloup.upnpvideoplayer.rootSetter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.wechantloup.core.utils.Serializer.serialize
import com.wechantloup.upnp.ControlPointManager
import com.wechantloup.upnp.R
import com.wechantloup.upnp.dataholder.DlnaRoot
import com.wechantloup.upnp.dataholder.UpnpElement
import com.wechantloup.upnpvideoplayer.UPnPApplication
import com.wechantloup.upnpvideoplayer.data.useCase.SetRootUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.mm2d.log.DefaultSender
import net.mm2d.log.Logger

class RootSetterActivity : FragmentActivity() {

    private var selectedElement: UpnpElement? = null
    private lateinit var list: RecyclerView
    private lateinit var adapter: RootSetterAdapter
    private val mAllFiles = mutableListOf<UpnpElement>()
    private val setRootUseCase: SetRootUseCase by lazy { SetRootUseCase((application as UPnPApplication).rootRepository) }

    private val controlPointManager = ControlPointManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_root_setter)

        list = findViewById(R.id.list)
        adapter = RootSetterAdapter(mAllFiles, ::onItemClick)
        list.adapter = adapter

//        setUpLogger()
    }

    private fun setUpLogger() {
        Logger.setLogLevel(Logger.DEBUG)
        Logger.setSender(DefaultSender.create { level, tag, message ->
            GlobalScope.launch(Dispatchers.Main) {
                message.split("\n").forEach {
                    Log.println(level, tag, it)
                }
            }
        })
        DefaultSender.appendThread(true)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onResume() {
        super.onResume()
//        upnpServiceConnection.bind(this)
        val cpManager = ControlPointManager()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                cpManager.findDevices(lifecycleScope).consumeEach { onNewDeviceFound(it) }
            }
        }
    }

    override fun onPause() {
        super.onPause()
//        upnpServiceConnection.unbind(this)
    }

    override fun onBackPressed() {
        val element = selectedElement
        if (element == null) {
            setResult(RESULT_CANCELED)
        } else {
            exportRoot(element)
            setResult(RESULT_OK, intent)
        }
        finish()
    }

    private fun exportRoot(root: UpnpElement) {
        setRootUseCase.execute(root)
    }

    private fun onItemClick(element: UpnpElement) {
        lifecycleScope.launch {
            val position = mAllFiles.indexOf(element)
            val content = controlPointManager.parseAndUpdate(element)
            val folders= content.filter { it.type == UpnpElement.Type.CONTAINER }
            mAllFiles.addAll(position + 1, folders)
            adapter.notifyItemRangeInserted(position + 1, folders.size)
            selectedElement = element
            adapter.setSelectedElement(position)
        }
    }

//    override fun onErrorConnectingServer() {
//        TODO("Not yet implemented")
//    }

//    @OptIn(ExperimentalCoroutinesApi::class)
//    override fun onServiceConnected() {
//        lifecycleScope.launch {
//            Log.i(TAG, "Search for UPnP devices")
//            upnpServiceConnection.findDevices(lifecycleScope).consumeEach { onNewDeviceFound(it) }
//        }
//    }

    private fun onNewDeviceFound(server: UpnpElement) {
        runOnUiThread {
            Log.i(TAG, "New UPnP device found")
            var position: Int = mAllFiles.indexOf(server)
            if (position >= 0) {
                Log.i(TAG, "Replace device")
                // Device already in the list, re-set new value at same position
                mAllFiles.remove(server)
                mAllFiles.add(position, server)
                adapter.notifyItemChanged(position)
            } else {
                Log.i(TAG, "Add new device")
                mAllFiles.add(server)
                position = mAllFiles.indexOf(server)
                adapter.notifyItemInserted(position)
            }
        }
    }

//    override fun onServerConnected(rootContainer: UpnpElement) {
//        TODO("Not yet implemented")
//    }

    companion object {
        private val TAG = RootSetterActivity::class.java.simpleName
        const val ARG_ROOT = "root"
    }
}
