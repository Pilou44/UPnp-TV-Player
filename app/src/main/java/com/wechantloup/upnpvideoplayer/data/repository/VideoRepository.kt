package com.wechantloup.upnpvideoplayer.data.repository

import com.wechantloup.upnpvideoplayer.data.content.AppDatabase
import com.wechantloup.upnpvideoplayer.data.dataholder.VideoElement

internal class VideoRepository(db: AppDatabase) {

    private var videoDao: AppDatabase.VideoDao = db.videoDao()

    suspend fun writeVideoElement(video: VideoElement) {
        videoDao.insert(video)
    }

    suspend fun getAllVideo(): List<VideoElement> {
        return videoDao.all()
    }

    suspend fun removeVideo(element: VideoElement): Int {
        return videoDao.delete(element)
    }

}