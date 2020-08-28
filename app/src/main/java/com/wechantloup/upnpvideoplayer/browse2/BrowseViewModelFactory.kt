package com.wechantloup.upnpvideoplayer.browse2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wechantloup.upnpvideoplayer.UPnPApplication
import com.wechantloup.upnpvideoplayer.data.repository.CacheRepository
import com.wechantloup.upnpvideoplayer.data.repository.VideoRepository

internal class BrowseViewModelFactory(
    private val videoRepository: VideoRepository,
    private val cacheRepository: CacheRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return BrowseViewModel(
            videoRepository,
            cacheRepository
        ) as T
    }

    companion object {
        fun createViewModelFactory(
            application: UPnPApplication
        ): ViewModelProvider.Factory {

            return BrowseViewModelFactory(
                application.videoRepository,
                CacheRepository(application)
            )
        }
    }
}
