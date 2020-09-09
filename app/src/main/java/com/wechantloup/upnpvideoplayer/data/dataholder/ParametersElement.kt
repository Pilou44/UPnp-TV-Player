package com.wechantloup.upnpvideoplayer.data.dataholder

import android.content.Intent
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

class ParametersElement(
    @StringRes val titleId: Int,
    @ColorRes val background: Int,
    @DrawableRes val icon: Int,
    val intent: Intent,
    val requestCode: Int
)