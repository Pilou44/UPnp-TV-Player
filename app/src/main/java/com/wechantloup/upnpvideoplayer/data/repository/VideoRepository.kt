package com.wechantloup.upnpvideoplayer.data.repository

import com.wechantloup.upnpvideoplayer.data.content.AppDatabase
import com.wechantloup.upnpvideoplayer.data.dataholder.StartedVideoElement

internal class VideoRepository(db: AppDatabase) {

    private var videoDao: AppDatabase.VideoDao = db.videoDao()

    suspend fun writeVideoElement(video: StartedVideoElement) {
        videoDao.insert(video)
    }

    suspend fun getAllVideo(): List<StartedVideoElement> {
        return videoDao.all()
    }

    suspend fun removeVideo(element: StartedVideoElement): Int {
        return videoDao.delete(element)
    }

}