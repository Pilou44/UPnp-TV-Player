package com.wechantloup.upnpvideoplayer.data.dataholder

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class BrowsableVideoElement(
    val isDirectory: Boolean,
    val path: String,
    val name: String,
    val parent: BrowsableVideoElement?
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        return other is VideoElement && path == other.path && name == other.name
    }
}