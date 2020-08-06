package com.wechantloup.upnpvideoplayer.videoPlayer

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.utils.ThreadsafeConstraintsApplier
import com.wechantloup.upnpvideoplayer.utils.ViewUtils.startAnimatingConstraints
import java.lang.IllegalStateException
import java.util.Timer
import java.util.TimerTask

class ControlsOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    private var timer = Timer()
    private val constraintSetter = ThreadsafeConstraintsApplier()
    private val content: View
    @kotlin.jvm.JvmField var isOpened: Boolean = false

    init {
        inflate(context, R.layout.controls_overlay_layout, this)
        content = findViewById(R.id.content)
    }

    fun hide() {
        if (!isOpened) return

        startAnimatingConstraints()
        constraintSetter.applyConstraintsTo(this) {
            it.clear(content.id, ConstraintSet.TOP)
            it.connect(content.id, ConstraintSet.TOP, id, ConstraintSet.BOTTOM)
        }
        isOpened = false
    }

    fun show() {
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
}