package com.wechantloup.upnpvideoplayer.data.useCase

import com.wechantloup.upnp.dataholder.DlnaRoot
import com.wechantloup.upnpvideoplayer.data.repository.RootRepository

class SetRootUseCase(private val repository: RootRepository) {

    fun execute(root: DlnaRoot) {
        repository.setRoot(root)
    }
}