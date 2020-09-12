package com.wechantloup.upnpvideoplayer.data.content

import android.content.Context
import android.preference.PreferenceManager

class Preferences(context:Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun getNullableString(key: String): String? {
        return prefs.getString(key, null)
    }

    fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}