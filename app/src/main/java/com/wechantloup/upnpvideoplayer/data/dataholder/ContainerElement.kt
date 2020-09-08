package com.wechantloup.upnpvideoplayer.data.dataholder

class ContainerElement(
    override val path: String,
    val name: String,
    val parent: ContainerElement?
) : BrowsableElement(path)