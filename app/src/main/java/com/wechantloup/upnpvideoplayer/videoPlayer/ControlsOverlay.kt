package com.wechantloup.upnpvideoplayer.videoPlayer

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButton
import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.utils.ThreadsafeConstraintsApplier
import com.wechantloup.upnpvideoplayer.utils.TimeUtils
import com.wechantloup.upnpvideoplayer.utils.ViewUtils.startAnimatingConstraints
import java.util.Formatter
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

internal class ControlsOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs), MediaSeekBar.MediaControls {

    private var controlsListener: ControlsOverlayListener? = null
    private var timer = Timer()
    private val constraintSetter = ThreadsafeConstraintsApplier()
    private val content: View
    private val progressView: TextView
    private val durationView: TextView
    private val progressBar: MediaSeekBar
    private val playPauseButton: MaterialButton
    @kotlin.jvm.JvmField var isOpened: Boolean = false
    private val mFormatBuilder: StringBuilder = StringBuilder()
    private val mFormatter: Formatter = Formatter(mFormatBuilder, Locale.getDefault())

    init {
        inflate(context, R.layout.controls_overlay_layout, this)
        content = findViewById(R.id.content)

        playPauseButton = findViewById(R.id.play_pause)
        playPauseButton.setOnClickListener { playPause() }
        val nextButton: Button = findViewById(R.id.next)
        nextButton.setOnClickListener { controlsListener?.next() }
        val previousButton: Button = findViewById(R.id.previous)
        previousButton.setOnClickListener { controlsListener?.previous() }
        progressView = findViewById(R.id.progress)
        durationView = findViewById(R.id.duration)
        progressBar = findViewById(R.id.progress_bar)
        progressBar.setProgressChangedByKeyListener(object: MediaSeekBar.OnProgressChangedByKeyListener {
            override fun onProgressChangedByKey(progress: Int) {
                controlsListener?.setPosition(progress.toFloat() / progressBar.max)
            }
        })
    }

    private fun playPause() {
        val isPlaying = controlsListener?.isPlaying?.value ?: return
        if (isPlaying) controlsListener?.pause() else controlsListener ?.resume()
    }

    fun hide() {
        if (!isOpened) return

        controlsListener?.apply {
            progress.removeObservers(owner)
            duration.removeObservers(owner)
        }
        controlsListener = null
        progressBar.isFocusable = false

        startAnimatingConstraints()
        constraintSetter.applyConstraintsTo(this) {
            it.clear(content.id, ConstraintSet.TOP)
            it.connect(content.id, ConstraintSet.TOP, id, ConstraintSet.BOTTOM)
        }
        isOpened = false
    }

    fun show(listener: ControlsOverlayListener) {
        listener.apply {
            controlsListener = this
            progressBar.bind(this@ControlsOverlay)
            val timeObserver = Observer<Long> { progress ->
                progressView.text = TimeUtils.getStringForTime(mFormatBuilder, mFormatter, progress)
            }
            time.observe(owner, timeObserver)
            val progressObserver = Observer<Float> { progress ->
                progressBar.progress = (progress * progressBar.max).toInt()
            }
            progress.observe(owner, progressObserver)
            val durationObserver = Observer<Long> { duration ->
                durationView.text = TimeUtils.getStringForTime(mFormatBuilder, mFormatter, duration)
            }
            duration.observe(owner, durationObserver)
            val playStateObserver = Observer<Boolean> { isPlaying ->
                if (isPlaying) {
                    playPauseButton.setIconResource(R.drawable.ic_pause)
                } else {
                    playPauseButton.setIconResource(R.drawable.ic_play)
                }
            }
            isPlaying.observe(owner, playStateObserver);
        }
        progressBar.isFocusable = true

        startAnimatingConstraints()
        constraintSetter.applyConstraintsTo(this) {
            it.clear(content.id, ConstraintSet.TOP)
            it.connect(content.id, ConstraintSet.TOP, id, ConstraintSet.TOP)
        }
        playPauseButton.requestFocus()
        isOpened = true
        launchTimer()
    }

    fun launchTimer() {
        try {
            timer.cancel()
        } catch (e: IllegalStateException) {}
        timer = Timer()
        timer.schedule(HideTask(this), 10000)
    }

    private class HideTask(private val controls: ControlsOverlay) : TimerTask() {
        override fun run() {
            val handler: Handler? = controls.handler
            handler?.post {
                controls.hide()
            }
        }
    }

    interface ControlsOverlayListener {
        val owner: LifecycleOwner
        val duration: LiveData<Long>
        val progress: LiveData<Float>
        val time: LiveData<Long>
        val isPlaying: LiveData<Boolean>
        fun pause()
        fun resume()
        fun next()
        fun previous()
        fun setPosition(progress: Float)
    }

    companion object {
        private val TAG = ControlsOverlay::class.java.simpleName
    }

    override val isPlaying: LiveData<Boolean>
        get() = requireNotNull(controlsListener).isPlaying

    override fun onKeyCatched() {
        launchTimer()
    }

    override fun pause() {
        controlsListener?.pause()
    }

    override fun resume() {
        controlsListener?.resume()
    }
}