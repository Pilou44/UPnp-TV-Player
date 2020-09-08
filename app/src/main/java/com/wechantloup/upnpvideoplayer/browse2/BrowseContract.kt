package com.wechantloup.upnpvideoplayer.browse2

import android.content.Context
import android.net.Uri
import com.wechantloup.upnpvideoplayer.data.dataholder.BrowsableElement
import com.wechantloup.upnpvideoplayer.data.dataholder.BrowsableVideoElement
import com.wechantloup.upnpvideoplayer.data.dataholder.ContainerElement
import com.wechantloup.upnpvideoplayer.data.dataholder.StartedVideoElement
import com.wechantloup.upnpvideoplayer.data.dataholder.VideoElement

internal interface BrowseContract {
    interface View {
        fun displayContent(
            title: String,
            startedMovies: List<StartedVideoElement>,
            directories: List<ContainerElement>,
            movies: List<BrowsableVideoElement>,
            selectedElement: BrowsableElement?
        )
        fun launch(
            movies: ArrayList<VideoElement.ParcelableElement>,
            index: Int,
            position: Long
        )
        fun refreshItem(item: Any)
    }

    interface ViewModel {
        fun setView(view: View)
        fun onViewResumed(context: Context)
        fun onViewPaused(context: Context)
        fun parse(item: ContainerElement)
        fun goBack(): Boolean
        fun getThumbnail(item: VideoElement): Uri?
        fun launch(element: VideoElement, position: Long = 0)
        fun setLastPlayedElement(lastPlayedElement: VideoElement)
    }
}