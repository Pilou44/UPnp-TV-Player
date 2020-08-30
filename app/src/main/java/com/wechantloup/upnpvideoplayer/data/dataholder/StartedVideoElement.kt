package com.wechantloup.upnpvideoplayer.data.dataholder

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "video")
data class StartedVideoElement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    override val path: String,
    val containerId: String,
    override val name: String,
    val position: Long,
    val date: Long
) : VideoElement(path, containerId, name) {

    constructor(element: VideoElement, position: Long, time: Long) : this(
        0,
        element.path,
        element.parentPath,
        element.name,
        position,
        time
    )
}