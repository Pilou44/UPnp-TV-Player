package com.wechantloup.upnpvideoplayer.data.dataholder

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class BrowsableVideoElement(
    val isDirectory: Boolean,
    override val path: String,
    override val name: String,
    val parent: BrowsableVideoElement?
) : VideoElement(path, name), Parcelable {

    override fun equals(other: Any?): Boolean {
        return other is BrowsableVideoElement && path == other.path && name == other.name
    }
}