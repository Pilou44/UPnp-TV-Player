package com.wechantloup.upnpvideoplayer.browse2

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.wechantloup.upnp.dataholder.UpnpElement
import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.data.dataholder.ParametersElement
import com.wechantloup.upnpvideoplayer.widgets.BrowsingCardView
import kotlin.properties.Delegates

/**
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an ImageCardView.
 */
internal class CardPresenter(private val viewModel: BrowseContract.ViewModel) : Presenter() {

    private var mDefaultCardImage: Drawable? = null
    private var sSelectedBackgroundColor: Int by Delegates.notNull()
    private var sDefaultBackgroundColor: Int by Delegates.notNull()

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        Log.d(TAG, "onCreateViewHolder")

        sDefaultBackgroundColor = ContextCompat.getColor(parent.context, R.color.default_background)
        sSelectedBackgroundColor = ContextCompat.getColor(parent.context, R.color.selected_background)
        mDefaultCardImage = ContextCompat.getDrawable(parent.context, R.drawable.movie)

        val cardView = object : BrowsingCardView(parent.context) {
            override fun setSelected(selected: Boolean) {
                super.setSelected(selected)
                updateCardBackgroundColor(this, false, selected)
            }
        }

        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        Log.d(TAG, "onBindViewHolder")
        val cardView = viewHolder.view as BrowsingCardView

        when (item) {
            is UpnpElement -> {
                when (item.type) {
                    UpnpElement.Type.CONTAINER -> {
                        cardView.isFocusable = true
                        cardView.isFocusableInTouchMode = true
                        updateCardBackgroundColor(cardView, empty = false, selected = false)

                        cardView.setTitle(item.name)
                        cardView.setMainImage(R.drawable.ic_folder)
                    }
                    UpnpElement.Type.FILE -> {
                        cardView.isFocusable = true
                        cardView.isFocusableInTouchMode = true
                        updateCardBackgroundColor(cardView, empty = false, selected = false)

                        cardView.setTitle(item.name)

                        val uri = viewModel.getThumbnail(item)
                        uri?.let {
                            Glide.with(viewHolder.view.context)
                                .load(uri)
                                .centerCrop()
                                .error(mDefaultCardImage)
                                .into(cardView.getMainImageView())
                        }
                    }
                }
            }
            is ParametersElement -> {
                cardView.isFocusable = true
                cardView.isFocusableInTouchMode = true
                updateCardBackgroundColor(cardView, empty = false, selected = false)

                cardView.setTitle(item.titleId)
                cardView.setMainImage(item.icon)
            }
            else -> {
                cardView.setTitle(null)
                cardView.isFocusable = false
                cardView.isFocusableInTouchMode = false
                updateCardBackgroundColor(cardView, empty = true, selected = false)
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        Log.d(TAG, "onUnbindViewHolder")
        val cardView = viewHolder.view as BrowsingCardView
        // Remove references to images so that the garbage collector can free up memory
//        cardView.setBadgeImage(null)
        cardView.setMainImage(null)
    }

    private fun updateCardBackgroundColor(view: BrowsingCardView, empty: Boolean, selected: Boolean) {
        val color = when {
            empty -> Color.TRANSPARENT
            selected -> sSelectedBackgroundColor
            else -> sDefaultBackgroundColor
        }
        // Both background colors should be set because the view's background is temporarily visible
        // during animations.
        view.setBackgroundColor(color)
//        view.setInfoAreaBackgroundColor(color)
    }

    companion object {
        private val TAG = CardPresenter::class.java.simpleName
    }
}
