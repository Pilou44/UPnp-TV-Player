package com.wechantloup.upnpvideoplayer.browse2

import android.content.Context
import com.wechantloup.upnpvideoplayer.data.dataholder.BrowsableVideoElement

internal interface BrowseContract {
    interface View {
        fun displayContent(
            title: String,
            directories: List<BrowsableVideoElement>,
            movies: List<BrowsableVideoElement>,
            selectedElement: BrowsableVideoElement?
        )
    }

    interface ViewModel {
        fun setView(view: View)
        fun onViewResumed(context: Context)
        fun onViewPaused(context: Context)
        fun parse(item: BrowsableVideoElement)
        fun goBack(): Boolean
    }
}