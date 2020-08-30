package com.wechantloup.upnpvideoplayer.data.dataholder

class ContainerElement(
    val path: String,
    val name: String,
    val parent: ContainerElement?
) {
    override fun equals(other: Any?): Boolean {
        return other is ContainerElement && path == other.path && name == other.name
    }
}