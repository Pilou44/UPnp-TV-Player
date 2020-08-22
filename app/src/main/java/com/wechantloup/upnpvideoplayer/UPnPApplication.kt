package com.wechantloup.upnpvideoplayer

import android.app.Application
import androidx.room.Room
import com.wechantloup.upnpvideoplayer.data.content.AppDatabase
import com.wechantloup.upnpvideoplayer.data.repository.VideoRepository

internal class UPnPApplication: Application() {

    lateinit var videoRepository: VideoRepository
        private set

    override fun onCreate() {
        super.onCreate()

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            applicationContext.packageName
        ).build()
        videoRepository = VideoRepository(db)
    }
}