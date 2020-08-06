package com.wechantloup.upnpvideoplayer.videoPlayer

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.utils.ThreadsafeConstraintsApplier
import com.wechantloup.upnpvideoplayer.utils.ViewUtils.startAnimatingConstraints
import java.util.Timer
import java.util.TimerTask

internal class ControlsOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    private var controlsListener: ControlsOverlayListener? = null
    private var timer = Timer()
    private val constraintSetter = ThreadsafeConstraintsApplier()
    private val content: View
    @kotlin.jvm.JvmField var isOpened: Boolean = false

    init {
        inflate(context, R.layout.controls_overlay_layout, this)
        content = findViewById(R.id.content)

        val playPauseButton: Button = findViewById(R.id.play_pause)
        playPauseButton.setOnClickListener { controlsListener?.playPause() }
        val nextButton: Button = findViewById(R.id.next)
        nextButton.setOnClickListener { controlsListener?.next() }
        val previousButton: Button = findViewById(R.id.previous)
        previousButton.setOnClickListener { controlsListener?.previous() }
    }

    fun hide() {
        if (!isOpened) return

        controlsListener = null

        startAnimatingConstraints()
        constraintSetter.applyConstraintsTo(this) {
            it.clear(content.id, ConstraintSet.TOP)
            it.connect(content.id, ConstraintSet.TOP, id, ConstraintSet.BOTTOM)
        }
        isOpened = false
    }

    fun show(listener: ControlsOverlayListener) {
        controlsListener = listener
        startAnimatingConstraints()
        constraintSetter.applyConstraintsTo(this) {
            it.clear(content.id, ConstraintSet.TOP)
            it.connect(content.id, ConstraintSet.TOP, id, ConstraintSet.TOP)
        }
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
            controls.handler.post {
                controls.hide()
            }
        }
    }

    interface ControlsOverlayListener {
        fun playPause()
        fun next()
        fun previous()
    }
}