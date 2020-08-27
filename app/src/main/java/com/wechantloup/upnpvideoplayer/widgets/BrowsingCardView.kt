package com.wechantloup.upnpvideoplayer.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
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

    override fun setSelected(selected: Boolean) {
        if (selected) {
            titleView.ellipsize = TextUtils.TruncateAt.MARQUEE
            titleView.marqueeRepeatLimit = -1
            titleView.isSingleLine = true
        } else {
            titleView.ellipsize = TextUtils.TruncateAt.END
        }
    }

    fun setTitleText(text: String?) {
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

}