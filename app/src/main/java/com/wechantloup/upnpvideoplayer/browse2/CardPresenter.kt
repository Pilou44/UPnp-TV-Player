package com.wechantloup.upnpvideoplayer.browse2

import android.graphics.drawable.Drawable
import android.text.TextUtils
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import androidx.core.content.ContextCompat
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView

import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.data.dataholder.BrowsableVideoElement
import com.wechantloup.upnpvideoplayer.data.dataholder.VideoElement
import kotlin.properties.Delegates

/**
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an ImageCardView.
 */
class CardPresenter : Presenter() {

    private var mDefaultCardImage: Drawable? = null
    private var sSelectedBackgroundColor: Int by Delegates.notNull()
    private var sDefaultBackgroundColor: Int by Delegates.notNull()

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        Log.d(TAG, "onCreateViewHolder")

        sDefaultBackgroundColor = ContextCompat.getColor(parent.context, R.color.default_background)
        sSelectedBackgroundColor = ContextCompat.getColor(parent.context, R.color.selected_background)
        mDefaultCardImage = ContextCompat.getDrawable(parent.context, R.drawable.movie)

        val cardView = object : ImageCardView(parent.context) {
            override fun setSelected(selected: Boolean) {
                val titleView = findViewById<TextView>(R.id.title_text)
                if (selected) {
                    titleView.ellipsize = TextUtils.TruncateAt.MARQUEE
                    titleView.marqueeRepeatLimit = -1
                    titleView.isSingleLine = true
                } else {
                    titleView.ellipsize = TextUtils.TruncateAt.END
                }

                updateCardBackgroundColor(this, selected)
                super.setSelected(selected)
            }
        }
        val titleView = cardView.findViewById<TextView>(R.id.title_text)
        val params = titleView.layoutParams as RelativeLayout.LayoutParams
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        titleView.layoutParams = params

        updateCardBackgroundColor(cardView, false)
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        Log.d(TAG, "onBindViewHolder")
        val cardView = viewHolder.view as ImageCardView
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        val mainImage = cardView.findViewById<ImageView>(R.id.main_image)
        val info = cardView.findViewById<RelativeLayout>(R.id.info_field)
        if (item is BrowsableVideoElement) {
            mainImage.visibility = VISIBLE
            info.visibility = VISIBLE
            cardView.isFocusable = true
            cardView.isFocusableInTouchMode = true

//        if (movie.cardImageUrl != null) {
            cardView.titleText = item.name
//            cardView.contentText = movie.studio
            if (item.isDirectory) {
                cardView.mainImageView.apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageResource(R.drawable.ic_directory)
                }
            }
//            Glide.with(viewHolder.view.context)
//                .load(movie.cardImageUrl)
//                .centerCrop()
//                .error(mDefaultCardImage)
//                .into(cardView.mainImageView)
//        }
        } else {
            mainImage.visibility = GONE
            info.visibility = GONE
            cardView.titleText = null
            cardView.isFocusable = false
            cardView.isFocusableInTouchMode = false
        }
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        Log.d(TAG, "onUnbindViewHolder")
        val cardView = viewHolder.view as ImageCardView
        // Remove references to images so that the garbage collector can free up memory
        cardView.badgeImage = null
        cardView.mainImage = null
    }

    private fun updateCardBackgroundColor(view: ImageCardView, selected: Boolean) {
        val color = if (selected) sSelectedBackgroundColor else sDefaultBackgroundColor
        // Both background colors should be set because the view's background is temporarily visible
        // during animations.
        view.setBackgroundColor(color)
        view.setInfoAreaBackgroundColor(color)
    }

    companion object {
        private val TAG = "CardPresenter"

        private val CARD_WIDTH = 313
        private val CARD_HEIGHT = 176
    }
}
