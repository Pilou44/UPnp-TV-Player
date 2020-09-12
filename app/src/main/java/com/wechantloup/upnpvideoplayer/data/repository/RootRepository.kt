package com.wechantloup.upnpvideoplayer.data.repository

import android.content.Context
import com.wechantloup.core.utils.Serializer.deserialize
import com.wechantloup.core.utils.Serializer.serialize
import com.wechantloup.upnp.dataholder.DlnaRoot
import com.wechantloup.upnpvideoplayer.data.content.Preferences

class RootRepository(context: Context) {
    private val preferences = Preferences(context)
    private var root: DlnaRoot? = preferences.getNullableString(KEY_ROOT)?.deserialize()

    fun getRoot() = root

    fun setRoot(newRoot: DlnaRoot) {
        root = newRoot
        preferences.setString(KEY_ROOT, newRoot.serialize())
    }

    companion object {
        private const val KEY_ROOT = "ROOT"
    }
}