package com.wechantloup.upnpvideoplayer.data.dataholder

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.wechantloup.upnp.dataholder.UpnpElement

@Entity(tableName = "video")
data class StartedVideoElement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val path: String,
    val containerId: String,
    val name: String,
    val position: Long,
    val date: Long
)