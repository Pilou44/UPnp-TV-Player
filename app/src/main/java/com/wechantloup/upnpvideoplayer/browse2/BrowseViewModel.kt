package com.wechantloup.upnpvideoplayer.browse2

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wechantloup.core.utils.SHA1
import com.wechantloup.upnp.ControlPointManager
import com.wechantloup.upnp.dataholder.UpnpElement
import com.wechantloup.upnpvideoplayer.data.dataholder.AppPlayableItem
import com.wechantloup.upnpvideoplayer.data.dataholder.StartedVideoElement
import com.wechantloup.upnpvideoplayer.data.repository.ThumbnailRepository
import com.wechantloup.upnpvideoplayer.data.repository.VideoRepository
import com.wechantloup.upnpvideoplayer.data.useCase.GetRootUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.IllegalStateException

@OptIn(ExperimentalCoroutinesApi::class)
internal class BrowseViewModel(
    private val videoRepository: VideoRepository,
    private val thumbnailRepository: ThumbnailRepository,
    private val getRootUseCase: GetRootUseCase
) : ViewModel(), BrowseContract.ViewModel {

    private lateinit var view: BrowseContract.View
    private var lastPlayedElement: StartedVideoElement? = null
    private var currentContainer: UpnpElement? = null
    private val requestChannel: Channel<Any> = Channel()
    private val controlPointManager = ControlPointManager()

    init {
        viewModelScope.launch {
            requestChannel.consumeEach { retrieveThumbnail(it) }
        }
        displayFiles()
    }

    override fun setView(view: BrowseContract.View) {
        this.view = view
    }

    override fun onViewResumed(context: Context) {
//        upnpServiceConnection.bind(context)
    }

    override fun onViewPaused(context: Context) {
//        upnpServiceConnection.unbind(context)
    }

    override fun parse(item: UpnpElement) {
        viewModelScope.launch {
            display(item, null)
        }
    }

    private suspend fun display(item: UpnpElement?, selectedElement: Any?) {
        val list = item?.let { controlPointManager.parseAndUpdate(it) } ?: emptyList()
        val startedMovies = videoRepository.getAllVideo()
        view.displayContent(
            item?.name ?: "",
            startedMovies,
            list.filter { it.type == UpnpElement.Type.CONTAINER },
            list.filter { it.type == UpnpElement.Type.FILE },
            selectedElement
        )
        currentContainer = item
    }

    override fun goBack(): Boolean {
        val parent = currentContainer?.parent ?: return false

        viewModelScope.launch {
            display(parent, currentContainer)
        }
        return true
    }

    override fun launch(element: StartedVideoElement, position: Long) {
        viewModelScope.launch {
            videoRepository.removeVideo(element)
        }
        val current = currentContainer ?: return
        val parent = UpnpElement(
            UpnpElement.Type.CONTAINER,
            element.containerId,
            "",
            null,
            current.udn,
            current.location
        )
        val upnpElement = UpnpElement(
            UpnpElement.Type.FILE,
            element.path,
            element.name,
            parent,
            current.udn,
            current.location
        )
        launch(upnpElement, position)
    }

    override fun launch(element: UpnpElement, position: Long) {
        if (element.type == UpnpElement.Type.CONTAINER) return
        viewModelScope.launch {
            val parent = requireNotNull(element.parent)
            val movies = controlPointManager.parseAndUpdate(parent).filter { it.type == UpnpElement.Type.FILE }
            val playableMovies = movies.map {
                val startPosition = if (element.path == it.path) position else 0L
                StartedVideoElement(
                    0,
                    it.path,
                    it.parent!!.path,
                    it.name,
                    startPosition,
                    0
                )
            }
            val index = movies.indexOfFirst { it.path == element.path }
            val item = AppPlayableItem(playableMovies, index)
            view.launch(item)
        }
    }

    override fun setLastPlayedElementPath(lastPlayedElement: StartedVideoElement) {
        this.lastPlayedElement = lastPlayedElement
        viewModelScope.launch {
            videoRepository.writeVideoElement(lastPlayedElement)
        }
        displayFiles()
    }

    override fun resetRoot() {
        Log.i(TAG, "Reset root")
        currentContainer = null
        displayFiles()
    }

    override fun getThumbnail(item: Any): Uri? {
        val sha1 = extractSha1(item) ?: return null
        val uri: Uri? = thumbnailRepository.getElementThumbnail(sha1)
        uri?.let { return it }
        viewModelScope.launch {
            requestChannel.send(item)
        }
        return null
    }

    private fun extractSha1(item: Any): String? {
        val itemName = when (item) {
            is UpnpElement -> {
                if (item.type == UpnpElement.Type.FILE) {
                    item.path
                } else {
                    item.name
                }
            }
            is StartedVideoElement -> item.path
            else -> null
        } ?: return null

        return SHA1(itemName)
    }

    private fun displayFiles() {
        val root = getRootUseCase.execute()

        viewModelScope.launch {
            if (root == null) {
                display(null, null)
                return@launch
            } else if (currentContainer == null) {
                display(root, null)
                return@launch
            }
            display(currentContainer!!, lastPlayedElement)
        }
    }

    private suspend fun retrieveThumbnail(element: Any) {
        val sha1 = extractSha1(element) ?: return
        val uri: Uri? = thumbnailRepository.getElementThumbnail(sha1)
        uri?.let { return }
        val path = element.getPath()
        val name = element.getName()

        var bitmap: Bitmap? = null
        Log.i(TAG, "Launch retrieveThumbnail coroutine for $name")
        withContext(Dispatchers.IO) {
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(path, HashMap<String, String>())
                val duration: String? = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val timeToCatch = extractTimeToCapture(duration)
                bitmap = mmr.getFrameAtTime(timeToCatch * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                Log.i(TAG, "For item $name: duration = $duration, bitmap at $timeToCatch is null: ${bitmap == null}")
            } catch (e: RuntimeException) {
                e.printStackTrace()
            } finally {
                mmr.release()
            }
            bitmap?.let {
                thumbnailRepository.writeElementThumbnail(it, sha1)
                view.refreshItem(element)
            }
        }
        Log.i(TAG, "End of retrieveThumbnail coroutine for $name")
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

        return longDuration / 100L * PERCENT_FOR_CAPTURE
    }

    private fun Any.getPath(): String = when (this) {
            is UpnpElement -> path
            is StartedVideoElement -> path
            else -> throw IllegalStateException()
        }

    private fun Any.getName(): String = when (this) {
            is UpnpElement -> name
            is StartedVideoElement -> name
            else -> throw IllegalStateException()
        }

    companion object {
        private val TAG = BrowseViewModel::class.java.simpleName
        private const val DEFAULT_TIME_TO_CAPTURE: Long = 20000L // 20s
        private const val PERCENT_FOR_CAPTURE: Long = 25L // 25%
    }
}
