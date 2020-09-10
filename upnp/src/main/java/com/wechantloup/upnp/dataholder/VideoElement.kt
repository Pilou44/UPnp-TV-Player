package com.wechantloup.upnp.dataholder

data class VideoElement(
    override val path: String,
    override val name: String,
    val parent: ContainerElement
) : UpnpElement(path, name, parent.path)