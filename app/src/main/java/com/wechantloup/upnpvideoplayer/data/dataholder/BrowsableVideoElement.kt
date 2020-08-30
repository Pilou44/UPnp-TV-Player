package com.wechantloup.upnpvideoplayer.data.dataholder

data class BrowsableVideoElement(
    override val path: String,
    override val name: String,
    val parent: ContainerElement
) : VideoElement(path, parent.path, name) {

    override fun equals(other: Any?): Boolean {
        return other is BrowsableVideoElement && path == other.path && name == other.name
    }
}