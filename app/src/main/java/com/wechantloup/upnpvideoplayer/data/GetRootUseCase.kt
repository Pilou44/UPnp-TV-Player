package com.wechantloup.upnpvideoplayer.data

import com.wechantloup.upnpvideoplayer.data.repository.PreferencesRepository
import com.wechantloup.core.utils.Serializer.deserialize

class GetRootUseCase(private val repository: PreferencesRepository) {

    fun execute(): com.wechantloup.upnp.dataholder.DlnaRoot? {
        val serializedRoot = repository.getNullableString(KEY_ROOT)
        return serializedRoot?.deserialize<com.wechantloup.upnp.dataholder.DlnaRoot>()
    }

    companion object {
        private const val KEY_ROOT = "ROOT"
    }
}