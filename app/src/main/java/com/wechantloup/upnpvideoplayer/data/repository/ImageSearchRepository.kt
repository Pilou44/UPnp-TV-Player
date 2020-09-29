package com.wechantloup.upnpvideoplayer.data.repository

import com.facebook.stetho.okhttp3.StethoInterceptor
import com.wechantloup.upnpvideoplayer.data.content.ImageSearchApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ImageSearchRepository {

    private fun getBaseUrl(): String = "https://contextualwebsearch-websearch-v1.p.rapidapi.com/api/Search/"

    private val service = apiService()

    suspend fun getImages(search: String) =
        service.getImages(30, search)

    private fun apiService(): ImageSearchApi = Retrofit.Builder()
        .baseUrl(getBaseUrl())
        .addConverterFactory(GsonConverterFactory.create())
        .client(createHttpClient())
        .build()
        .create(ImageSearchApi::class.java)

    private fun createHttpClient() = OkHttpClient.Builder()
        .addNetworkInterceptor(StethoInterceptor())
        .addInterceptor(RequestInterceptor())
        .build()

    class RequestInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val newRequest = request.newBuilder()
                .addHeader("x-rapidapi-host", "contextualwebsearch-websearch-v1.p.rapidapi.com")
                .addHeader("x-rapidapi-key", "6fd7b5dd1bmsh8df2814366f7ed8p1049e4jsn2374ef45e220")
                .build()

            return chain.proceed(newRequest)
        }
    }
}
