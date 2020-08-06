package com.wechantloup.upnpvideoplayer.utils

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

internal class ConstraintSetHelper(private val layout: ConstraintLayout) {
    fun setDimensionRatioTo(view: View, ratio: Double) {
        updateConstraints {
            setDimensionRatio(view.id, "${(ratio * 100).toInt()}:100")
        }
    }

    fun updateConstraints(constraintsSetter: ConstraintSet.(ConstraintLayout) -> Unit) {
        ConstraintSet().apply {
            clone(layout)
            constraintsSetter(layout)
        }.applyTo(layout)
    }
}
