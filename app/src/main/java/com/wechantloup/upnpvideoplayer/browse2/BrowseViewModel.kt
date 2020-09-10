package com.wechantloup.upnpvideoplayer.browse2

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wechantloup.upnpvideoplayer.upnp.UpnpServiceConnection
import com.wechantloup.upnpvideoplayer.data.GetRootUseCase
import com.wechantloup.upnpvideoplayer.data.dataholder.BrowsableElement
import com.wechantloup.upnpvideoplayer.data.dataholder.BrowsableVideoElement
import com.wechantloup.upnpvideoplayer.data.dataholder.ContainerElement
import com.wechantloup.upnpvideoplayer.data.dataholder.DlnaRoot
import com.wechantloup.upnpvideoplayer.data.dataholder.StartedVideoElement
import com.wechantloup.upnpvideoplayer.data.dataholder.VideoElement
import com.wechantloup.upnpvideoplayer.data.repository.ThumbnailRepository
import com.wechantloup.upnpvideoplayer.data.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class BrowseViewModel(
    private val videoRepository: VideoRepository,
    private val thumbnailRepository: ThumbnailRepository,
    private val getRootUseCase: GetRootUseCase
) : ViewModel(), BrowseContract.ViewModel, UpnpServiceConnection.Callback {

    private lateinit var view: BrowseContract.View

    private val upnpServiceConnection = UpnpServiceConnection(
        getRootUseCase.execute(),
        viewModelScope,
        this
    )

    private val requestChannel: Channel<VideoElement> = Channel()

    init {
        viewModelScope.launch {
            @Suppress("EXPERIMENTAL_API_USAGE")
            requestChannel.consumeEach { retrieveThumbnail(it) }
        }
    }

    override fun setView(view: BrowseContract.View) {
        this.view = view
    }

    override fun onViewResumed(context: Context) {
        upnpServiceConnection.bind(context)
    }

    override fun onViewPaused(context: Context) {
        upnpServiceConnection.unbind(context)
    }

    override fun parse(item: ContainerElement) {
        viewModelScope.launch {
            upnpServiceConnection.parseAndUpdate(item, null)
        }
    }

    override fun goBack(): Boolean {
        return upnpServiceConnection.browseParent()
    }

    override fun launch(element: VideoElement, position: Long) {
        viewModelScope.launch {
            if (element is StartedVideoElement) {
                videoRepository.removeVideo(element)
            }
            upnpServiceConnection.launch(element, position)
        }
    }

    override fun setLastPlayedElement(lastPlayedElement: VideoElement) {
        upnpServiceConnection.setLastPlayedElement(lastPlayedElement)
    }

    override fun resetRoot(newRoot: DlnaRoot) {
        upnpServiceConnection.resetRoot(newRoot)
    }

    override fun getThumbnail(item: VideoElement): Uri? {
        val uri: Uri? = thumbnailRepository.getElementThumbnail(item)
        uri?.let { return it }
        viewModelScope.launch {
            requestChannel.send(item)
        }
        Log.i(TAG, "Return null for ${item.name}")
        return null
    }

    override fun setNewContent(
        title: String,
        directories: List<ContainerElement>,
        movies: List<BrowsableVideoElement>,
        selectedElement: BrowsableElement?
    ) {
        viewModelScope.launch {
            val startedMovies = videoRepository.getAllVideo()
            view.displayContent(title, startedMovies, directories, movies, selectedElement)
        }
    }

    override fun launch(movies: ArrayList<VideoElement.ParcelableElement>, index: Int, position: Long) {
        view.launch(movies, index, position)
    }

    private suspend fun retrieveThumbnail(element: VideoElement) {
        val uri: Uri? = thumbnailRepository.getElementThumbnail(element)
        uri?.let { return }

        var bitmap: Bitmap? = null
        Log.i(TAG, "Launch coroutine for ${element.name}")
        withContext(Dispatchers.IO) {
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(element.path, HashMap<String, String>())
                val duration: String? = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val timeToCatch = extractTimeToCapture(duration)
                bitmap = mmr.getFrameAtTime(timeToCatch * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                Log.i("MMR", "For item ${element.name}: duration = $duration, bitmap is null: ${bitmap == null}")
            } catch (e: RuntimeException) {
                e.printStackTrace()
            } finally {
                mmr.release()
            }
            bitmap?.let {
                thumbnailRepository.writeElementThumbnail(it, element)
                view.refreshItem(element)
            }
        }
    }

    private fun extractTimeToCapture(duration: String?): Long {
        if (duration == null) return DEFAULT_TIME_TO_CAPTURE

        val longDuration: Long
        try {
            longDuration = duration.toLong()
        } catch (e: NumberFormatException) {
            return DEFAULT_TIME_TO_CAPTURE
        }
        if (longDuration < 0L) return DEFAULT_TIME_TO_CAPTURE

        return longDuration * 100L / PERCENT_FOR_CAPTURE
    }

    companion object {
        private val TAG = BrowseViewModel::class.java.simpleName
        private const val DEFAULT_TIME_TO_CAPTURE: Long = 20000L // 20s
        private const val PERCENT_FOR_CAPTURE: Long = 25L // 25%
    }
}