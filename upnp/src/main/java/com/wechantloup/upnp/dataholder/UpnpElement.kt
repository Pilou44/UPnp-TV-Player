package com.wechantloup.upnp.dataholder

data class UpnpElement(
    val type: Type,
    val path: String,
    val name: String,
    val parent: UpnpElement?,
    val server: DlnaServer
) {
    override fun equals(other: Any?): Boolean {
        return other is UpnpElement && path == other.path && server.info.mUrl == other.server.info.mUrl
    }

    enum class Type {
        FILE,
        CONTAINER
    }
}