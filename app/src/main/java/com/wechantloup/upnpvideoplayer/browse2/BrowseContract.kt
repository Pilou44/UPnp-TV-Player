package com.wechantloup.upnpvideoplayer.browse2

import android.content.Context
import android.net.Uri
import com.wechantloup.upnpvideoplayer.data.dataholder.BrowsableVideoElement
import com.wechantloup.upnpvideoplayer.data.dataholder.StartedVideoElement
import com.wechantloup.upnpvideoplayer.data.dataholder.VideoElement

internal interface BrowseContract {
    interface View {
        fun displayContent(
            title: String,
            startedMovies: List<StartedVideoElement>,
            directories: List<BrowsableVideoElement>,
            movies: List<BrowsableVideoElement>,
            selectedElement: BrowsableVideoElement?
        )
        fun launch(
            movies: ArrayList<BrowsableVideoElement>,
            index: Int,
            position: Long
        )

        fun updateStarted(startedMovies: List<StartedVideoElement>)
        fun refreshItem(item: Any)
    }

    interface ViewModel {
        fun setView(view: View)
        fun onViewResumed(context: Context)
        fun onViewPaused(context: Context)
        fun parse(item: BrowsableVideoElement)
        fun goBack(): Boolean
        fun convertToBrowsableVideoElement(item: StartedVideoElement)
        fun getThumbnail(item: VideoElement): Uri?
    }
}