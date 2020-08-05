package com.wechantloup.upnpvideoplayer.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

internal object ViewUtils {

    fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View =
        LayoutInflater
            .from(context)
            .inflate(layoutRes, this, attachToRoot)
}