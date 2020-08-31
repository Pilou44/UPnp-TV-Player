package com.wechantloup.upnpvideoplayer.data.dataholder

import android.os.Parcelable
import androidx.room.Ignore
import kotlinx.android.parcel.Parcelize

open class VideoElement(
    @Ignore open val path: String,
    @Ignore open val parentPath : String,
    @Ignore open val name: String
){

    @Parcelize
    class ParcelableElement(
        override val path: String,
        override val parentPath : String,
        override val name: String
    ) : VideoElement(path, parentPath, name), Parcelable

}