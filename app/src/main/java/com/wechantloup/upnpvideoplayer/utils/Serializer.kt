package com.wechantloup.upnpvideoplayer.utils

import com.google.gson.Gson

internal object Serializer {

    private fun <T> String.deserialize(clazz: Class<T>): T = Gson().fromJson(this, clazz)
    inline fun <reified T> String.deserialize(): T = deserialize(T::class.java)
    fun Any.serialize() = Gson().toJson(this)
}