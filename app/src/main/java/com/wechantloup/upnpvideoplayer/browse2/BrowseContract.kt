package com.wechantloup.upnpvideoplayer.browse2

import android.content.Context
import android.net.Uri
import com.wechantloup.upnp.dataholder.UpnpElement
import com.wechantloup.upnp.dataholder.VideoElement
import com.wechantloup.upnp.dataholder.ContainerElement
import com.wechantloup.upnp.dataholder.DlnaRoot
import com.wechantloup.upnp.dataholder.PlayableItem
import com.wechantloup.upnpvideoplayer.data.dataholder.StartedVideoElement

internal interface BrowseContract {
    interface View {
        fun displayContent(
            title: String,
            startedMovies: List<StartedVideoElement>,
            directories: List<ContainerElement>,
            movies: List<VideoElement>,
            selectedElement: UpnpElement?
        )
        fun launch(
            playableItem: PlayableItem,
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
        fun launch(element: UpnpElement, position: Long = 0)
        fun setLastPlayedElementPath(lastPlayedElement: String)
        fun resetRoot(newRoot: DlnaRoot)
    }
}