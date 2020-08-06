package com.wechantloup.upnpvideoplayer.utils

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import java.util.concurrent.locks.ReentrantLock

internal class ThreadsafeConstraintsApplier {

    private val lock = ReentrantLock()

    fun applyConstraintsTo(layout: ConstraintLayout, actions: (ConstraintSet) -> Unit) {
        val constraintsHelper = ConstraintSetHelper(layout)
        lock.lock()
        constraintsHelper.updateConstraints { actions(this) }
        lock.unlock()
    }
}
