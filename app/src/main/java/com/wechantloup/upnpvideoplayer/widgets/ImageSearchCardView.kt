package com.wechantloup.upnpvideoplayer.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.wechantloup.upnpvideoplayer.R

class ImageSearchCardView(context: Context) : ConstraintLayout(context) {

    private val imageView: ImageView

    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.image_search_card_view, this)
        imageView = findViewById(R.id.image)
    }

    fun setMainImage(imageUrl: String?) {
        if (imageUrl == null) {
            imageView.setImageDrawable(null)
            return
        }

        val imageView = findViewById<ImageView>(R.id.image)
        Glide.with(imageView.context)
            .load(imageUrl)
            .centerCrop()
            .into(imageView)
    }
}
