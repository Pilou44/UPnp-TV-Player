package com.wechantloup.upnp.dataholder

open class UpnpElement(
    val type: Type,
    open val path: String,
    open val name: String,
    open val parent: UpnpElement?
) {
    override fun equals(other: Any?): Boolean {
        return other is UpnpElement && path == other.path
    }

    enum class Type {
        FILE,
        CONTAINER
    }
}