package com.wechantloup.upnpvideoplayer.data.dataholder

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.wechantloup.upnp.dataholder.UpnpElement
import com.wechantloup.upnp.dataholder.VideoElement

@Entity(tableName = "video")
data class StartedVideoElement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @Ignore override val path: String,
    @Ignore override val parentPath: String,
    @Ignore override val name: String,
    val position: Long,
    val date: Long
) : UpnpElement(path, name, parentPath) {

    constructor(element: VideoElement, position: Long, time: Long) : this(
        0,
        element.path,
        element.parentPath,
        element.name,
        position,
        time
    )
}