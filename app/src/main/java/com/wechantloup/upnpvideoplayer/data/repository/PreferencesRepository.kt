package com.wechantloup.upnpvideoplayer.data.repository

import android.content.Context
import android.preference.PreferenceManager

class PreferencesRepository(context:Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun getNullableString(key: String): String? {
        return prefs.getString(key, null)
    }
}