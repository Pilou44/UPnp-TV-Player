package com.wechantloup.upnpvideoplayer.data.dataholder

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video")
data class VideoElement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val path: String,
    val pathToDirectory: String,
    val name: String,
    val position: Long,
    val date: Long
) {

    constructor(browsableElement: BrowsableVideoElement, position: Long, time: Long) : this(
        0,
        browsableElement.path,
        browsableElement.path.substring(0, browsableElement.path.lastIndexOf("/")),
        browsableElement.name,
        position,
        time
    )
}