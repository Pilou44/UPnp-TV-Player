package com.wechantloup.upnp

import android.net.nsd.NsdManager
import android.util.Log
import com.wechantloup.upnp.dataholder.UpnpElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import net.mm2d.upnp.ControlPoint
import net.mm2d.upnp.ControlPointFactory
import net.mm2d.upnp.Device
import net.mm2d.upnp.Service
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.StringReader
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ControlPointManager {

    private val cp: ControlPoint = ControlPointFactory.create().also {
        // adding listener if necessary.
//            it.addDiscoveryListener(this)
        it.initialize()
        it.start()
    }

    fun findDevices(scope: CoroutineScope): Channel<UpnpElement> {
        val channel = Channel<UpnpElement>()
        cp.addDiscoveryListener(object : ControlPoint.DiscoveryListener {
            override fun onDiscover(device: Device) {
                if (device.deviceType == "urn:schemas-upnp-org:device:MediaServer:1") {
                    val server = UpnpElement(
                        UpnpElement.Type.CONTAINER,
                        "0",
                        device.modelName,
                        null,
                        device.udn,
                        device.location
                    )
                    scope.launch {
                        channel.send(server)
                    }
                }
            }

            override fun onLost(device: Device) {
                TODO("Not yet implemented")
            }
        })
        cp.search()
        return channel
    }

    suspend fun parseAndUpdate(element: UpnpElement): List<UpnpElement> {
        if (element.type == UpnpElement.Type.FILE) return emptyList()

        var device = cp.getDevice(element.udn)

        if (device != null) {
            return browseDevice(device, element)
        }

        return suspendCoroutine { continuation ->
            cp.addDiscoveryListener(object : ControlPoint.DiscoveryListener {
                override fun onDiscover(device: Device) {
                    val browse = device.findAction("Browse")
                    browse?.invoke(
                        mapOf(
                            "ObjectID" to element.path,
                            "BrowseFlag" to "BrowseDirectChildren",
                            "Filter" to "*",
                            "StartingIndex" to "0",
                            "RequestedCount" to "0",
                            "SortCriteria" to ""
                        ),
                        onResult = {
                            val resultXml = it["Result"]// get result
                            continuation.resume(parseXml(resultXml, element))
                        },
                        onError = {
                            continuation.resumeWithException(it)
                        }
                    )
                }

                override fun onLost(device: Device) {
                    continuation.resumeWithException(Exception("Device not found"))
                }
            })
            cp.tryAddDevice(element.udn, element.location)
        }
    }

    private suspend fun browseDevice(device: Device, element: UpnpElement): List<UpnpElement> {
        val browse = device.findAction("Browse")

        return suspendCoroutine { continuation ->
            browse?.invoke(
                mapOf(
                    "ObjectID" to element.path,
                    "BrowseFlag" to "BrowseDirectChildren",
                    "Filter" to "*",
                    "StartingIndex" to "0",
                    "RequestedCount" to "0",
                    "SortCriteria" to ""
                ),
                onResult = {
                    val resultXml = it["Result"]// get result
                    continuation.resume(parseXml(resultXml, element))
                },
                onError = {
                    continuation.resumeWithException(it)
                }
            )
        }
    }

    private fun parseXml(xmlString: String?, parent: UpnpElement): List<UpnpElement> {
        if (xmlString == null) return emptyList()

        val elements = mutableListOf<UpnpElement>()

        try {
            var gettingTitle = false
            var gettingUrl = false
            var id = ""
            var title = ""
            var url = ""

            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val xpp = factory.newPullParser()
            xpp.setInput(StringReader(xmlString)) // pass input whatever xml you have
            var eventType = xpp.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
//                    Log.d(TAG, "Start document")
                } else if (eventType == XmlPullParser.START_TAG) {
//                    Log.d(TAG, "Start tag " + xpp.name)
                    when (xpp.name) {
                        "container",
                        "item" -> id = xpp.getAttributeValue("", "id")
                        "title" -> gettingTitle = true
                        "res" -> gettingUrl = true
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
//                    Log.d(TAG, "End tag " + xpp.name)
                    if (xpp.name == "container") {
                        val upnpElement = UpnpElement(
                            UpnpElement.Type.CONTAINER,
                            id,
                            title,
                            parent,
                            parent.udn,
                            parent.location
                        )
                        elements.add(upnpElement)
                    } else if (xpp.name == "item") {
                        val upnpElement = UpnpElement(
                            UpnpElement.Type.FILE,
                            url,
                            title,
                            parent,
                            parent.udn,
                            parent.location
                        )
                        elements.add(upnpElement)
                    }
                } else if (eventType == XmlPullParser.TEXT) {
//                    Log.d(TAG, "Text " + xpp.text) // here you get the text from xml
                    if (gettingTitle) {
                        title = xpp.text
                        gettingTitle = false
                    } else if (gettingUrl) {
                        url = xpp.text
                        gettingUrl = false
                    }
                }
                eventType = xpp.next()
            }
            Log.d(TAG, "End document")
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        Log.d(TAG, "${elements.size} elements found")
        return elements
    }

    companion object {
        private const val TAG = "ControlPointManager"
    }
}
