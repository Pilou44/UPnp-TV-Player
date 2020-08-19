package com.wechantloup.upnpvideoplayer.dataholder

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class VideoElement(
    val isDirectory: Boolean,
    val path: String,
    val name: String,
    val parent: VideoElement?
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        return other is VideoElement && path == other.path && name == other.name
    }
}