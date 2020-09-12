package com.wechantloup.upnpvideoplayer.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import com.wechantloup.upnp.dataholder.UpnpElement
import java.io.File
import java.io.FileOutputStream

class ThumbnailRepository(context: Context) {

    private val root: File = requireNotNull(context.externalCacheDir)

    fun writeElementThumbnail(bitmap: Bitmap, element: UpnpElement) {
        val fileName = extractFileName(element.path)
        val imageFile = File(root, fileName)
        val bmp = ThumbnailUtils.extractThumbnail(bitmap, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, ThumbnailUtils.OPTIONS_RECYCLE_INPUT)
        val fos = FileOutputStream(imageFile)
        bmp.compress(Bitmap.CompressFormat.JPEG, 30, fos)
        fos.close()
        bmp.recycle()
    }

    fun getElementThumbnail(element: UpnpElement): Uri? {
        val fileName = extractFileName(element.path)
        val imageFile = File(root, fileName)

        if (!imageFile.exists()) return null

        return Uri.fromFile(imageFile)
    }

    private fun extractFileName(path: String) =
        "${path.substring(path.lastIndexOf("/") + 1)}.jpg"

    companion object {
        private const val THUMBNAIL_WIDTH = 313
        private const val THUMBNAIL_HEIGHT = 176
    }
}