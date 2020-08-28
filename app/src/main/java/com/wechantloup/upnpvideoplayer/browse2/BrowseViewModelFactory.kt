package com.wechantloup.upnpvideoplayer.browse2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wechantloup.upnpvideoplayer.UPnPApplication
import com.wechantloup.upnpvideoplayer.data.repository.ThumbnailRepository
import com.wechantloup.upnpvideoplayer.data.repository.VideoRepository

internal class BrowseViewModelFactory(
    private val videoRepository: VideoRepository,
    private val thumbnailRepository: ThumbnailRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return BrowseViewModel(
            videoRepository,
            thumbnailRepository
        ) as T
    }

    companion object {
        fun createViewModelFactory(
            application: UPnPApplication
        ): ViewModelProvider.Factory {

            return BrowseViewModelFactory(
                application.videoRepository,
                ThumbnailRepository(application)
            )
        }
    }
}
