package com.wechantloup.upnpvideoplayer.data.dataholder

class UpnpContainerData(
    val container: ContainerElement,
    val folders: List<ContainerElement>,
    val movies: List<BrowsableVideoElement>
)