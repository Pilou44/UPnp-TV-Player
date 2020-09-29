package com.wechantloup.upnpvideoplayer.data.content

import com.wechantloup.upnpvideoplayer.data.dataholder.ImageSearchApiResult
import org.json.JSONObject
import retrofit2.http.GET
import retrofit2.http.Query

interface ImageSearchApi {

    @GET("ImageSearchAPI")
    suspend fun getImages(
        @Query("pageSize") count: Int,
        @Query("q") search: String
    ): ImageSearchApiResult
}