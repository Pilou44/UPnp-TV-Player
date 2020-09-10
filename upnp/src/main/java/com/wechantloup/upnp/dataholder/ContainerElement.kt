package com.wechantloup.upnp.dataholder

class ContainerElement(
    override val path: String,
    override val name: String,
    val parent: ContainerElement?
) : UpnpElement(path, name, parent?.path ?: "")