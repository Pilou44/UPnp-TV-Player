package com.wechantloup.upnpvideoplayer.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

class CacheRepository(context: Context) {

    val root: File = requireNotNull(context.externalCacheDir)

    fun writeFile(bitmap: Bitmap, fileName: String) {
//        root.mkdirs()
        val imageFile = File(root, "$fileName.jpg")
//        if (!imageFile.exists()) {
//            imageFile.createNewFile()
//        }
        val bmp = ThumbnailUtils.extractThumbnail(bitmap, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, ThumbnailUtils.OPTIONS_RECYCLE_INPUT)
        val fos = FileOutputStream(imageFile)
        bmp.compress(Bitmap.CompressFormat.JPEG, 30, fos)
        fos.close()
        bmp.recycle()
    }

    fun getBitmapUri(fileName: String): Uri? {
        val imageFile = File(root, "$fileName.jpg")

        if (!imageFile.exists()) return null

        return Uri.fromFile(imageFile)
    }

    companion object {
        private const val THUMBNAIL_WIDTH = 313
        private const val THUMBNAIL_HEIGHT = 176
    }
}