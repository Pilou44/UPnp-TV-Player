package com.wechantloup.upnpvideoplayer.browse

import android.graphics.Color
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.dataholder.VideoElement
import com.wechantloup.upnpvideoplayer.utils.ViewUtils.inflate

class BrowseAdapter(
    private var elements: List<Any>,
    private var onItemClicked: (VideoElement) -> Unit
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
                hasFocus -> v.setBackgroundColor(focusedBackgroundColor)
                else -> v.setBackgroundColor(defaultBackgroundColor)
            }
        }
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return elements.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val element = elements[position]) {
            is VideoElement -> bindVideoElement(holder, element, position)
            is String -> bindTitle(holder, element)
        }
    }

    private fun bindTitle(holder: ViewHolder, element: String) {
        holder.text.text = element
    }

    private fun bindVideoElement(holder: ViewHolder, element: VideoElement, position: Int) {
        if (element.isDirectory) {
            holder.icon?.setImageResource(R.drawable.mini_dossier)
        }
        holder.text.text = element.name
        holder.itemView.setOnClickListener { onItemClicked(element) }
        if (position == elementToFocus) {
            holder.itemView.requestFocus()
            elementToFocus = null
        }
        val layoutParams = holder.itemView.layoutParams
        (layoutParams as FlexboxLayoutManager.LayoutParams).flexBasisPercent = .16f
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
    }
}
