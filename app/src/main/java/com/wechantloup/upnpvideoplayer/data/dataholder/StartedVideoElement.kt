package com.wechantloup.upnpvideoplayer.data.dataholder

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "video")
data class StartedVideoElement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @Ignore override val path: String,
    val containerId: String,
    @Ignore override val name: String,
    val position: Long,
    val date: Long
) : VideoElement(path, name) {

    constructor(browsableElement: BrowsableVideoElement, position: Long, time: Long) : this(
        0,
        browsableElement.path,
        browsableElement.parent!!.path,
        browsableElement.name,
        position,
        time
    )
}