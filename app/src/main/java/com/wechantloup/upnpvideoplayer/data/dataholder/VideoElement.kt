package com.wechantloup.upnpvideoplayer.data.dataholder

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video")
data class VideoElement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val isDirectory: Boolean,
    val path: String,
    val name: String
)