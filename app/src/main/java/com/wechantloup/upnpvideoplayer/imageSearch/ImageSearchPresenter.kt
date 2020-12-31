package com.wechantloup.upnpvideoplayer.imageSearch

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.Presenter
import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.data.dataholder.ImageSearchApiResult
import com.wechantloup.upnpvideoplayer.widgets.ImageSearchCardView
import kotlin.properties.Delegates

class ImageSearchPresenter : Presenter() {

    private var mDefaultCardImage: Drawable? = null
    private var sSelectedBackgroundColor: Int by Delegates.notNull()
    private var sDefaultBackgroundColor: Int by Delegates.notNull()

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        sDefaultBackgroundColor = ContextCompat.getColor(parent.context, R.color.default_background)
        sSelectedBackgroundColor = ContextCompat.getColor(parent.context, R.color.selected_background)
        mDefaultCardImage = ContextCompat.getDrawable(parent.context, R.drawable.movie)

        val cardView = ImageSearchCardView(parent.context)

        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val cardView = viewHolder.view as ImageSearchCardView
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true

        val thumbnail = requireNotNull(item as? ImageSearchApiResult.Item).thumbnail
        cardView.setMainImage(thumbnail)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageSearchCardView
        // Remove references to images so that the garbage collector can free up memory
//        cardView.setBadgeImage(null)
        cardView.setMainImage(null)
    }
}
