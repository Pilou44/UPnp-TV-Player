package com.wechantloup.upnpvideoplayer.videoPlayer

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.widget.SeekBar
import androidx.lifecycle.LiveData

class MediaSeekBar(
    context: Context,
    attrs: AttributeSet? = null
) : SeekBar(context, attrs) {

    private var onProgressListener: OnProgressChangedByKeyListener? = null
    private lateinit var controls: MediaControls
    private var isPlaying: Boolean? = null

    fun bind(media: MediaControls) {
        controls = media
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!isEnabled) return false

        if (isPlaying == null)
            isPlaying = requireNotNull(controls.isPlaying.value)

        var increment: Int = max / 100
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MINUS -> {
                increment = -increment
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_EQUALS -> {
                progress += increment
            }
            else -> return false
        }
//                    increment = if (isLayoutRtl()) -increment else increment
        isPlaying?.let { controls.pause() }
        progress += increment
        onProgressListener?.onProgressChangedByKey(progress)
        controls.onKeyCatched()
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_EQUALS -> {
                isPlaying?.let {
                    controls.resume()
                    isPlaying = null
                }
                controls.onKeyCatched()
                return true
            }
            else -> return false
        }
    }

    fun setProgressChangedByKeyListener(listener: OnProgressChangedByKeyListener) {
        onProgressListener = listener
    }

    interface OnProgressChangedByKeyListener {
        fun onProgressChangedByKey(progress: Int)
    }

    interface MediaControls {
        val isPlaying: LiveData<Boolean>
        fun onKeyCatched()
        fun pause()
        fun resume()
    }
}