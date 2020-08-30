package com.wechantloup.upnpvideoplayer.videoPlayer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.UPnPApplication
import com.wechantloup.upnpvideoplayer.data.dataholder.StartedVideoElement
import com.wechantloup.upnpvideoplayer.data.dataholder.VideoElement
import com.wechantloup.upnpvideoplayer.videoPlayer.ControlsOverlay.ControlsOverlayListener
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.util.ArrayList

class VideoPlayerActivity : AppCompatActivity(), ControlsOverlayListener {
    private var mVideoLayout: VLCVideoLayout? = null
    private var mLibVLC: LibVLC? = null
    private var mMediaPlayer: MediaPlayer? = null
    private var controls: ControlsOverlay? = null
    private var list: ArrayList<VideoElement.ParcelableElement>? = null
    private var current: VideoElement.ParcelableElement? = null
    private var index = 0
    private var position = 0L
    override val duration = MutableLiveData<Long>()
    override val time = MutableLiveData<Long>()
    override val progress = MutableLiveData<Float>()
    override val isPlaying = MutableLiveData<Boolean>()
    private var next = false
    private var loop = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        loop = prefs.getBoolean("loop", false)
        next = prefs.getBoolean("next", true)
        val args = ArrayList<String>()
        args.add("-vvv")
        mLibVLC = LibVLC(this, args)
        mMediaPlayer = MediaPlayer(mLibVLC)
        mVideoLayout = findViewById(R.id.video_layout)
        controls = findViewById(R.id.controls)
        list = intent.getParcelableArrayListExtra(EXTRA_URLS)
        index = intent.getIntExtra(EXTRA_INDEX, 0) - 1
        position = intent.getLongExtra(EXTRA_POSITION, 0L)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        Log.i(TAG, "Key down, code=$keyCode")
        return if (keyCode == KeyEvent.KEYCODE_BACK) {
            super.onKeyDown(keyCode, event)
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
        ) {
            if (isPlaying.value!!) {
                pause()
            } else {
                resume()
            }
            true
        } else if (!controls!!.isOpened) {
            controls!!.show(this)
            true
        } else {
            controls!!.launchTimer()
            super.onKeyDown(keyCode, event)
        }
    }

    override fun onBackPressed() {
        if (controls!!.isOpened) {
            controls!!.hide()
        } else {
            saveCurrent()
            finishWithResult()
        }
    }

    private fun saveCurrent() {
        val current = list!![index]
        val currentPosition = mMediaPlayer!!.time
        val currentTime = System.currentTimeMillis()
        val savableElement = StartedVideoElement(current, currentPosition, currentTime)
        lifecycleScope.launch {
            Log.i("TEST", "Save an element")
            (applicationContext as UPnPApplication).videoRepository.writeVideoElement(savableElement)
        }
    }

    private fun finishWithResult() {
        val returnIntent = Intent()
        returnIntent.putExtra(ELEMENT, current)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mMediaPlayer!!.release()
        mLibVLC!!.release()
    }

    override fun onStart() {
        super.onStart()
        mMediaPlayer!!.attachViews(
            mVideoLayout!!,
            null,
            ENABLE_SUBTITLES,
            USE_TEXTURE_VIEW
        )
        mMediaPlayer!!.setEventListener(object : MediaPlayer.EventListener {
            private var audioTracksCount = -1
            private var spuTracksCount = -1
            override fun onEvent(event: MediaPlayer.Event) {
                Log.i(TAG, "Video event: ${event.type}")
                if (event.type == MediaPlayer.Event.Stopped) {
                    Log.i(TAG, "Video stopped")
                    if (!next) {
                        finishWithResult()
                        return
                    } else if (!loop && index == list!!.size - 1) {
                        finishWithResult()
                        return
                    }
                    next()
                } else if (event.type == MediaPlayer.Event.Playing) {
                    isPlaying.setValue(true)
                } else if (event.type == MediaPlayer.Event.Buffering) {
                    if (position > 0) {
                        mMediaPlayer!!.time = position
                        position = 0L
                    }
                } else if (event.type == MediaPlayer.Event.Paused) {
                    isPlaying.setValue(false)
                } else if (event.type == MediaPlayer.Event.LengthChanged) {
//                    Log.i(TAG, "LengthChanged event");
                    duration.setValue(event.lengthChanged)
                } else if (event.type == MediaPlayer.Event.TimeChanged) {
//                    Log.i(TAG, "TimeChanged event " + event.getTimeChanged());
                    time.setValue(event.timeChanged)
                } else if (event.type == MediaPlayer.Event.PositionChanged) {
//                    Log.i(TAG, "PositionChanged event " + event.getPositionChanged());
                    progress.value = event.positionChanged
                }
                val newAudioTracksCount = mMediaPlayer!!.audioTracksCount
                if (newAudioTracksCount != audioTracksCount) {
                    Log.i(
                        TAG,
                        "Audio track count: " + mMediaPlayer!!.audioTracksCount
                    )
                    audioTracksCount = newAudioTracksCount
                    val audioTracks = mMediaPlayer!!.audioTracks
                    var frenchTrack = -1
                    if (audioTracksCount > 0) {
                        for (audioTrack in audioTracks) {
                            Log.i(TAG, "Audio track: " + audioTrack.name)
                            Log.i(TAG, "Audio track: " + audioTrack.id)
                            if (audioTrack.name.toLowerCase().contains("vf") || audioTrack.name.toLowerCase()
                                    .contains("fr")
                            ) {
                                frenchTrack = audioTrack.id
                            }
                        }
                    }
                    if (frenchTrack >= 0) {
                        Log.i(
                            TAG,
                            "Set french audio track: " + mMediaPlayer!!.setAudioTrack(frenchTrack)
                        )
                        //                        mMediaPlayer.setAudioTrack(2);
                    }
                }
                val newSpuTracks = mMediaPlayer!!.spuTracksCount
                if (newSpuTracks != spuTracksCount) {
                    Log.i(
                        TAG,
                        "SPU track count: " + mMediaPlayer!!.audioTracksCount
                    )
                    spuTracksCount = newSpuTracks
                    val spuTracks = mMediaPlayer!!.spuTracks
                    if (spuTracksCount > 0) {
                        for (spuTrack in spuTracks) {
                            Log.i(TAG, "SPU track: " + spuTrack.name)
                            Log.i(TAG, "Audio track: " + spuTrack.id)
                        }
                    }
                    if (spuTracksCount > 0) {
                        Log.i(
                            TAG,
                            "Set spu track: " + spuTracks[0].name + " " + mMediaPlayer!!.setSpuTrack(spuTracks[0].id)
                        )
                        //                        mMediaPlayer.setSpuTrack(0);
                    }
                }
            }
        })
        next()
    }

    override fun onStop() {
        super.onStop()
        mMediaPlayer!!.stop()
        mMediaPlayer!!.detachViews()
    }

    override operator fun next() {
        index = (index + 1) % list!!.size
        current = list!![index]
        playCurrent()
    }

    override fun previous() {
        index = (index - 1 + list!!.size) % list!!.size
        current = list!![index]
        playCurrent()
    }

    private fun playCurrent() {
        val path = current!!.path
        val media = Media(mLibVLC, Uri.parse(path))
        mMediaPlayer!!.media = media
        media.release()
        mMediaPlayer!!.play()
    }

    override fun setPosition(progress: Float) {
        mMediaPlayer!!.position = progress
    }

    override val owner: LifecycleOwner
        get() = this

    override fun pause() {
        mMediaPlayer!!.pause()
    }

    override fun resume() {
        mMediaPlayer!!.play()
    }

    companion object {
        const val ELEMENT = "CURRENT_ELEMENT"
        private const val USE_TEXTURE_VIEW = false
        private const val ENABLE_SUBTITLES = true
        const val EXTRA_URLS = "urls"
        const val EXTRA_INDEX = "index"
        const val EXTRA_POSITION = "position"
        private val TAG = VideoPlayerActivity::class.java.simpleName
    }
}
