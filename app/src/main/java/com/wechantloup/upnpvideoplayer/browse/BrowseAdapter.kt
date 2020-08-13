package com.wechantloup.upnpvideoplayer.browse

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.browse.BrowseActivity.Companion.NUMBER_OF_COLUMNS
import com.wechantloup.upnpvideoplayer.dataholder.VideoElement
import com.wechantloup.upnpvideoplayer.utils.ViewUtils.inflate

class BrowseAdapter(
    private val elements: List<Any>,
    private val onItemClicked: (VideoElement) -> Unit,
    private val onItemSelected: (Int) -> Unit,
    private val directoriesButton: Int,
    private val videosButton: Int
) : RecyclerView.Adapter<BrowseAdapter.ViewHolder>() {

    private var elementToFocus: Int? = null
    private var defaultBackgroundColor = Color.parseColor("#00ffffff")
    private var focusedBackgroundColor = Color.parseColor("#66ffe680")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (viewType == TYPE_TITLE) {
            val view = parent.inflate(R.layout.item_title)
            return ViewHolder(view)
        }

        val view = parent.inflate(R.layout.item_browse_element)
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.setOnFocusChangeListener { v: View, hasFocus: Boolean ->
            when {
                hasFocus -> {
                    val position: Int = view.tag as Int
                    onItemSelected(position)
                    v.setBackgroundColor(focusedBackgroundColor)
                }
                else -> v.setBackgroundColor(defaultBackgroundColor)
            }
        }
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return elements.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.tag = position
        when (val element = elements[position]) {
            is VideoElement -> bindVideoElement(holder, element, position)
            is String -> bindTitle(holder, element)
        }
    }

    private fun bindTitle(holder: ViewHolder, element: String) {
        holder.text.text = element
    }

    private fun bindVideoElement(holder: ViewHolder, element: VideoElement, position: Int) {
        val directoriesList = elements.filterIsInstance<VideoElement>().filter { it.isDirectory }
        val moviesList = elements.filterIsInstance<VideoElement>().filter { !it.isDirectory }
        if (element.isDirectory) {
            holder.icon?.setImageResource(R.drawable.mini_dossier)
            if (directoriesList.indexOf(element) % NUMBER_OF_COLUMNS == 0) {
                holder.itemView.nextFocusLeftId = directoriesButton
            }
        } else {
            if (moviesList.indexOf(element) % NUMBER_OF_COLUMNS == 0) {
                holder.itemView.nextFocusLeftId = videosButton
            }
        }
        holder.text.text = element.name
        holder.itemView.setOnClickListener { onItemClicked(element) }
        if (position == elementToFocus) {
            holder.itemView.requestFocus()
            elementToFocus = null
        }
        val layoutParams = holder.itemView.layoutParams
        (layoutParams as FlexboxLayoutManager.LayoutParams).flexBasisPercent = COLUMN_WIDTH
    }

    override fun getItemViewType(position: Int): Int {
        return when (val element = elements[position]) {
            is VideoElement -> if (element.isDirectory) TYPE_DIRECTORY else TYPE_VIDEO
            else -> TYPE_TITLE
        }
    }

    fun requestFocusFor(position: Int) {
        elementToFocus = position
        notifyItemChanged(position)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView? = view.findViewById(R.id.icon)
        val text: TextView = view.findViewById(R.id.name)
    }

    companion object {
        private const val TYPE_DIRECTORY = 0
        private const val TYPE_VIDEO = 1
        private const val TYPE_TITLE = 2
        private const val COLUMN_WIDTH = (100 / NUMBER_OF_COLUMNS).toFloat() / 100f
    }
}
