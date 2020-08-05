package com.wechantloup.upnpvideoplayer.rootSetter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.wechantloup.upnpvideoplayer.R
import com.wechantloup.upnpvideoplayer.dataholder.DlnaElement
import com.wechantloup.upnpvideoplayer.utils.ViewUtils.inflate

class RootSetterAdapter(
    private var list: List<DlnaElement>,
    private var onItemClicked: (DlnaElement) -> Unit
): RecyclerView.Adapter<RootSetterAdapter.DlnaElementHolder>() {

    private var selectedElement = -1

    private var defaultBackgroundColor = Color.parseColor("#00ffffff")
    private var selectedBackgroundColor = Color.parseColor("#66666666")
    private var focusedBackgroundColor = Color.parseColor("#66ffe680")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DlnaElementHolder {
        val view = parent.inflate(R.layout.item_dlna_element)
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.setOnFocusChangeListener { v: View, hasFocus: Boolean ->
            when {
                hasFocus -> v.setBackgroundColor(focusedBackgroundColor)
                v.tag == selectedElement -> v.setBackgroundColor(selectedBackgroundColor)
                else -> v.setBackgroundColor(defaultBackgroundColor)
            }
        }
        return DlnaElementHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: DlnaElementHolder, position: Int) {
        val element = list[position]
        holder.apply {
            itemView.tag = position
            name.text = element.name
            itemView.setOnClickListener { onItemClicked(element) }
            val padding: Int = holder.layout.paddingRight
            layout.setPadding(
                padding * (element.indent + 1),
                padding, padding, padding
            )
            if (position == selectedElement) {
                itemView.setBackgroundColor(selectedBackgroundColor)
            } else {
                itemView.setBackgroundColor(defaultBackgroundColor)
            }
        }
    }

    class DlnaElementHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name)
        val layout: RelativeLayout = view.findViewById(R.id.layout)
    }

    fun setSelectedElement(position: Int) {
        val oldSelection = selectedElement
        selectedElement = position
        notifyItemChanged(oldSelection)
        notifyItemChanged(selectedElement)
    }
}