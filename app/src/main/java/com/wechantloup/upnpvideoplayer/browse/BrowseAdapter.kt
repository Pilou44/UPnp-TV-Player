package com.wechantloup.upnpvideoplayer.browse

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.dataholder.DlnaElement
import com.wechantloup.upnpvideoplayer.dataholder.VideoElement
import com.wechantloup.upnpvideoplayer.utils.ViewUtils.inflate

class BrowseAdapter(
    private var elements: MutableList<VideoElement>,
    private var onItemClicked: (VideoElement) -> Unit
) : RecyclerView.Adapter<BrowseAdapter.ViewHolder>() {

    private var defaultBackgroundColor = Color.parseColor("#00ffffff")
    private var focusedBackgroundColor = Color.parseColor("#66ffe680")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
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
        val element = elements[position]
        if (element.isDirectory) {
            holder.icon.setImageResource(R.drawable.mini_dossier)
        }
        holder.text.text = element.name
        holder.itemView.setOnClickListener { onItemClicked(element) }
    }

    override fun getItemViewType(position: Int): Int {
        return when (elements[position].isDirectory) {
            true -> TYPE_DIRECTORY
            false -> TYPE_VIDEO
        }
    }

    class ViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val text: TextView = view.findViewById(R.id.name)
    }

    companion object {
        private const val TYPE_DIRECTORY = 0
        private const val TYPE_VIDEO = 1
    }
}
