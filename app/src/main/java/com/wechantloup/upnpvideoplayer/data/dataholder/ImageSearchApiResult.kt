package com.wechantloup.upnpvideoplayer.data.dataholder

class ImageSearchApiResult(
    val value: List<Item>
) {

    class Item(
        val url: String,
        val thumbnail: String
    )
}