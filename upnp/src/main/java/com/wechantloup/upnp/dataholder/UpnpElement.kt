package com.wechantloup.upnp.dataholder

data class UpnpElement(
    val type: Type,
    val path: String,
    val name: String,
    val parent: UpnpElement?,
    val udn: String,
    val location: String
) {
    enum class Type {
        FILE,
        CONTAINER
    }
}
