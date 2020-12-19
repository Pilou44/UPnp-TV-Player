package com.wechantloup.upnpvideoplayer.data.useCase

import com.wechantloup.upnp.dataholder.DlnaRoot
import com.wechantloup.upnp.dataholder.UpnpElement
import com.wechantloup.upnpvideoplayer.data.repository.RootRepository

class GetRootUseCase(private val repository: RootRepository) {

    fun execute(): UpnpElement? {
        return repository.getRoot()
    }
}
