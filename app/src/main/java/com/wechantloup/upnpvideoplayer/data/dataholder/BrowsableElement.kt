package com.wechantloup.upnpvideoplayer.data.dataholder

import androidx.room.Ignore

open class BrowsableElement(
    @Ignore open val path: String
) {
    override fun equals(other: Any?): Boolean {
        return other is BrowsableElement && path == other.path
    }
}