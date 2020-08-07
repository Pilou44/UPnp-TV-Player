package com.wechantloup.upnpvideoplayer.videoPlayer

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.widget.SeekBar

class MediaSeekBar(
    context: Context,
    attrs: AttributeSet? = null
) : SeekBar(context, attrs) {

    private var onProgressListener: OnProgressChangedByKeyListener? = null

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isEnabled) {
            var increment: Int = max / 100
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MINUS -> {
                    Log.i("TEST", "rewind")
                    increment = -increment
//                    increment = if (isLayoutRtl()) -increment else increment
                    progress += increment
                    onProgressListener?.onProgressChangedByKey(progress)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_EQUALS -> {
                    Log.i("TEST", "forward")
//                    increment = if (isLayoutRtl()) -increment else increment
                    progress += increment
                    onProgressListener?.onProgressChangedByKey(progress)
                    return true
                }
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    fun setProgressChangedByKeyListener(listener: OnProgressChangedByKeyListener) {
        onProgressListener = listener
    }

    interface OnProgressChangedByKeyListener {
        fun onProgressChangedByKey(progress: Int)
    }

    interface MediaPlayer {
        fun pause()
        fun resume()
    }
}