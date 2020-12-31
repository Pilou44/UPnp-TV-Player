package com.wechantloup.upnpvideoplayer.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.wechantloup.upnpvideoplayer.R

open class BrowsingCardView(context: Context) : ConstraintLayout(context) {

    private val imageView: ImageView
    private val titleView: TextView

    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.browsing_card_view, this)
        imageView = findViewById(R.id.image)
        titleView = findViewById(R.id.title)
    }

    fun setTitle(@StringRes textId: Int) {
        val titleView = findViewById<TextView>(R.id.title)
        titleView.setText(textId)
    }

    fun setTitle(text: String?) {
        val titleView = findViewById<TextView>(R.id.title)
        titleView.text = text
    }

    fun setMainImage(@DrawableRes drawable: Int) {
        val imageView = findViewById<ImageView>(R.id.image)
        imageView.setImageResource(drawable)
    }

    fun setMainImage(image: Drawable?) {
        val imageView = findViewById<ImageView>(R.id.image)
        imageView.setImageDrawable(image)
    }

    fun setMainImage(imageUrl: String) {
        val imageView = findViewById<ImageView>(R.id.image)
        Glide.with(imageView.context)
            .load(imageUrl)
            .centerCrop()
            .into(imageView)
    }

    fun getMainImageView(): ImageView {
        return imageView
    }
}
