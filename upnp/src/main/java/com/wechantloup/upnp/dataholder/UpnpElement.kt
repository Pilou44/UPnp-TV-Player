package com.wechantloup.upnp.dataholder

open class UpnpElement(
    open val path: String,
    open val name: String,
    open val parentPath: String
) {
    override fun equals(other: Any?): Boolean {
        return other is UpnpElement && path == other.path
    }
}