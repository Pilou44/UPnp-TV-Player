package com.wechantloup.upnpvideoplayer.data

import com.wechantloup.upnpvideoplayer.data.dataholder.DlnaRoot
import com.wechantloup.upnpvideoplayer.data.repository.PreferencesRepository
import com.wechantloup.upnpvideoplayer.utils.Serializer.deserialize

class GetRootUseCase(private val repository: PreferencesRepository) {

    fun execute(): DlnaRoot? {
        val serializedRoot = repository.getNullableString(KEY_ROOT)
        return serializedRoot?.deserialize<DlnaRoot>()
    }

    companion object {
        private const val KEY_ROOT = "ROOT"
    }
}